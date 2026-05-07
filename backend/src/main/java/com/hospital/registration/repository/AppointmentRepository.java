package com.hospital.registration.repository;

import com.hospital.registration.entity.Appointment;
import com.hospital.registration.enums.AppointmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Boolean existsByPatientIdAndDepartmentIdAndAppointmentDateAndStatusIn(
            Long patientId,
            Long departmentId,
            LocalDate appointmentDate,
            Collection<AppointmentStatus> statuses
    );

    List<Appointment> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Appointment> findByIdAndUserId(Long id, Long userId);

    Long countByAppointmentDateAndStatusNot(LocalDate appointmentDate, AppointmentStatus status);

    Long countByStatus(AppointmentStatus status);

    @Query("select a.departmentId, count(a.id) from Appointment a " +
            "where a.status <> :cancelled group by a.departmentId order by count(a.id) desc")
    List<Object[]> countHotDepartments(@Param("cancelled") AppointmentStatus cancelled, Pageable pageable);
}
