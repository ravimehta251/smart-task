package com.smarttask.auth;

import com.smarttask.auth.dto.LoginRequest;
import com.smarttask.auth.dto.RegisterRequest;
import com.smarttask.auth.entity.EmailVerificationToken;
import com.smarttask.auth.repository.EmailVerificationTokenRepository;
import com.smarttask.auth.repository.PasswordResetTokenRepository;
import com.smarttask.auth.repository.RefreshTokenRepository;
import com.smarttask.auth.service.AuthService;
import com.smarttask.common.enums.Role;
import com.smarttask.common.enums.UserStatus;
import com.smarttask.common.exception.BusinessException;
import com.smarttask.common.exception.TokenException;
import com.smarttask.config.EmailService;
import com.smarttask.config.security.JwtProperties;
import com.smarttask.config.security.JwtService;
import com.smarttask.organization.repository.OrganizationRepository;
import com.smarttask.user.entity.User;
import com.smarttask.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock JwtProperties jwtProperties;
    @Mock AuthenticationManager authenticationManager;
    @Mock EmailService emailService;

    @InjectMocks
    AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("encodedPassword")
                .role(Role.DEVELOPER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("register — should throw when email already exists")
    void register_duplicateEmail_throwsBusinessException() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("john@example.com")).thenReturn(true);

        RegisterRequest req = RegisterRequest.builder()
                .firstName("John").lastName("Doe")
                .email("john@example.com").password("password123")
                .build();

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("register — should create user and send verification email")
    void register_validRequest_createsUserAndSendsEmail() {
        when(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        RegisterRequest req = RegisterRequest.builder()
                .firstName("John").lastName("Doe")
                .email("john@example.com").password("password123")
                .build();

        var result = authService.register(req);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("john@example.com");
        assertThat(result.role()).isEqualTo(Role.DEVELOPER);
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("verifyEmail — should activate user on valid token")
    void verifyEmail_validToken_activatesUser() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("valid-token")
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        testUser.setStatus(UserStatus.PENDING_VERIFICATION);

        when(emailVerificationTokenRepository.findByTokenAndUsedFalse("valid-token"))
                .thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.verifyEmail("valid-token");

        assertThat(testUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(testUser.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("verifyEmail — should throw on expired token")
    void verifyEmail_expiredToken_throwsTokenException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("expired-token")
                .user(testUser)
                .expiryDate(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();

        when(emailVerificationTokenRepository.findByTokenAndUsedFalse("expired-token"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("changePassword — should throw when current password is wrong")
    void changePassword_wrongCurrentPassword_throwsBusinessException() {
        when(userRepository.findByIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        var req = new com.smarttask.auth.dto.ChangePasswordRequest("wrongPassword", "newPassword123");

        assertThatThrownBy(() -> authService.changePassword("user-123", req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("incorrect");
    }
}
