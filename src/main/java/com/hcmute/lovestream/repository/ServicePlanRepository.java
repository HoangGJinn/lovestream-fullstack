package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.ServicePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePlanRepository extends JpaRepository<ServicePlan, String> {
    List<ServicePlan> findByIsActiveTrueOrderByPriceAsc();
    Optional<ServicePlan> findByIdAndIsActiveTrue(String id);
}
