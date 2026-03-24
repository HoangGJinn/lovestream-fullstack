package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {
    List<MediaAsset> findByVideoContent_Id(String videoContentId);
    java.util.Optional<MediaAsset> findByVideoContent_IdAndAssetType(String videoContentId, com.hcmute.lovestream.entity.enums.AssetType assetType);
}
