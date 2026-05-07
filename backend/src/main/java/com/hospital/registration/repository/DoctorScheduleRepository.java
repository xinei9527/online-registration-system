package com.hospital.registration.repository;

import com.hospital.registration.entity.DoctorSchedule;
import com.hospital.registration.enums.TimeSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorSchedule s where s.id = :id")
    Optional<DoctorSchedule> findLockedById(@Param("id") Long id);

    List<DoctorSchedule> findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscTimeSlotAsc(
            Long doctorId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<DoctorSchedule> findByDoctorIdAndScheduleDateAndTimeSlot(Long doctorId, LocalDate scheduleDate, TimeSlot timeSlot);
}
