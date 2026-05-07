package com.hospital.registration.controller;

import com.hospital.registration.dto.ApiResponse;
import com.hospital.registration.dto.PatientRequest;
import com.hospital.registration.dto.PatientResponse;
import com.hospital.registration.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ApiResponse<List<PatientResponse>> list(HttpServletRequest request) {
        return ApiResponse.ok(patientService.list(currentUserId(request)));
    }

    @PostMapping
    public ApiResponse<PatientResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody PatientRequest patientRequest
    ) {
        return ApiResponse.ok(patientService.create(currentUserId(request), patientRequest));
    }

    @PutMapping("/{patientId}")
    public ApiResponse<PatientResponse> update(
            HttpServletRequest request,
            @PathVariable Long patientId,
            @Valid @RequestBody PatientRequest patientRequest
    ) {
        return ApiResponse.ok(patientService.update(currentUserId(request), patientId, patientRequest));
    }

    @DeleteMapping("/{patientId}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long patientId) {
        patientService.delete(currentUserId(request), patientId);
        return ApiResponse.ok(null);
    }

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
