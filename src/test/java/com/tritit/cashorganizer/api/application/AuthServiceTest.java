package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.AuthenticationFailedException;
import com.tritit.cashorganizer.api.domain.exception.DuplicateResourceException;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.tritit.cashorganizer.api.infrastructure.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock PersistenceMapper mapper;

    @InjectMocks
    AuthService authService;

    private UserEntity userEntity;
    private User user;

    @BeforeEach
    void setUp() {
        userEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .password("hashed_password")
                .role(User.Role.USER)
                .build();
        user = User.builder()
                .id(userEntity.getId())
                .email("user@test.com")
                .role(User.Role.USER)
                .build();
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        void returnsTokenAndEmailOnSuccess() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("pass123", "hashed_password")).thenReturn(true);
            when(mapper.toDomain(userEntity)).thenReturn(user);
            when(jwtService.generateToken(any(), any(), anyLong())).thenReturn("jwt_token");

            Map<String, String> result = authService.login("user@test.com", "pass123", false);

            assertThat(result).containsKey("token");
            assertThat(result).containsEntry("email", "user@test.com");
            assertThat(result.get("token")).isEqualTo("jwt_token");
        }

        @Test
        void usesLongerExpirationWhenRememberMe() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(mapper.toDomain(userEntity)).thenReturn(user);
            when(jwtService.generateToken(any(), any(), anyLong())).thenReturn("token");

            authService.login("user@test.com", "pass", true);
            authService.login("user@test.com", "pass", false);

            // rememberMe=true → 30 days (2592000000L), false → 1 day (86400000L)
            verify(jwtService).generateToken(any(), any(), eq(2592000000L));
            verify(jwtService).generateToken(any(), any(), eq(86400000L));
        }

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.login("nobody@test.com", "pass", false))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("Credenciales");
        }

        @Test
        void throwsWhenPasswordDoesNotMatch() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("wrongpass", "hashed_password")).thenReturn(false);

            assertThatThrownBy(() -> authService.login("user@test.com", "wrongpass", false))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("Credenciales");
        }

        @Test
        void neverGeneratesTokenOnFailedLogin() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.login("x@x.com", "pass", false))
                    .isInstanceOf(AuthenticationFailedException.class);
            verify(jwtService, never()).generateToken(any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        void savesNewUserWithEncodedPassword() {
            when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("newpass")).thenReturn("encoded_pass");

            authService.register("new@test.com", "newpass");

            verify(userRepository).save(argThat(entity ->
                    "new@test.com".equals(entity.getEmail()) &&
                    "encoded_pass".equals(entity.getPassword())
            ));
        }

        @Test
        void throwsWhenEmailAlreadyExists() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(userEntity));
            assertThatThrownBy(() -> authService.register("user@test.com", "pass"))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        void assignsUserRole() {
            when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("enc");

            authService.register("new@test.com", "pass");

            verify(userRepository).save(argThat(entity ->
                    User.Role.USER.equals(entity.getRole())
            ));
        }
    }

    @Nested
    @DisplayName("loginTest()")
    class LoginTest {

        @Test
        void returnsNonNullString() {
            assertThat(authService.loginTest()).isNotNull();
        }
    }
}
