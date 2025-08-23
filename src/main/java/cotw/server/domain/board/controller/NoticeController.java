package cotw.server.domain.board.controller;

import cotw.server.domain.board.dto.request.NoticeRequestDto;
import cotw.server.domain.board.dto.response.NoticeResponseDto;
import cotw.server.domain.board.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeResponseDto> create(@RequestBody NoticeRequestDto dto) {
        return ResponseEntity.ok(noticeService.createNotice(dto));
    }

    @GetMapping
    public ResponseEntity<List<NoticeResponseDto>> getAll() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponseDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.getNotice(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoticeResponseDto> update(@PathVariable Long id,
                                                    @RequestBody NoticeRequestDto dto) {
        return ResponseEntity.ok(noticeService.updateNotice(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }
}
