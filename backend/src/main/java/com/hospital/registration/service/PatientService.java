package com.hospital.registration.service;

import com.hospital.registration.dto.PatientRequest;
import com.hospital.registration.dto.PatientResponse;
import com.hospital.registration.entity.Patient;
import com.hospital.registration.exception.BusinessException;
import com.hospital.registration.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponse> list(Long userId) {
        return patientRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PatientResponse create(Long userId, PatientRequest request) {
        if (patientRepository.existsByUserIdAndIdCardAndDeletedFalse(userId, request.idCard())) {
            throw new BusinessException("该身份证号已添加为就诊人");
        }
        Patient patient = new Patient();
        patient.setUserId(userId);
        patient.setName(request.name());
        patient.setIdCard(request.idCard());
        patient.setPhone(request.phone());
        patientRepository.save(patient);
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse update(Long userId, Long patientId, PatientRequest request) {
        Patient patient = patientRepository.findByIdAndUserIdAndDeletedFalse(patientId, userId)
                .orElseThrow(() -> new BusinessException("就诊人不存在"));

        boolean duplicate = patientRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .anyMatch(item -> !item.getId().equals(patientId) && item.getIdCard().equals(request.idCard()));
        if (duplicate) {
            throw new BusinessException("该身份证号已添加为就诊人");
        }

        patient.setName(request.name());
        patient.setIdCard(request.idCard());
        patient.setPhone(request.phone());
        return toResponse(patientRepository.save(patient));
    }

    @Transactional
    public void delete(Long userId, Long patientId) {
        Patient patient = patientRepository.findByIdAndUserIdAndDeletedFalse(patientId, userId)
                .orElseThrow(() -> new BusinessException("就诊人不存在"));
        patient.setDeleted(true);
        patientRepository.save(patient);
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getName(),
                patient.getIdCard(),
                maskIdCard(patient.getIdCard()),
                patient.getPhone()
        );
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
}
