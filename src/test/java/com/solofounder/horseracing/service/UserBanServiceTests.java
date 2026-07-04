package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.user.BanUserRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBanServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User admin;
    private User target;

    @BeforeEach
    void setUp() {
        admin = user(1L, "admin@example.com", Role.ADMIN);
        target = user(2L, "owner@example.com", Role.OWNER);
        authenticate(admin);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCanBanUser() {
        mockCurrentAdmin();
        when(userRepository.findById(target.getUserId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.banUser(
                target.getUserId(),
                BanUserRequest.builder().reason(" Violation of system rules ").build()
        );

        assertEquals("banned", response.getAccountStatus());
        assertEquals("Violation of system rules", response.getBannedReason());
        assertEquals(admin.getUserId(), response.getBannedBy());
        assertTrue(response.getBannedAt() != null);
    }

    @Test
    void adminCanUnbanUser() {
        mockCurrentAdmin();
        target.setAccountStatus("banned");
        target.setBannedReason("Reason");
        target.setBannedBy(admin.getUserId());
        when(userRepository.findById(target.getUserId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.unbanUser(target.getUserId());

        assertEquals("active", response.getAccountStatus());
        assertNull(response.getBannedReason());
        assertNull(response.getBannedAt());
        assertNull(response.getBannedBy());
    }

    @Test
    void adminCannotBanOwnAccount() {
        mockCurrentAdmin();
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.banUser(
                        admin.getUserId(),
                        BanUserRequest.builder().reason("Reason").build()
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void nonAdminCannotBanUser() {
        User owner = user(3L, "another-owner@example.com", Role.OWNER);
        authenticate(owner);
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.banUser(
                        target.getUserId(),
                        BanUserRequest.builder().reason("Reason").build()
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void responseContainsAccountStatus() {
        target.setAccountStatus("banned");

        UserResponse response = userService.toResponse(target);

        assertEquals("banned", response.getAccountStatus());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );
    }

    private void mockCurrentAdmin() {
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .userId(id)
                .fullName(email)
                .email(email)
                .passwordHash("encoded")
                .role(role)
                .status(UserStatus.ACTIVE)
                .accountStatus("active")
                .build();
    }
}
