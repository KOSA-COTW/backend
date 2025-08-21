package cotw.server.domain.member.entity;

import cotw.server.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(
        name = "member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(255) default 'LOCAL' check (provider in ('LOCAL','GOOGLE','KAKAO','NAVER'))"
    )
    @Builder.Default
    private ProviderType provider = ProviderType.LOCAL;

    @PrePersist
    void ensureProvider() {
        if (provider == null) provider = ProviderType.LOCAL;
    }

    @Column(name = "provider_id")
    private String providerId;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;
    private String password;

    @Column(nullable = true)
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;


    // == 팩토리 ==
    public static Member ofLocal(String name, String email, String passwordHash) {
        Member m = new Member();
        m.provider = ProviderType.LOCAL;
        m.providerId = null;
        m.name = name;
        m.email = email != null ? email.toLowerCase() : null;
        m.password = passwordHash;
        return m;
    }

    public static Member ofSocial(ProviderType provider, String providerId,
                                  String name, String email) {
        Member m = new Member();
        m.provider = provider;                 // GOOGLE/KAKAO/...
        m.providerId = providerId;
        m.name = name;
        m.email = email != null ? email.toLowerCase() : null;
        return m;
    }

    // == 소셜 동기화 ==
    public Member update(String name, String email) {
        if (name != null) this.name = name;
        if (email != null) this.email = email.toLowerCase();

        return this;
    }

    public void linkSocial(ProviderType provider, String providerId) {
        this.setProvider(provider);
        this.setProviderId(providerId);
    }

}
