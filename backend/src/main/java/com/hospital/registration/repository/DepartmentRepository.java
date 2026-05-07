package com.hospital.registration.repository;

import com.hospital.registration.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<Department> findByEnabledTrueAndNameContainingOrderBySortOrderAscIdAsc(String name);
}
