package com.hospital.registration.dto;

import com.hospital.registration.enums.UserRole;

public record AuthResponse(
        Long userId,
        String phone,
        String email,
        UserRole role,
        String token
) {
}
