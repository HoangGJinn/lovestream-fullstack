package com.hcmute.lovestream.service.contentmanager.impl;

import com.hcmute.lovestream.dto.request.contentmanager.GenreRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.service.contentmanager.GenreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Genre> getAllGenres(String sort) {
        List<Genre> genres = genreRepository.findAll();
        
        String safeSort = (sort == null) ? "name-asc" : sort;
        
        return switch (safeSort) {
            case "name-desc" -> genres.stream()
                    .sorted((g1, g2) -> g2.getName().compareToIgnoreCase(g1.getName()))
                    .toList();
            case "count-desc" -> genres.stream()
                    .sorted((g1, g2) -> {
                        int count1 = g1.getVideoContents() != null ? g1.getVideoContents().size() : 0;
                        int count2 = g2.getVideoContents() != null ? g2.getVideoContents().size() : 0;
                        if (count1 != count2) return Integer.compare(count2, count1);
                        return g1.getName().compareToIgnoreCase(g2.getName());
                    })
                    .toList();
            case "count-asc" -> genres.stream()
                    .sorted((g1, g2) -> {
                        int count1 = g1.getVideoContents() != null ? g1.getVideoContents().size() : 0;
                        int count2 = g2.getVideoContents() != null ? g2.getVideoContents().size() : 0;
                        if (count1 != count2) return Integer.compare(count1, count2);
                        return g1.getName().compareToIgnoreCase(g2.getName());
                    })
                    .toList();
            default -> genres.stream()
                    .sorted((g1, g2) -> g1.getName().compareToIgnoreCase(g2.getName()))
                    .toList();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Genre getGenreById(String id) {
        if (id == null) {
            throw new RuntimeException("ID thể loại không được để trống");
        }
        return genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với ID: " + id));
    }

    @Override
    @Transactional
    public Genre createGenre(GenreRequest request) {
        String normalizedName = normalizeName(request.getName());
        validateName(normalizedName, null);

        Genre genre = new Genre();
        genre.setName(normalizedName);
        return genreRepository.save(genre);
    }

    @Override
    @Transactional
    public Genre updateGenre(String id, GenreRequest request) {
        Genre genre = getGenreById(id);
        String normalizedName = normalizeName(request.getName());
        validateName(normalizedName, id);

        genre.setName(normalizedName);
        return genreRepository.save(genre);
    }

    @Override
    @Transactional
    public void deleteGenre(String id) {
        Genre genre = getGenreById(id);

        // Gỡ bỏ mối quan hệ many-to-many với VideoContent trước khi xóa
        if (genre.getVideoContents() != null && !genre.getVideoContents().isEmpty()) {
            for (VideoContent video : genre.getVideoContents()) {
                video.getGenres().remove(genre);
            }
        }

        genreRepository.delete(genre);
    }

    @Override
    @Transactional(readOnly = true)
    public long countVideoContentByGenre(String id) {
        Genre genre = getGenreById(id);
        return genre.getVideoContents() != null ? genre.getVideoContents().size() : 0;
    }

    private String normalizeName(String name) {
        if (name == null) return null;
        return name.trim().replaceAll("\\s+", " ");
    }

    private void validateName(String name, String excludeId) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Tên thể loại không được để trống");
        }

        // Kiểm tra ký tự đặc biệt (chỉ cho phép chữ, số, khoảng trắng và dấu gạch ngang)
        // Lưu ý: regex này đã bao gồm tiếng Việt Unicode
        String regex = "^[a-zA-Z0-9\\s\\-ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂÂÊÔƠƯ\\u00C0-\\u024F\\u1E00-\\u1EFF]+$";
        if (!Pattern.matches(regex, name)) {
            throw new RuntimeException("Tên thể loại chứa ký tự không hợp lệ");
        }

        // Kiểm tra trùng tên (không phân biệt hoa thường)
        Genre existingGenre = genreRepository.findByNameIgnoreCase(name).orElse(null);
        if (existingGenre != null && !existingGenre.getId().equals(excludeId)) {
                throw new RuntimeException("Thể loại '" + name + "' đã tồn tại");
        }
    }
}
