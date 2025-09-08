package cotw.server.domain.board.controller;

import cotw.server.domain.board.dto.request.NoticeRequestDto;
import cotw.server.domain.board.dto.response.NoticeResponseDto;
import cotw.server.domain.board.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 공지사항 생성
     */
    @PostMapping
    public ResponseEntity<NoticeResponseDto> create(@Valid @RequestBody NoticeRequestDto dto) {
        return ResponseEntity.ok(noticeService.createNotice(dto));
    }

    /**
     * 모든 공지사항 조회
     */
    @GetMapping
    public ResponseEntity<List<NoticeResponseDto>> getAll() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    /**
     * 공지사항 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponseDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.getNotice(id));
    }

    /**
     * 공지사항 수정
     */
    @PatchMapping("/{id}")
    public ResponseEntity<NoticeResponseDto> update(@PathVariable Long id,
                                                    @Valid @RequestBody NoticeRequestDto dto) {
        return ResponseEntity.ok(noticeService.updateNotice(id, dto));
    }

    /**
     * 공지사항 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }
}