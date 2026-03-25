package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {
    @Query("""
    SELECT ma.assetUrl 
    FROM MediaAsset ma
    WHERE ma.videoContent.id = :id
    AND ma.assetType = com.hcmute.lovestream.entity.enums.AssetType.FULL_VIDEO
""")
    Optional<String> findVideoUrl(@Param("id") String id);
}
