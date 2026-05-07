package com.hospital.registration.dto;

public record PatientResponse(
        Long id,
        String name,
        String idCard,
        String maskedIdCard,
        String phone
) {
}
