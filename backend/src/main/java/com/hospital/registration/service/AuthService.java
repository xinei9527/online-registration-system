package com.hospital.registration.service;

import com.hospital.registration.dto.AuthResponse;
import com.hospital.registration.dto.LoginRequest;
import com.hospital.registration.dto.RegisterRequest;
import com.hospital.registration.entity.User;
import com.hospital.registration.enums.UserRole;
import com.hospital.registration.exception.BusinessException;
import com.hospital.registration.repository.UserRepository;
import com.hospital.registration.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean hasPhone = StringUtils.hasText(request.phone());
        boolean hasEmail = StringUtils.hasText(request.email());
        if (!hasPhone && !hasEmail) {
            throw new BusinessException("手机号和邮箱至少填写一个");
        }
        if (hasPhone && userRepository.existsByPhone(request.phone())) {
            throw new BusinessException("手机号已注册");
        }
        if (hasEmail && userRepository.existsByEmail(request.email())) {
            throw new BusinessException("邮箱已注册");
        }

        User user = new User();
        user.setPhone(hasPhone ? request.phone() : null);
        user.setEmail(hasEmail ? request.email() : null);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.PATIENT);
        userRepository.save(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByAccount(request.account())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (!matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        return toAuthResponse(user);
    }

    private boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword != null && encodedPassword.startsWith("{noop}")) {
            return rawPassword.equals(encodedPassword.substring(6));
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                user.getId(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                jwtUtil.generateToken(user.getId(), user.getRole())
        );
    }
}
