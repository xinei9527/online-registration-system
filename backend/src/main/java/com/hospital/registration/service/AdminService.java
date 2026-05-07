package com.hospital.registration.service;

import com.hospital.registration.dto.AdminScheduleRequest;
import com.hospital.registration.dto.ScheduleResponse;
import com.hospital.registration.dto.StatsResponse;
import com.hospital.registration.entity.Department;
import com.hospital.registration.entity.Doctor;
import com.hospital.registration.entity.DoctorSchedule;
import com.hospital.registration.enums.AppointmentStatus;
import com.hospital.registration.enums.ScheduleStatus;
import com.hospital.registration.enums.UserRole;
import com.hospital.registration.exception.BusinessException;
import com.hospital.registration.repository.AppointmentRepository;
import com.hospital.registration.repository.DepartmentRepository;
import com.hospital.registration.repository.DoctorRepository;
import com.hospital.registration.repository.DoctorScheduleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final DepartmentRepository departmentRepository;

    public AdminService(
            DoctorRepository doctorRepository,
            DoctorScheduleRepository scheduleRepository,
            AppointmentRepository appointmentRepository,
            DepartmentRepository departmentRepository
    ) {
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public ScheduleResponse upsertSchedule(String role, AdminScheduleRequest request) {
        requireAdmin(role);
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .filter(Doctor::getEnabled)
                .orElseThrow(() -> new BusinessException("医生不存在"));
        if (request.remainCount() > request.totalCount()) {
            throw new BusinessException("剩余号源不能大于总号源");
        }

        DoctorSchedule schedule = scheduleRepository
                .findByDoctorIdAndScheduleDateAndTimeSlot(request.doctorId(), request.scheduleDate(), request.timeSlot())
                .orElseGet(DoctorSchedule::new);
        schedule.setDoctorId(doctor.getId());
        schedule.setDepartmentId(doctor.getDepartmentId());
        schedule.setScheduleDate(request.scheduleDate());
        schedule.setTimeSlot(request.timeSlot());
        schedule.setTotalCount(request.totalCount());
        schedule.setRemainCount(request.remainCount());
        schedule.setStatus(normalizeStatus(request.status(), request.remainCount()));
        schedule = scheduleRepository.save(schedule);

        return new ScheduleResponse(
                schedule.getId(),
                schedule.getDoctorId(),
                schedule.getScheduleDate(),
                schedule.getTimeSlot(),
                schedule.getTotalCount(),
                schedule.getRemainCount(),
                schedule.getStatus()
        );
    }

    public StatsResponse statistics(String role) {
        requireAdmin(role);
        Map<Long, Department> departmentMap = departmentRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));

        List<StatsResponse.DepartmentStat> hotDepartments = appointmentRepository
                .countHotDepartments(AppointmentStatus.CANCELLED, PageRequest.of(0, 5))
                .stream()
                .map(row -> {
                    Long departmentId = (Long) row[0];
                    Long count = (Long) row[1];
                    Department department = departmentMap.get(departmentId);
                    return new StatsResponse.DepartmentStat(
                            departmentId,
                            department == null ? "" : department.getName(),
                            count
                    );
                })
                .toList();

        return new StatsResponse(
                appointmentRepository.countByAppointmentDateAndStatusNot(LocalDate.now(), AppointmentStatus.CANCELLED),
                appointmentRepository.countByStatus(AppointmentStatus.PENDING),
                hotDepartments
        );
    }

    private ScheduleStatus normalizeStatus(ScheduleStatus status, Integer remainCount) {
        if (status == ScheduleStatus.STOPPED) {
            return ScheduleStatus.STOPPED;
        }
        if (remainCount <= 0) {
            return ScheduleStatus.FULL;
        }
        return ScheduleStatus.AVAILABLE;
    }

    private void requireAdmin(String role) {
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new BusinessException(403, "仅管理员可访问");
        }
    }
}
