package com.hospital.registration.controller;

import com.hospital.registration.dto.ApiResponse;
import com.hospital.registration.dto.DepartmentResponse;
import com.hospital.registration.dto.DoctorResponse;
import com.hospital.registration.dto.ScheduleResponse;
import com.hospital.registration.service.CatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/departments")
    public ApiResponse<List<DepartmentResponse>> departments(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(catalogService.departments(keyword));
    }

    @GetMapping("/api/departments/{departmentId}/doctors")
    public ApiResponse<List<DoctorResponse>> doctorsByDepartment(@PathVariable Long departmentId) {
        return ApiResponse.ok(catalogService.doctorsByDepartment(departmentId));
    }

    @GetMapping("/api/doctors/search")
    public ApiResponse<List<DoctorResponse>> searchDoctors(@RequestParam String keyword) {
        return ApiResponse.ok(catalogService.searchDoctors(keyword));
    }

    @GetMapping("/api/doctors/{doctorId}")
    public ApiResponse<DoctorResponse> doctor(@PathVariable Long doctorId) {
        return ApiResponse.ok(catalogService.doctor(doctorId));
    }

    @GetMapping("/api/doctors/{doctorId}/schedules")
    public ApiResponse<List<ScheduleResponse>> schedules(@PathVariable Long doctorId) {
        return ApiResponse.ok(catalogService.schedules(doctorId));
    }
}
