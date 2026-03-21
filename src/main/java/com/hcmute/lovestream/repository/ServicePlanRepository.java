package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.ServicePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicePlanRepository extends JpaRepository<ServicePlan, Long> {
}
