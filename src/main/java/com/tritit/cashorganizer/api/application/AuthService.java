package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.tritit.cashorganizer.api.infrastructure.config.CustomUserDetails;
import com.tritit.cashorganizer.api.infrastructure.config.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PersistenceMapper mapper;

    public void register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("El usuario ya existe");
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
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(password, userEntity.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        User user = mapper.toDomain(userEntity);
        long expiration = rememberMe ? 2592000000L : 86400000L;
        String token = jwtService.generateToken(new HashMap<>(), new CustomUserDetails(user), expiration);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("email", user.getEmail());
        return response;
    }
}
