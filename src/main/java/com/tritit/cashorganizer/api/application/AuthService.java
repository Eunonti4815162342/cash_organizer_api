package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.AuthenticationFailedException;
import com.tritit.cashorganizer.api.domain.exception.DuplicateResourceException;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PasswordResetTokenRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.tritit.cashorganizer.api.infrastructure.config.CustomUserDetails;
import com.tritit.cashorganizer.api.infrastructure.config.EmailService;
import com.tritit.cashorganizer.api.infrastructure.config.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PersistenceMapper mapper;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    public void register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("El usuario ya existe");
        }
        UserEntity userEntity = UserEntity.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(User.Role.USER)
                .build();
        userRepository.save(userEntity);
    }

    public Map<String, String> login(String email, String password, boolean rememberMe) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, userEntity.getPassword())) {
            throw new AuthenticationFailedException("Credenciales inválidas");
        }

        User user = mapper.toDomain(userEntity);
        long expiration = rememberMe ? 2592000000L : 86400000L;
        String token = jwtService.generateToken(new HashMap<>(), new CustomUserDetails(user), expiration);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("email", user.getEmail());
        return response;
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            resetTokenRepository.deleteByEmail(email);

            String token = UUID.randomUUID().toString();
            PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                    .email(email)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();
            resetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(email, token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetTokenEntity resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationFailedException("Token inválido o expirado"));

        if (resetToken.isUsed()) {
            throw new AuthenticationFailedException("Este token ya ha sido utilizado");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthenticationFailedException("El token ha expirado");
        }

        UserEntity user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }
}
