package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, String> {

	@Query("""
			select r.videoContent.id, avg(r.score), count(r)
			from Rating r
			where r.videoContent.id in :videoIds
			group by r.videoContent.id
			""")
	List<Object[]> findRatingStatsByVideoIds(@Param("videoIds") Collection<String> videoIds);

	@Query("""
			select r.videoContent.id, r.score
			from Rating r
			where r.user.id = :userId
			""")
	List<Object[]> findUserScoresByUserId(@Param("userId") String userId);

	@Query("""
			select g.name
			from Rating r
			join r.videoContent v
			join v.genres g
			where r.user.id = :userId and r.score >= 4
			""")
	List<String> findPreferredGenreNamesByUserId(@Param("userId") String userId);
}
