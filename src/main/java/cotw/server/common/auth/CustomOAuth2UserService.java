package cotw.server.common.auth;

import cotw.server.common.auth.DTO.GoogleUserInfo;
import cotw.server.common.auth.DTO.KakaoUserInfo;
import cotw.server.common.auth.DTO.NaverUserInfo;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.ProviderType;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService defaultOAuth2UserService = new DefaultOAuth2UserService();
    private final MemberRepository memberRepository;


    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = defaultOAuth2UserService.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo info = switch(registrationId){
            case "google" -> GoogleUserInfo.from(attributes);
            case "naver" -> NaverUserInfo.from(attributes);
            case "kakao" -> KakaoUserInfo.from(attributes);
            default -> throw new IllegalArgumentException("Unsupported provider:  " + registrationId);
        };

        // 1) registrationId → ProviderType 정규화
        ProviderType provider = toProviderType(registrationId);
        String providerId = info.id();    // 구글=sub, 카카오=id, 네이버=response.id 로 표준화된 값
        String email = info.email();      // (없을 수도 있음)

        // 2) (provider, providerId)로 먼저 조회 → 있으면 정보만 갱신
        Member member = memberRepository.findByProviderAndProviderId(provider, providerId)
                .map(u -> {
                    u.update(info.name(), email);
                    return memberRepository.save(u);
                })
                .orElseGet(() -> {
                    // 2) 없으면 email로 병합 시도 (email이 제공된 경우)
                    if (email != null && !email.isBlank()) {
                        return memberRepository.findByEmail(email)
                                .map(existing -> {
                                    // 기존 일반회원/다른 소셜 → 현재 소셜로 연결
                                    existing.linkSocial(provider, providerId);
                                    existing.update(info.name(), email);
                                    return memberRepository.save(existing);
                                })
                                .orElseGet(() ->
                                        // 3) email로도 없으면 신규 소셜 회원 생성
                                        memberRepository.save(
                                                Member.ofSocial(provider, providerId, info.name(), email)
                                        )
                                );
                    }
                    // 4) email이 아예 내려오지 않는 경우(동의 거부 등) → 새 소셜 회원 생성
                    return memberRepository.save(
                            Member.ofSocial(provider, providerId, info.name(), null)
                    );
                });

        // 3) 권한 및 Principal 반환
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("USER"));

        return new DefaultOAuth2User(authorities, // 권한
                info.toAttributeMap(),           // 뷰에서 쓰고 싶으면 표준화된 맵
                "id"                              // nameAttributeKey
        );

    }

    private ProviderType toProviderType(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> ProviderType.GOOGLE;
            case "kakao"  -> ProviderType.KAKAO;
            case "naver"  -> ProviderType.NAVER;
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }


}
