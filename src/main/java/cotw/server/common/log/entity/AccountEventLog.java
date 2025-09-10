package cotw.server.common.log.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "account_event_log")
public class AccountEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AccountEventType eventType;

    private Long memberId;
    private String email;
    private String ip;
    private String ua;
    private String purpose;
    private boolean success;

    @Column(length = 500)
    private String message;

    @CreationTimestamp
    private LocalDateTime createdAt;
}