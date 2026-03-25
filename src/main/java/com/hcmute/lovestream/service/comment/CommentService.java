package com.hcmute.lovestream.service.comment;

import com.hcmute.lovestream.dto.request.CommentRequest;
import com.hcmute.lovestream.entity.Comment;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.UserStatus;

import com.hcmute.lovestream.repository.CommentRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final VideoContentRepository videoContentRepository;
    private final BadWordFilterService badWordFilter;

    @Transactional
    public void addComment(String email, CommentRequest request) {

        if(badWordFilter.containsBadWord(request.getContent())){
            throw new RuntimeException("Bình luận của bạn chứa các từ ngữ không phù hợp với tiêu chuẩn cộng đồng");
        }

        User user = userRepository.findByEmail(email).orElseThrow();

        if(user.getStatus() == UserStatus.BANNED){
            throw new RuntimeException("Tài khoản của bạn đang bị tước quyền bình luận");
        }

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setUser(user);

        if(request.getVideoContentId() != null){
            VideoContent videoContent = videoContentRepository.findById(request.getVideoContentId()).orElseThrow();
            if (videoContent.getStatus() != ContentStatus.ACTIVE) {
                throw new RuntimeException("Nội dung không tồn tại hoặc đã bị ẩn");
            }
            comment.setVideo(videoContent);
        }

        commentRepository.save(comment);
    }

    @Transactional
    public void editComment(String email, String commentId, String newComment) {
        if(badWordFilter.containsBadWord(newComment)){
            throw new RuntimeException("Bình luận chứa từ ngữ không phù hợp");
        }

        Comment comment = commentRepository.findById(commentId).orElseThrow();

        if (!comment.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền sửa bình luận của người khác");
        }

        comment.setContent(newComment);
        commentRepository.save(comment);


    }

    @Transactional
    public void deleteComment(String email, String commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        if (!comment.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này");
        }
        commentRepository.delete(comment);
    }

    // KỊCH BẢN: PHẢN HỒI BÌNH LUẬN (REPLY)
    @Transactional
    public void replyComment(String email, String parentCommentId, CommentRequest request) {

        // [EXCEPTION FLOW 4B]: Kiểm tra từ cấm
        if (badWordFilter.containsBadWord(request.getContent())) {
            throw new RuntimeException("Nội dung chứa từ ngữ không phù hợp, vui lòng chỉnh sửa lại");
        }

        // [PRE-CONDITION]: Tìm User và kiểm tra quyền
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa tính năng bình luận");
        }

        // [EXCEPTION FLOW 5A]: Kiểm tra xem bình luận gốc có còn tồn tại không
        // Nếu thằng cha bị xóa rồi thì ném lỗi văng ra màn hình ngay
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Bình luận này không còn tồn tại"));

        // [MAIN FLOW 5]: Sang vòng tạo bình luận mới và lưu Database
        Comment reply = new Comment();
        reply.setContent(request.getContent());
        reply.setUser(user);

        // Đây là dòng mấu chốt: Trỏ bình luận này làm CON của bình luận gốc
        reply.setParentComment(parentComment);

        // (Tùy chọn) Kế thừa luôn thông tin bộ Phim / Tập phim từ bình luận gốc
        // Để biết cái reply này nằm trong bộ phim nào
        reply.setVideo(parentComment.getVideo());
        reply.setEpisode(parentComment.getEpisode());

        commentRepository.save(reply);
    }


}
