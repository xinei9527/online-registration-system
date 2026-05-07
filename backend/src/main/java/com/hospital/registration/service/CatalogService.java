package com.hospital.registration.service;

import com.hospital.registration.dto.DepartmentResponse;
import com.hospital.registration.dto.DoctorResponse;
import com.hospital.registration.dto.ScheduleResponse;
import com.hospital.registration.entity.Department;
import com.hospital.registration.entity.Doctor;
import com.hospital.registration.entity.DoctorSchedule;
import com.hospital.registration.enums.ScheduleStatus;
import com.hospital.registration.exception.BusinessException;
import com.hospital.registration.repository.DepartmentRepository;
import com.hospital.registration.repository.DoctorRepository;
import com.hospital.registration.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;

    public CatalogService(
            DepartmentRepository departmentRepository,
            DoctorRepository doctorRepository,
            DoctorScheduleRepository scheduleRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<DepartmentResponse> departments(String keyword) {
        List<Department> departments = StringUtils.hasText(keyword)
                ? departmentRepository.findByEnabledTrueAndNameContainingOrderBySortOrderAscIdAsc(keyword)
                : departmentRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();
        return departments.stream().map(this::toDepartmentResponse).toList();
    }

    public List<DoctorResponse> doctorsByDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("科室不存在"));
        return doctorRepository.findByDepartmentIdAndEnabledTrueOrderByIdAsc(departmentId)
                .stream()
                .map(doctor -> toDoctorResponse(doctor, department.getName()))
                .toList();
    }

    public List<DoctorResponse> searchDoctors(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        Map<Long, Department> departmentMap = departmentRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));
        return doctorRepository.findByEnabledTrueAndNameContainingOrderByIdAsc(keyword)
                .stream()
                .map(doctor -> toDoctorResponse(doctor, departmentMap.get(doctor.getDepartmentId()).getName()))
                .toList();
    }

    public DoctorResponse doctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .filter(Doctor::getEnabled)
                .orElseThrow(() -> new BusinessException("医生不存在"));
        String departmentName = departmentRepository.findById(doctor.getDepartmentId())
                .map(Department::getName)
                .orElse("");
        return toDoctorResponse(doctor, departmentName);
    }

    public List<ScheduleResponse> schedules(Long doctorId) {
        doctorRepository.findById(doctorId)
                .filter(Doctor::getEnabled)
                .orElseThrow(() -> new BusinessException("医生不存在"));
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(6);
        return scheduleRepository.findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscTimeSlotAsc(doctorId, start, end)
                .stream()
                .map(this::toScheduleResponse)
                .toList();
    }

    private DepartmentResponse toDepartmentResponse(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getIntro());
    }

    private DoctorResponse toDoctorResponse(Doctor doctor, String departmentName) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getDepartmentId(),
                departmentName,
                doctor.getName(),
                doctor.getTitle(),
                doctor.getSpecialty(),
                doctor.getBio(),
                doctor.getAvatarUrl()
        );
    }

    private ScheduleResponse toScheduleResponse(DoctorSchedule schedule) {
        ScheduleStatus status = schedule.getStatus();
        if (status == ScheduleStatus.AVAILABLE && schedule.getRemainCount() <= 0) {
            status = ScheduleStatus.FULL;
        }
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getDoctorId(),
                schedule.getScheduleDate(),
                schedule.getTimeSlot(),
                schedule.getTotalCount(),
                schedule.getRemainCount(),
                status
        );
    }
}
