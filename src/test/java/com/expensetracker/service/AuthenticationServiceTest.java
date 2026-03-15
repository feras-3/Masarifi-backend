package com.expensetracker.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtUtil;

/**
 * Unit tests for AuthenticationService.
 * Covers: AS-01 through AS-06 (TDD §4.2)
 * Requirements: FR-AUTH-01, FR-AUTH-02, FR-AUTH-03, FR-AUTH-04, FR-AUTH-08
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User("alice", "$2a$10$hashedpassword");
        existingUser.setId("user-001");
    }

    // AS-01
    @Test
    void register_WithNewUsername_CreatesUser() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authenticationService.register("newuser", "pass");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    // AS-02
    @Test
    void register_WithDuplicateUsername_ThrowsException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));

        assertThrows(RuntimeException.class,
                () -> authenticationService.register("alice", "anypass"));
        verify(userRepository, never()).save(any());
    }

    // AS-03
    @Test
    void login_WithValidCredentials_ReturnsToken() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correctpass", existingUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token-abc");

        String token = authenticationService.login("alice", "correctpass");

        assertEquals("jwt-token-abc", token);
    }

    // AS-04
    @Test
    void login_WithWrongPassword_ThrowsException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongpass", existingUser.getPassword())).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> authenticationService.login("alice", "wrongpass"));
    }

    // AS-05
    @Test
    void login_WithNonExistentUser_ThrowsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authenticationService.login("ghost", "pass"));
    }

    // AS-06
    @Test
    void register_PasswordIsHashed() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticationService.register("newuser", "plaintext");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotEquals("plaintext", captor.getValue().getPassword());
    }
}
