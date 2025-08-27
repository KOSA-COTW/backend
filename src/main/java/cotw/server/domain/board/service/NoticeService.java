package cotw.server.domain.board.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import cotw.server.domain.board.dto.request.NoticeRequestDto;
import cotw.server.domain.board.dto.response.NoticeResponseDto;
import cotw.server.domain.board.entity.Notice;
import cotw.server.domain.board.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public NoticeResponseDto createNotice(NoticeRequestDto dto) {
        Notice notice = Notice.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .isPinned(dto.getIsPinned())
                .imageUrls(dto.getImageUrls() != null ? dto.getImageUrls() : List.of())
                .build();
        return new NoticeResponseDto(noticeRepository.save(notice));
    }

    public List<NoticeResponseDto> getAllNotices() {
        return noticeRepository.findAll(
                        Sort.by(
                                Sort.Order.desc("isPinned"),    // 고정 여부 먼저
                                Sort.Order.desc("createdAt")    // 그 다음 작성일 내림차순
                        )
                ).stream()
                .map(NoticeResponseDto::new)
                .collect(Collectors.toList());
    }

    public NoticeResponseDto getNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        return new NoticeResponseDto(notice);
    }

    public NoticeResponseDto updateNotice(Long id, NoticeRequestDto dto) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));

        notice.update(dto);
        return new NoticeResponseDto(noticeRepository.save(notice));
    }

    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }
}
