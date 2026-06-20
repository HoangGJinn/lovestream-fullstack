package com.hcmute.lovestream.factory;

import com.hcmute.lovestream.entity.VideoContent;

/**
 * Trừu tượng hóa logic khởi tạo các loại Video Content (Movie, TV Series)
 */
public interface VideoContentFactory<R, T extends VideoContent> {
    
    /**
     * Khởi tạo đối tượng VideoContent từ DTO request
     * 
     * @param request DTO chứa các tham số từ form
     * @param posterUrl URL ảnh poster (nếu có)
     * @param trailerUrl URL video trailer (nếu có)
     * @return Đối tượng VideoContent đã được khởi tạo và thiết lập các thông tin cơ bản
     */
    T createContent(R request, String posterUrl, String trailerUrl);
}
