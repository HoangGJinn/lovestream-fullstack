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

    // LẤY DANH SÁCH BÌNH LUẬN THEO PHIM (Hỗ trợ đệ quy)
    @GetMapping
    public ResponseEntity<?> getCommentsByVideo(@RequestParam String videoContentId) {
        // Chỉ lấy các bình luận gốc (không có cha)
        List<Comment> rootComments = commentRepository
                .findByVideo_IdAndParentCommentIsNullOrderByCreatedAtDesc(videoContentId);

        // Map danh sách gốc bằng hàm đệ quy
        List<Map<String, Object>> result = rootComments.stream()
                .map(this::mapCommentToResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Hàm helper đệ quy để chuyển đổi Comment Entity sang Map dữ liệu
     * Hàm này sẽ tự gọi lại chính nó cho các replies
     */
    private Map<String, Object> mapCommentToResponse(Comment c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("content", c.getContent());
        map.put("userName", c.getUser() != null ? c.getUser().getFullName() : "Ẩn danh");

        // Bổ sung Email và Avatar để Frontend kiểm tra quyền xóa và hiển thị ảnh
        map.put("email", c.getUser() != null ? c.getUser().getEmail() : null);
        map.put("avatar", c.getUser() != null ? c.getUser().getAvatar() : null);

        map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);

        // Trong hàm helper mapCommentToResponse mà bạn đã sửa lúc trước, hãy thêm:
        map.put("likeCount", c.getLikeCount());
        map.put("dislikeCount", c.getDislikeCount());


        // ĐỆ QUY: Map danh sách replies của bình luận hiện tại
        List<Map<String, Object>> replies = (c.getReplies() != null)
                ? c.getReplies().stream()
                .map(this::mapCommentToResponse) // Gọi lại chính hàm này
                .toList()
                : List.of();

        map.put("replies", replies);
        return map;
    }

    // PUSH BÌNH LUẬN GỐC LÊN SERVER
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

    // PHẢN HỒI BÌNH LUẬN (Có thể phản hồi cho bất kỳ cấp nào)
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

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeComment(Principal principal, @PathVariable String id) {

        if (principal == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để bình luận!");
        }

        try {
            commentService.voteComment(principal.getName(), id, true);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<?> dislikeComment(Principal principal, @PathVariable String id) {

        if (principal == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để bình luận!");
        }

        try {
            commentService.voteComment(principal.getName(), id, false);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
