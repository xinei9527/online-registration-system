package com.hospital.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PatientRequest(
        @NotBlank(message = "姓名不能为空")
        String name,
        @NotBlank(message = "身份证号不能为空")
        String idCard,
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone
) {
}
