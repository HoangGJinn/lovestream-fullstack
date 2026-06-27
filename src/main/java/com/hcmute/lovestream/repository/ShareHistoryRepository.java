package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.ShareHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareHistoryRepository extends JpaRepository<ShareHistory, String> {
}
