package com.hospital.registration.repository;

import com.hospital.registration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    Boolean existsByPhone(String phone);

    Boolean existsByEmail(String email);

    @Query("select u from User u where u.phone = :account or u.email = :account")
    Optional<User> findByAccount(@Param("account") String account);
}
