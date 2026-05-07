package com.hospital.registration.dto;

import com.hospital.registration.enums.ScheduleStatus;
import com.hospital.registration.enums.TimeSlot;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdminScheduleRequest(
        @NotNull(message = "医生不能为空")
        Long doctorId,
        @NotNull(message = "日期不能为空")
        LocalDate scheduleDate,
        @NotNull(message = "时段不能为空")
        TimeSlot timeSlot,
        @Min(value = 0, message = "总号源不能小于0")
        Integer totalCount,
        @Min(value = 0, message = "剩余号源不能小于0")
        Integer remainCount,
        @NotNull(message = "状态不能为空")
        ScheduleStatus status
) {
}
