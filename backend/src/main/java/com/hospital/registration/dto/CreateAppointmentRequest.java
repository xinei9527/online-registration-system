package com.hospital.registration.dto;

import jakarta.validation.constraints.NotNull;

public record CreateAppointmentRequest(
        @NotNull(message = "就诊人不能为空")
        Long patientId,
        @NotNull(message = "号源不能为空")
        Long scheduleId,
        String idempotencyKey
) {
}
