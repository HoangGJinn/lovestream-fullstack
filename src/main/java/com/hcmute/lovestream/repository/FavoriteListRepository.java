package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.FavoriteList;
import com.hcmute.lovestream.entity.VideoContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteListRepository extends JpaRepository<FavoriteList, String> {

	@Query("""
			select f.video.id, count(f)
			from FavoriteList f
			where f.video.id in :videoIds
			group by f.video.id
			""")
	List<Object[]> countFavoritesByVideoIds(@Param("videoIds") Collection<String> videoIds);

	@Query("""
			select g.name
			from FavoriteList f
			join f.video v
			join v.genres g
			where f.user.id = :userId
			""")
	List<String> findFavoriteGenreNamesByUserId(@Param("userId") String userId);

	boolean existsByUserIdAndVideoId(String userId, String videoId);

	Optional<FavoriteList> findByUserIdAndVideoId(String userId, String videoId);

	void deleteByUserIdAndVideoId(String userId, String videoId);

	@Query("""
			select f
			from FavoriteList f
			left join f.video.genres g
			where f.user.id = :userId
			  and (:keyword is null or lower(f.video.title) like lower(concat('%', :keyword, '%')))
			  and (:year is null or f.video.releaseYear = :year)
			  and (:genre is null or g.name = :genre)
			order by f.addedAt desc
			""")
	List<FavoriteList> findFilteredFavorites(
			@Param("userId") String userId,
			@Param("keyword") String keyword,
			@Param("year") Integer year,
			@Param("genre") String genre
	);
}
