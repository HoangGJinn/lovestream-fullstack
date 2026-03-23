package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.FavoriteList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

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
}
