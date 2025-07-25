package cotw.server.domain.member.controller;

import cotw.server.domain.member.Dto.request.LoginRequestDTO;
import cotw.server.domain.member.Dto.request.SignUpRequestDTO;
import cotw.server.domain.member.Dto.response.SignUpResponseDTO;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO signUpRequestDTO) {
        SignUpResponseDTO response = memberService.signUpMember(signUpRequestDTO);
        ResponseEntity<SignUpResponseDTO> responseEntity = new ResponseEntity<>(response, HttpStatus.CREATED);

        return responseEntity;
    }

//    @PostMapping("/login")
//    public ResponseEntity<Member> login(@RequestBody LoginRequestDTO loginRequestDTO){
//        Member member = memberService.loginMember(loginRequestDTO);
//        // 이메일, 비밀번호 일치 시 jwt token 발행
//
//
//        return new ResponseEntity<>(member, HttpStatus.OK);
//    }

}
