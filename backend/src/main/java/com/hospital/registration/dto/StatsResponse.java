package com.hospital.registration.dto;

import java.util.List;

public record StatsResponse(
        Long todayAppointments,
        Long pendingAppointments,
        List<DepartmentStat> hotDepartments
) {

    public record DepartmentStat(
            Long departmentId,
            String departmentName,
            Long appointmentCount
    ) {
    }
}
