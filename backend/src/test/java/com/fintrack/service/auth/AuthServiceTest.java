package com.fintrack.service.auth;

import com.fintrack.domain.model.User;
import com.fintrack.exception.BadRequestException;
import com.fintrack.exception.UnauthorisedException;
import com.fintrack.repository.UserRepository;
import com.fintrack.security.JwtService;
import com.fintrack.web.dto.request.LoginRequest;
import com.fintrack.web.dto.request.RegisterRequest;
import com.fintrack.web.dto.response.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService depends on UserRepository, PasswordEncoder, and JwtService.
 * @ExtendWith(MockitoExtension.class) tells JUnit to process @Mock/@InjectMocks
 * annotations without needing to start Spring — this is a plain unit test,
 * just with fakes standing in for the real collaborators.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    // @Mock fields are injected by MockitoExtension AFTER the test instance is
    // constructed, so we can't build AuthService as a field initializer above —
    // the mocks would still be null at that point. @BeforeEach runs after
    // injection, so this is the correct place to wire it up.
    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_whenEmailNotTaken_createsUserAndReturnsToken() {
        // Arrange: describe how the mocks should behave for this scenario
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        // save() is called with a transient User (no id yet); simulate the DB
        // assigning an id by returning a User that has one set.
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("fake-jwt-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert: both the returned response...
        assertThat(response.accessToken()).isEqualTo("fake-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("new@example.com");
        assertThat(response.user().displayName()).isEqualTo("New User");

        // ...and that the collaborators were called the way we expect.
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_whenEmailAlreadyExists_throwsBadRequestException() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);
        assertThrows(
            BadRequestException.class,
            () -> authService.register(request)
        );
       
        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any(), any());
    }


    @Test
    void login_withValidCredentials_returnsToken() {
        LoginRequest req = new LoginRequest(
            "existing@example.com", "password123");

        UUID existingUserId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(existingUserId);
        existingUser.setEmail("existing@example.com");
        existingUser.setPasswordHash("hashed-passowrd");
        existingUser.setDisplayName("Existing User");

        when(userRepository.findByEmailAndDeletedAtIsNull("existing@example.com")).thenReturn(Optional.of(existingUser));

        when(passwordEncoder.matches("password123", existingUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(existingUserId, "existing@example.com")).thenReturn("fake-jwt-token");
        AuthResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("fake-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("existing@example.com");
        assertThat(response.user().displayName()).isEqualTo("Existing User");


    }

    void login_withWrongPassword_throwsUnauthorisedException() {
        LoginRequest req = new LoginRequest(
            "existing@example.com", "password123");

        UUID existingUserId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(existingUserId);
        existingUser.setEmail("existing@example.com");
        existingUser.setPasswordHash("hashed-passowrd");
        existingUser.setDisplayName("Existing User");

        when(userRepository.findByEmailAndDeletedAtIsNull("existing@example.com")).thenReturn(Optional.of(existingUser));

        when(passwordEncoder.matches("password123", existingUser.getPasswordHash())).thenReturn(false);
        when(jwtService.generateToken(existingUserId, "existing@example.com")).thenReturn("fake-jwt-token");
        UnauthorisedException ex = assertThrows(UnauthorisedException.class, () -> authService.login(req));
        assertThat(ex.getMessage()).isEqualTo("Invalid email or password");
    }

    void login_withUnknownEmail_throwsUnauthorisedException() {
        LoginRequest req = new LoginRequest("new@example.com", "password123");
        
        when(userRepository.findByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(Optional.empty());
        assertThrows(UnauthorisedException.class, () -> authService.login(req));

        verify(userRepository, never()).save(any());


    }
    // TODO: login_withUnknownEmail_throwsUnauthorisedException
    //   - stub userRepository.findByEmailAndDeletedAtIsNull(...) to return Optional.empty()
    //   - assert authService.login(request) throws UnauthorisedException
    //   - verify passwordEncoder.matches(...) is NEVER called (no user to check a password against)
}
