package cotw.server.domain.member.controller;

import cotw.server.common.jwt.CustomUserDetails;
import cotw.server.domain.member.Dto.request.LoginRequestDTO;
import cotw.server.domain.member.Dto.request.SignUpRequestDTO;
import cotw.server.domain.member.Dto.response.ShowInfoResponseDTO;
import cotw.server.domain.member.Dto.response.SignUpResponseDTO;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/auth/signup")
    public ResponseEntity<SignUpResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO signUpRequestDTO) {
        System.out.println("signUpRequestDTO");
        SignUpResponseDTO response = memberService.signUpMember(signUpRequestDTO);
        ResponseEntity<SignUpResponseDTO> responseEntity = new ResponseEntity<>(response, HttpStatus.CREATED);

        return responseEntity;
    }

    @GetMapping("/info")
    public ResponseEntity<ShowInfoResponseDTO> getInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        ShowInfoResponseDTO response = memberService.showMemberInfo(customUserDetails);
        ResponseEntity<ShowInfoResponseDTO> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
        return responseEntity;
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        memberService.withdrawMember(customUserDetails);
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);

        return responseEntity;
    }



}
