package cotw.server.common.mail.tokenPayload;

public enum EmailTokenPurpose {
    EMAIL_VERIFY,
    USERNAME_LOOKUP,   // "아이디 보기" 확인 페이지에 쓸 경우
    PASSWORD_RESET,
    SIGNUP, ACCOUNT_RECOVER
}
