package com.hospital.registration.service;

import com.hospital.registration.dto.AppointmentResponse;
import com.hospital.registration.dto.CreateAppointmentRequest;
import com.hospital.registration.entity.*;
import com.hospital.registration.enums.AppointmentStatus;
import com.hospital.registration.enums.ScheduleStatus;
import com.hospital.registration.enums.TimeSlot;
import com.hospital.registration.exception.BusinessException;
import com.hospital.registration.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PATIENT_ID = 10L;
    private static final Long DEPARTMENT_ID = 20L;
    private static final Long DOCTOR_ID = 30L;
    private static final Long SCHEDULE_ID = 40L;

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorScheduleRepository scheduleRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository,
                patientRepository,
                scheduleRepository,
                departmentRepository,
                doctorRepository,
                idempotencyRecordRepository
        );
    }

    @Test
    void createRejectsDuplicateAppointmentInSameDepartmentOnSameDay() {
        Patient patient = patient();
        DoctorSchedule schedule = schedule(5, ScheduleStatus.AVAILABLE);
        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(USER_ID, "dup-key"))
                .thenReturn(Optional.empty());
        when(patientRepository.findByIdAndUserIdAndDeletedFalse(PATIENT_ID, USER_ID))
                .thenReturn(Optional.of(patient));
        when(scheduleRepository.findLockedById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByPatientIdAndDepartmentIdAndAppointmentDateAndStatusIn(
                eq(PATIENT_ID), eq(DEPARTMENT_ID), eq(schedule.getScheduleDate()), anyList()
        )).thenReturn(true);

        assertThrows(
                BusinessException.class,
                () -> appointmentService.create(USER_ID, new CreateAppointmentRequest(PATIENT_ID, SCHEDULE_ID, "dup-key"))
        );

        verify(scheduleRepository, never()).save(any());
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDeductsQuotaAndMarksScheduleFullWhenLastSlotIsBooked() {
        Patient patient = patient();
        DoctorSchedule schedule = schedule(1, ScheduleStatus.AVAILABLE);
        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(USER_ID, "book-key"))
                .thenReturn(Optional.empty());
        when(patientRepository.findByIdAndUserIdAndDeletedFalse(PATIENT_ID, USER_ID))
                .thenReturn(Optional.of(patient));
        when(scheduleRepository.findLockedById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByPatientIdAndDepartmentIdAndAppointmentDateAndStatusIn(anyLong(), anyLong(), any(), anyList()))
                .thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenAnswer(invocation -> persistedAppointment(invocation.getArgument(0), 100L));
        when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubResponseLookups();

        AppointmentResponse response = appointmentService.create(
                USER_ID,
                new CreateAppointmentRequest(PATIENT_ID, SCHEDULE_ID, "book-key")
        );

        assertEquals(AppointmentStatus.PENDING, response.status());
        assertTrue(response.noticeSent());
        assertEquals(0, schedule.getRemainCount());
        assertEquals(ScheduleStatus.FULL, schedule.getStatus());
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void cancelReleasesQuotaAndRestoresAvailableStatus() {
        Appointment appointment = appointment(AppointmentStatus.PENDING, LocalDateTime.now().plusMinutes(10));
        DoctorSchedule schedule = schedule(0, ScheduleStatus.FULL);
        when(appointmentRepository.findByIdAndUserId(100L, USER_ID))
                .thenReturn(Optional.of(appointment));
        when(scheduleRepository.findLockedById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubResponseLookups();

        AppointmentResponse response = appointmentService.cancel(USER_ID, 100L);

        assertEquals(AppointmentStatus.CANCELLED, response.status());
        assertFalse(response.canCancel());
        assertEquals(1, schedule.getRemainCount());
        assertEquals(ScheduleStatus.AVAILABLE, schedule.getStatus());
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void cancelRejectsAppointmentAfterThirtyMinuteWindow() {
        Appointment appointment = appointment(AppointmentStatus.PENDING, LocalDateTime.now().minusMinutes(1));
        when(appointmentRepository.findByIdAndUserId(100L, USER_ID))
                .thenReturn(Optional.of(appointment));

        assertThrows(BusinessException.class, () -> appointmentService.cancel(USER_ID, 100L));

        verify(scheduleRepository, never()).findLockedById(anyLong());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createReturnsExistingAppointmentWhenIdempotencyKeyIsRepeated() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setUserId(USER_ID);
        record.setIdempotencyKey("same-key");
        record.setAppointmentId(100L);
        Appointment appointment = appointment(AppointmentStatus.PENDING, LocalDateTime.now().plusMinutes(10));
        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(USER_ID, "same-key"))
                .thenReturn(Optional.of(record));
        when(appointmentRepository.findByIdAndUserId(100L, USER_ID))
                .thenReturn(Optional.of(appointment));
        stubResponseLookups();

        AppointmentResponse response = appointmentService.create(
                USER_ID,
                new CreateAppointmentRequest(PATIENT_ID, SCHEDULE_ID, "same-key")
        );

        assertEquals("GH202605080001", response.appointmentNo());
        verify(scheduleRepository, never()).findLockedById(anyLong());
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setUserId(USER_ID);
        patient.setName("Test Patient");
        patient.setPhone("13900000000");
        patient.setIdCard("110101199001011234");
        return patient;
    }

    private DoctorSchedule schedule(int remainCount, ScheduleStatus status) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setDoctorId(DOCTOR_ID);
        schedule.setDepartmentId(DEPARTMENT_ID);
        schedule.setScheduleDate(LocalDate.now().plusDays(1));
        schedule.setTimeSlot(TimeSlot.AM);
        schedule.setTotalCount(10);
        schedule.setRemainCount(remainCount);
        schedule.setStatus(status);
        return schedule;
    }

    private Appointment appointment(AppointmentStatus status, LocalDateTime cancelDeadline) {
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setAppointmentNo("GH202605080001");
        appointment.setUserId(USER_ID);
        appointment.setPatientId(PATIENT_ID);
        appointment.setDepartmentId(DEPARTMENT_ID);
        appointment.setDoctorId(DOCTOR_ID);
        appointment.setScheduleId(SCHEDULE_ID);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setTimeSlot(TimeSlot.AM);
        appointment.setStatus(status);
        appointment.setNoticeSent(true);
        appointment.setCancelDeadline(cancelDeadline);
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointment;
    }

    private Appointment persistedAppointment(Appointment appointment, Long id) {
        appointment.setId(id);
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointment;
    }

    private void stubResponseLookups() {
        Department department = new Department();
        department.setId(DEPARTMENT_ID);
        department.setName("Internal Medicine");

        Doctor doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        doctor.setName("Doctor Wang");
        doctor.setTitle("Chief Physician");

        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient()));
        when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.of(department));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
    }
}
