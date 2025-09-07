package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.request.*;
import cotw.server.domain.admin.dto.response.*;
import cotw.server.domain.member.entity.AccountStatus;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminMemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public AdminMemberListResponse search(String keyword, String role, String status, int page, int size) {
        String kw = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim().toLowerCase() + "%" : null;
        Role r = parseRole(role);
        AccountStatus st = parseStatus(status);
        Page<Member> result = memberRepository.searchMembers(kw, r, st, PageRequest.of(page - 1, size));
        List<AdminMemberListItemResponse> content = result.getContent().stream()
                .map(m -> new AdminMemberListItemResponse(
                        m.getId(),
                        m.getName(),
                        m.getEmail(),
                        List.of(m.getRole().getAuthority()),
                        m.getStatus(),
                        m.getCreatedAt()))
                .toList();
        return new AdminMemberListResponse(content, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminMemberDetailResponse get(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        return new AdminMemberDetailResponse(
                m.getId(),
                m.getName(),
                m.getEmail(),
                m.getStatus(),
                List.of(m.getRole().getAuthority())
        );
    }

    public void updateProfile(Long id, AdminMemberProfileRequest req) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        Optional.ofNullable(req.name()).ifPresent(m::setName);
        Optional.ofNullable(req.email()).ifPresent(e -> m.setEmail(e.toLowerCase()));
        Optional.ofNullable(req.status()).ifPresent(m::setStatus);
    }

    public void updateRole(Long id, AdminMemberRoleRequest req) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        Role r = parseRole(req.role());
        if (r != null) {
            m.setRole(r);
        }
    }

    public void delete(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        m.setStatus(AccountStatus.DELETED);
        m.setDeletedAt(LocalDateTime.now());
    }

    public void bulkDelete(List<Long> ids) {
        ids.forEach(this::delete);
    }

    public void bulkUpdateStatus(List<Long> ids, AccountStatus status) {
        for (Long id : ids) {
            Member m = memberRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("member not found"));
            m.setStatus(status);
        }
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) return null;
        String r = role.startsWith("ROLE_") ? role.substring(5) : role;
        try {
            return Role.valueOf(r);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private AccountStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return AccountStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
