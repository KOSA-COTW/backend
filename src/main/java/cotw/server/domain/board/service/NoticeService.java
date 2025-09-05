package cotw.server.domain.board.service;

import cotw.server.domain.board.dto.request.NoticeRequestDto;
import cotw.server.domain.board.dto.response.NoticeResponseDto;
import cotw.server.domain.board.entity.Notice;
import cotw.server.domain.board.exception.BoardException;
import cotw.server.domain.board.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public NoticeResponseDto createNotice(NoticeRequestDto dto) {

        // 상단 고정 공지 개수 제한 (최대 3개)
        if (Boolean.TRUE.equals(dto.getIsPinned())) {
            long pinnedCount = noticeRepository.countByIsPinnedTrue();
            if (pinnedCount >= 3) {
                throw new BoardException("상단 고정 공지는 최대 3개까지만 등록할 수 있습니다.");
            }
        }

        Notice notice = Notice.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .isPinned(Boolean.TRUE.equals(dto.getIsPinned()))
                .imageUrls(dto.getImageUrls() != null ? dto.getImageUrls() : List.of())
                .build();
        return new NoticeResponseDto(noticeRepository.save(notice));
    }

    public List<NoticeResponseDto> getAllNotices() {
        return noticeRepository.findAll(
                        Sort.by(
                                Sort.Order.desc("isPinned"),    // 고정 여부 먼저
                                Sort.Order.desc("createdAt"),    // 그 다음 작성일 내림차순
                                Sort.Order.desc("id")           // 같은 작성일일 때 최신 id 우선
                        )
                ).stream()
                .map(NoticeResponseDto::new)
                .collect(Collectors.toList());
    }

    public NoticeResponseDto getNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new BoardException("공지사항을 찾을 수 없습니다."));
        return new NoticeResponseDto(notice);
    }

    public NoticeResponseDto updateNotice(Long id, NoticeRequestDto dto) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new BoardException("존재하지 않는 공지사항입니다."));

        // 고정 처리 시 개수 제한 체크
        if (Boolean.TRUE.equals(dto.getIsPinned()) && !notice.isPinned()) {
            long pinnedCount = noticeRepository.countByIsPinnedTrue();
            if (pinnedCount >= 3) {
                throw new BoardException("상단 고정 공지는 최대 3개까지만 등록할 수 있습니다.");
            }
        }

        notice.update(dto);
        return new NoticeResponseDto(noticeRepository.save(notice));
    }

    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new BoardException("삭제할 공지사항이 존재하지 않습니다."));
        noticeRepository.delete(notice);
    }
}
