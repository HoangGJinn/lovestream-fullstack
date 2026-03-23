package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.CommentRequest;
import com.hcmute.lovestream.service.comment.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentRestController {
    private final CommentService commentService;
    // PUSH BÌNH LUẬN LÊN SERVER
    @PostMapping
    public ResponseEntity<?> createComment(Principal principal, @Valid @RequestBody CommentRequest request) {
        try {
            commentService.addComment(principal.getName(), request);
            return ResponseEntity.ok("Gửi bình luận thành công");
        } catch (RuntimeException e) {
            // Ném thông báo lỗi chữ màu đỏ lên màn hình Frontend (Ví dụ: Từ ngữ vi phạm)
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
            // [EXCEPTION FLOW 1A]: Spring Security tự chặn nếu chưa đăng nhập (không có Token / principal)

            // [EXCEPTION FLOW 4A]: Chặn bình luận rỗng đã được @Valid và thư viện validation lo tự động

            commentService.replyComment(principal.getName(), parentCommentId, request);
            return ResponseEntity.ok("Đã gửi phản hồi thành công");
        } catch (RuntimeException e) {
            // Dùng cho Exception Flow 4B và 5A (Ném thông báo lỗi chữ màu đỏ lên cho Frontend hiển thị)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
