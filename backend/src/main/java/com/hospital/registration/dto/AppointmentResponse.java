package com.hospital.registration.dto;

import com.hospital.registration.enums.AppointmentStatus;
import com.hospital.registration.enums.TimeSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        String appointmentNo,
        Long patientId,
        String patientName,
        String patientPhone,
        Long departmentId,
        String departmentName,
        Long doctorId,
        String doctorName,
        String doctorTitle,
        LocalDate appointmentDate,
        TimeSlot timeSlot,
        AppointmentStatus status,
        Boolean noticeSent,
        LocalDateTime cancelDeadline,
        Boolean canCancel,
        LocalDateTime createdAt
) {
}
