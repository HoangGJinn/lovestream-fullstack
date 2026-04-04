package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Device;
import com.hcmute.lovestream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    Optional<Device> findByUserAndClientDeviceId(User user, String clientDeviceId);

    List<Device> findByUserOrderByLastLoginDesc(User user);
    List<Device> findByUserAndIsActiveTrueOrderByLastLoginDesc(User user);
    List<Device> findByUser_EmailAndIsActiveTrueOrderByLastLoginDesc(String email);
    Optional<Device> findByUser_IdAndClientDeviceId(String userId, String clientDeviceId);

    Optional<Device> findByUser_EmailAndClientDeviceId(String email, String clientDeviceId);
}
