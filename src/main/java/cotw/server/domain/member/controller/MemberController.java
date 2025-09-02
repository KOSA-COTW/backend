package cotw.server.domain.member.controller;

import cotw.server.common.jwt.CustomUserDetails;
import cotw.server.domain.member.Dto.request.*;
import cotw.server.domain.member.Dto.response.ShowInfoResponseDTO;
import cotw.server.domain.member.Dto.response.SignUpResponseDTO;
import cotw.server.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 일반 회원가입. 소셜 회원가입은 로그인 시도 시 자동으로 진행
    @PostMapping("/auth/signup")
    public ResponseEntity<SignUpResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO signUpRequestDTO) {
        SignUpResponseDTO response = memberService.signUpMember(signUpRequestDTO);
        ResponseEntity<SignUpResponseDTO> responseEntity = new ResponseEntity<>(response, HttpStatus.CREATED);

        return responseEntity;
    }

    // 내 정보 불러오기
    @GetMapping("/info")
    public ResponseEntity<ShowInfoResponseDTO> getInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        ShowInfoResponseDTO response = memberService.showMemberInfo(customUserDetails);
        ResponseEntity<ShowInfoResponseDTO> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
        return responseEntity;
    }

    // 본인 탈퇴
    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal CustomUserDetails me,
                                           @RequestBody DeactivateRequestDTO requestDTO) {
        memberService.deactivate(me.getMemberId(), Duration.ofDays(30), requestDTO.password()); // 보관기간 정책
        return ResponseEntity.ok().build();
    }

    // 보관기간 내 복구 - 이메일로 1회성 토큰 발송해서 복구하는 형태로 진행할 예정.
    @PostMapping("/recover")
    public ResponseEntity<Void> recover(@RequestBody String email) {
        memberService.recover(email);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/editpass")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                       @RequestBody PatchPasswordRequestDTO requestDTO) {
        memberService.editPassword(customUserDetails, requestDTO.currentPassword(), requestDTO.newPassword());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/changeimage")
    public ResponseEntity<Void> channgeProfileImage(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                    @RequestPart("file") MultipartFile file){
        memberService.editImage(customUserDetails, file);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/editnickname")
    public ResponseEntity<Void> changeNickname(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                               @RequestBody PatchNicknameRequestDTO requestDTO){
        memberService.editNickname(customUserDetails, requestDTO.newNickname());
        return ResponseEntity.ok().build();
    }


}
