package com.solofounder.horseracing.service;

import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.auth.LoginRequest;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
import com.solofounder.horseracing.repository.StaffRepository;
import com.solofounder.horseracing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthBanTests {

    @Mock private UserRepository userRepository;
    @Mock private JockeyRepository jockeyRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private RefereeRepository refereeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void bannedUserCannotLogin() {
        User bannedUser = User.builder()
                .userId(10L)
                .email("banned@example.com")
                .passwordHash("encoded")
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .accountStatus("banned")
                .build();
        when(userRepository.findByEmail(bannedUser.getEmail())).thenReturn(Optional.of(bannedUser));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(LoginRequest.builder()
                        .email(bannedUser.getEmail())
                        .password("123456")
                        .build())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Account is banned", exception.getReason());
        verify(authenticationManager, never()).authenticate(any());
    }
}
