package com.hospital.registration.service;

import com.hospital.registration.dto.AppointmentResponse;
import com.hospital.registration.dto.CreateAppointmentRequest;
import com.hospital.registration.entity.*;
import com.hospital.registration.enums.AppointmentStatus;
import com.hospital.registration.enums.ScheduleStatus;
import com.hospital.registration.exception.BusinessException;
import com.hospital.registration.repository.*;
import com.hospital.registration.util.AppointmentNoGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.COMPLETED
    );

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorScheduleRepository scheduleRepository,
            DepartmentRepository departmentRepository,
            DoctorRepository doctorRepository,
            IdempotencyRecordRepository idempotencyRecordRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.scheduleRepository = scheduleRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional
    public AppointmentResponse create(Long userId, CreateAppointmentRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var record = idempotencyRecordRepository
                    .findByUserIdAndIdempotencyKey(userId, request.idempotencyKey());
            if (record.isPresent()) {
                return getById(userId, record.get().getAppointmentId());
            }
        }

        Patient patient = patientRepository.findByIdAndUserIdAndDeletedFalse(request.patientId(), userId)
                .orElseThrow(() -> new BusinessException("就诊人不存在或不属于当前账号"));

        DoctorSchedule schedule = scheduleRepository.findLockedById(request.scheduleId())
                .orElseThrow(() -> new BusinessException("号源不存在"));
        validateScheduleCanBook(schedule);

        boolean duplicate = appointmentRepository.existsByPatientIdAndDepartmentIdAndAppointmentDateAndStatusIn(
                patient.getId(),
                schedule.getDepartmentId(),
                schedule.getScheduleDate(),
                ACTIVE_STATUSES
        );
        if (duplicate) {
            throw new BusinessException("同一就诊人同一科室同一天只能预约一次");
        }

        schedule.setRemainCount(schedule.getRemainCount() - 1);
        if (schedule.getRemainCount() == 0) {
            schedule.setStatus(ScheduleStatus.FULL);
        }
        scheduleRepository.save(schedule);

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(AppointmentNoGenerator.next());
        appointment.setUserId(userId);
        appointment.setPatientId(patient.getId());
        appointment.setDepartmentId(schedule.getDepartmentId());
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(schedule.getId());
        appointment.setAppointmentDate(schedule.getScheduleDate());
        appointment.setTimeSlot(schedule.getTimeSlot());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setNoticeSent(true);
        appointment.setCancelDeadline(LocalDateTime.now().plusMinutes(30));

        try {
            appointment = appointmentRepository.saveAndFlush(appointment);
            if (StringUtils.hasText(request.idempotencyKey())) {
                IdempotencyRecord record = new IdempotencyRecord();
                record.setUserId(userId);
                record.setIdempotencyKey(request.idempotencyKey());
                record.setAppointmentId(appointment.getId());
                idempotencyRecordRepository.saveAndFlush(record);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("预约提交重复或不符合唯一性规则，请刷新后重试");
        }

        return toResponse(appointment);
    }

    public List<AppointmentResponse> mine(Long userId) {
        return appointmentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AppointmentResponse getById(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdAndUserId(appointmentId, userId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdAndUserId(appointmentId, userId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BusinessException("当前预约状态不允许取消");
        }
        if (LocalDateTime.now().isAfter(appointment.getCancelDeadline())) {
            throw new BusinessException("预约超过30分钟，已不可取消");
        }

        DoctorSchedule schedule = scheduleRepository.findLockedById(appointment.getScheduleId())
                .orElseThrow(() -> new BusinessException("关联号源不存在"));
        schedule.setRemainCount(Math.min(schedule.getTotalCount(), schedule.getRemainCount() + 1));
        if (schedule.getStatus() != ScheduleStatus.STOPPED && schedule.getRemainCount() > 0) {
            schedule.setStatus(ScheduleStatus.AVAILABLE);
        }
        scheduleRepository.save(schedule);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());
        return toResponse(appointmentRepository.save(appointment));
    }

    private void validateScheduleCanBook(DoctorSchedule schedule) {
        LocalDate today = LocalDate.now();
        if (schedule.getScheduleDate().isBefore(today) || schedule.getScheduleDate().isAfter(today.plusDays(6))) {
            throw new BusinessException("只能预约未来7天内号源");
        }
        if (schedule.getStatus() != ScheduleStatus.AVAILABLE || schedule.getRemainCount() <= 0) {
            throw new BusinessException("当前号源不可预约");
        }
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
        Department department = departmentRepository.findById(appointment.getDepartmentId()).orElse(null);
        Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        boolean canCancel = appointment.getStatus() == AppointmentStatus.PENDING
                && !LocalDateTime.now().isAfter(appointment.getCancelDeadline());

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getPatientId(),
                patient == null ? "" : patient.getName(),
                patient == null ? "" : patient.getPhone(),
                appointment.getDepartmentId(),
                department == null ? "" : department.getName(),
                appointment.getDoctorId(),
                doctor == null ? "" : doctor.getName(),
                doctor == null ? "" : doctor.getTitle(),
                appointment.getAppointmentDate(),
                appointment.getTimeSlot(),
                appointment.getStatus(),
                appointment.getNoticeSent(),
                appointment.getCancelDeadline(),
                canCancel,
                appointment.getCreatedAt()
        );
    }
}
