package cotw.server.common.auth;

import cotw.server.common.auth.DTO.GoogleUserInfo;
import cotw.server.common.auth.DTO.KakaoUserInfo;
import cotw.server.common.auth.DTO.NaverUserInfo;
import cotw.server.domain.member.repository.MemberRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService defaultOAuth2UserService = new DefaultOAuth2UserService();
    private final MemberRepository memberRepository;

    public CustomOAuth2UserService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }


    @Override
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

        // 2) DB upsert
//        Member member = memberRepository.findByProviderAndProviderId((registrationId), info.id())
//                .map(u -> u.update(info.name(), info.email(), info.picture()))
//                .orElseGet(() -> memberRepository.save(
//                        Member.ofSocial(registrationId, info.id(), info.name(), info.email(), info.picture())
//                ));

        // 3) 권한 및 Principal 반환
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("USER"));

        return new DefaultOAuth2User(authorities, // 권한
                info.toAttributeMap(),           // 뷰에서 쓰고 싶으면 표준화된 맵
                "id"                              // nameAttributeKey
        );
    }
}
