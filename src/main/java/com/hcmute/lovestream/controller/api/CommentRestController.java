package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.CommentRequest;
import com.hcmute.lovestream.entity.Comment;
import com.hcmute.lovestream.repository.CommentRepository;
import com.hcmute.lovestream.service.comment.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentRestController {
    private final CommentService commentService;
    private final CommentRepository commentRepository;

    // LẤY DANH SÁCH BÌNH LUẬN THEO PHIM
    @GetMapping
    public ResponseEntity<?> getCommentsByVideo(@RequestParam String videoContentId) {
        List<Comment> comments = commentRepository
                .findByVideo_IdAndParentCommentIsNullOrderByCreatedAtDesc(videoContentId);

        List<Map<String, Object>> result = comments.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("content", c.getContent());
            map.put("userName", c.getUser() != null ? c.getUser().getFullName() : "Ẩn danh");
            map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);

            // Danh sách phản hồi
            List<Map<String, Object>> replies = c.getReplies() != null
                    ? c.getReplies().stream().map(r -> {
                        Map<String, Object> rMap = new LinkedHashMap<>();
                        rMap.put("id", r.getId());
                        rMap.put("content", r.getContent());
                        rMap.put("userName", r.getUser() != null ? r.getUser().getFullName() : "Ẩn danh");
                        rMap.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                        return rMap;
                    }).toList()
                    : List.of();
            map.put("replies", replies);

            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // PUSH BÌNH LUẬN LÊN SERVER
    @PostMapping
    public ResponseEntity<?> createComment(Principal principal, @Valid @RequestBody CommentRequest request) {
        try {
            commentService.addComment(principal.getName(), request);
            return ResponseEntity.ok("Gửi bình luận thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // SỬA BÌNH LUẬN
    @PutMapping("/{id}")
    public ResponseEntity<?> editComment(Principal principal, @PathVariable String id, @RequestBody String newContent) {
        try {
            commentService.editComment(principal.getName(), id, newContent);
            return ResponseEntity.ok("Sửa thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // XÓA BÌNH LUẬN
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(Principal principal, @PathVariable String id) {
        try {
            commentService.deleteComment(principal.getName(), id);
            return ResponseEntity.ok("Đã xóa");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // TÍNH NĂNG 4: PHẢN HỒI BÌNH LUẬN
    @PostMapping("/{parentCommentId}/replies")
    public ResponseEntity<?> replyComment(
            Principal principal,
            @PathVariable String parentCommentId,
            @Valid @RequestBody CommentRequest request) {
        try {
            commentService.replyComment(principal.getName(), parentCommentId, request);
            return ResponseEntity.ok("Đã gửi phản hồi thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
