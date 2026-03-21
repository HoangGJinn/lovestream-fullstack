package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.ContentCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentCreditRepository extends JpaRepository<ContentCredit, String> {

}
