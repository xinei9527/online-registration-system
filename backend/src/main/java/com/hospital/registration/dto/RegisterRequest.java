package com.hospital.registration.dto;

import jakarta.validation.constraints.Size;

public record RegisterRequest(
        String phone,
        String email,
        @Size(min = 6, max = 64, message = "密码长度需在6到64位之间")
        String password
) {
}
