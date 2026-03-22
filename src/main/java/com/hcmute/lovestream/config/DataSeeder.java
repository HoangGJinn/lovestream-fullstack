package com.hcmute.lovestream.config;

import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.service.MovieSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final MovieSyncService movieSyncService;

    @Override
    public void run(String... args) throws Exception {
        // Nếu DB trống, tự động cào API miễn phí
        if (movieRepository.count() == 0) {
            System.out.println("Phát hiện Database trống, đang bắt đầu lấy dữ liệu phim (Free API)...");
            movieSyncService.fetchAndSaveFreeMovies();
        }
    }
}