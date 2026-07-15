package com.userdocumentportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.userdocumentportal.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByCompanyName(String companyName);

    @Query("SELECT DISTINCT u.companyName FROM User u WHERE u.companyName IS NOT NULL AND u.companyName != ''")
    List<String> findAllCompanyNames();
}
