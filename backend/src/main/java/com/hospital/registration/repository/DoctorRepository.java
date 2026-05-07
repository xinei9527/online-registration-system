package com.hospital.registration.repository;

import com.hospital.registration.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByDepartmentIdAndEnabledTrueOrderByIdAsc(Long departmentId);

    List<Doctor> findByEnabledTrueAndNameContainingOrderByIdAsc(String name);
}
