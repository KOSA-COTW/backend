package cotw.server.domain.member.Dto.response;

import cotw.server.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record SignUpResponseDTO(
        String name,
        String email,
        String password,
        String residentRegistrationNumber,
        String createdDate) {

    public static SignUpResponseDTO fromEntity(Member member) {
        return new SignUpResponseDTO(
                member.getName(),
                member.getEmail(),
                member.getPassword(),
                member.getResidentRegistrationNumber(),
                member.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
}
