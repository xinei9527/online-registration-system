package com.hospital.registration.repository;

import com.hospital.registration.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);

    Optional<Patient> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    Boolean existsByUserIdAndIdCardAndDeletedFalse(Long userId, String idCard);
}
