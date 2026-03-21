package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Person;
import com.hcmute.lovestream.entity.enums.CreditType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface PersonRepository extends JpaRepository<Person, String> {
    // Lấy danh sách tất cả những người theo vai trò (chỉ đạo diễn, hoặc chỉ diễn viên)
    List<Person> findByCreditType(CreditType creditType);

    // Tìm kiếm người theo tên
    List<Person> findByFullNameContainingIgnoreCase(String fullName);
}
