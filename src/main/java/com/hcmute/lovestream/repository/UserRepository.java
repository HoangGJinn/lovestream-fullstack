package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, String id);


    // Queries phục vụ cho Admin / Quản lý User
    List<User> findAllByRole(Role role);
    Optional<User> findByIdAndRole(String id, Role role);
    Optional<User> findByEmailAndRole(String email, Role role);
}
