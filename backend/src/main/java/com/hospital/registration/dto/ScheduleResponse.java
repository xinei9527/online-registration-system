package com.hospital.registration.dto;

import com.hospital.registration.enums.ScheduleStatus;
import com.hospital.registration.enums.TimeSlot;

import java.time.LocalDate;

public record ScheduleResponse(
        Long id,
        Long doctorId,
        LocalDate scheduleDate,
        TimeSlot timeSlot,
        Integer totalCount,
        Integer remainCount,
        ScheduleStatus status
) {
}
