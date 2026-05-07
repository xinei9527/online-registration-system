package com.hospital.registration.controller;

import com.hospital.registration.dto.ApiResponse;
import com.hospital.registration.dto.AppointmentResponse;
import com.hospital.registration.dto.CreateAppointmentRequest;
import com.hospital.registration.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ApiResponse<AppointmentResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody CreateAppointmentRequest appointmentRequest
    ) {
        return ApiResponse.ok(appointmentService.create(currentUserId(request), appointmentRequest));
    }

    @GetMapping("/my")
    public ApiResponse<List<AppointmentResponse>> mine(HttpServletRequest request) {
        return ApiResponse.ok(appointmentService.mine(currentUserId(request)));
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<AppointmentResponse> detail(HttpServletRequest request, @PathVariable Long appointmentId) {
        return ApiResponse.ok(appointmentService.getById(currentUserId(request), appointmentId));
    }

    @PostMapping("/{appointmentId}/cancel")
    public ApiResponse<AppointmentResponse> cancel(HttpServletRequest request, @PathVariable Long appointmentId) {
        return ApiResponse.ok(appointmentService.cancel(currentUserId(request), appointmentId));
    }

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
