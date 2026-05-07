package com.hospital.registration.controller;

import com.hospital.registration.dto.AdminScheduleRequest;
import com.hospital.registration.dto.ApiResponse;
import com.hospital.registration.dto.ScheduleResponse;
import com.hospital.registration.dto.StatsResponse;
import com.hospital.registration.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/statistics")
    public ApiResponse<StatsResponse> statistics(HttpServletRequest request) {
        return ApiResponse.ok(adminService.statistics(currentRole(request)));
    }

    @PostMapping("/schedules")
    public ApiResponse<ScheduleResponse> upsertSchedule(
            HttpServletRequest request,
            @Valid @RequestBody AdminScheduleRequest scheduleRequest
    ) {
        return ApiResponse.ok(adminService.upsertSchedule(currentRole(request), scheduleRequest));
    }

    private String currentRole(HttpServletRequest request) {
        return (String) request.getAttribute("role");
    }
}
