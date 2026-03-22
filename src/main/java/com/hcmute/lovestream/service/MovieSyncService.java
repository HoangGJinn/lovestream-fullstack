package com.hcmute.lovestream.service;

import com.hcmute.lovestream.dto.response.TvmazeShowDto;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MovieSyncService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository; // Tiêm thêm GenreRepository
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void fetchAndSaveFreeMovies() {
        String url = "https://api.tvmaze.com/shows";

        try {
            TvmazeShowDto[] shows = restTemplate.getForObject(url, TvmazeShowDto[].class);

            if (shows != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                int count = 0;

                for (TvmazeShowDto dto : shows) {
                    if (count >= 20) break;

                    Movie movie = new Movie();
                    movie.setTitle(dto.getName());

                    String description = dto.getSummary() != null ? dto.getSummary().replaceAll("<[^>]*>", "") : "Đang cập nhật...";
                    movie.setDescription(description);

                    if (dto.getPremiered() != null && !dto.getPremiered().isEmpty()) {
                        Date releaseDate = dateFormat.parse(dto.getPremiered());
                        movie.setReleaseDate(releaseDate);
                        movie.setReleaseYear(Integer.parseInt(dto.getPremiered().substring(0, 4)));
                    } else {
                        movie.setReleaseYear(2024);
                    }

                    movie.setAgeRating(AgeRating.PG_13);
                    movie.setQuality(Quality.FULL_HD);
                    movie.setStatus(ContentStatus.ACTIVE);
                    movie.setDurationMinutes(dto.getRuntime() != null ? dto.getRuntime() : 45);

                    if (dto.getImage() != null && dto.getImage().getOriginal() != null) {
                        MediaAsset poster = new MediaAsset(null, AssetType.POSTER, dto.getImage().getOriginal(), movie, null);
                        List<MediaAsset> assets = new ArrayList<>();
                        assets.add(poster);
                        movie.setMediaAssets(assets);
                    }

                    // ==========================================
                    // LOGIC MỚI: XỬ LÝ THỂ LOẠI (GENRE) Ở ĐÂY
                    // ==========================================
                    Set<Genre> movieGenres = new HashSet<>();
                    if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
                        for (String genreName : dto.getGenres()) {
                            // Kiểm tra xem Thể loại này đã có trong Database chưa
                            Genre genre = genreRepository.findByName(genreName)
                                    .orElseGet(() -> {
                                        // Nếu chưa có thì tạo mới và lưu luôn vào bảng genre
                                        Genre newGenre = new Genre();
                                        newGenre.setName(genreName);
                                        return genreRepository.save(newGenre);
                                    });
                            movieGenres.add(genre);
                        }
                    }
                    // Gắn danh sách thể loại vào phim
                    movie.setGenres(movieGenres);
                    // ==========================================

                    movieRepository.save(movie);
                    count++;
                }
                System.out.println("Đã đồng bộ thành công " + count + " phim kèm THỂ LOẠI từ TVmaze!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đồng bộ dữ liệu phim Free: " + e.getMessage());
        }
    }
}