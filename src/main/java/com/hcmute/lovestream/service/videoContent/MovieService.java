package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.dto.response.MovieResponse;
import com.hcmute.lovestream.mapper.MovieMapper;
import com.hcmute.lovestream.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)

public class MovieService {
    MovieMapper movieMapper;
    MovieRepository movieRepository;

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(movieMapper::toMovieResponse)
                .collect(Collectors.toList());
    }
}
