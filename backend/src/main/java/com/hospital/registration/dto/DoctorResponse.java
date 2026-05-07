package com.hospital.registration.dto;

public record DoctorResponse(
        Long id,
        Long departmentId,
        String departmentName,
        String name,
        String title,
        String specialty,
        String bio,
        String avatarUrl
) {
}
