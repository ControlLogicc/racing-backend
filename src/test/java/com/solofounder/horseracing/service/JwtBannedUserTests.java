package com.solofounder.horseracing.service;

import com.solofounder.horseracing.config.JwtAuthenticationFilter;
import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtBannedUserTests {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void oldTokenFromBannedUserIsRejected() throws Exception {
        String token = "old-valid-token";
        User bannedUser = User.builder()
                .userId(20L)
                .email("banned@example.com")
                .passwordHash("encoded")
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .accountStatus("banned")
                .build();
        when(jwtService.extractUsername(token)).thenReturn(bannedUser.getEmail());
        when(userDetailsService.loadUserByUsername(bannedUser.getEmail())).thenReturn(bannedUser);
        when(jwtService.isTokenValid(token, bannedUser)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/owner/horses");
        request.setServletPath("/api/owner/horses");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationFilter(jwtService, userDetailsService)
                .doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Account is banned"));
        verify(filterChain, never()).doFilter(request, response);
    }
}
