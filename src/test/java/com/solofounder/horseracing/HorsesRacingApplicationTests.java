package com.solofounder.horseracing;

import tools.jackson.databind.ObjectMapper;
import com.solofounder.horseracing.dto.auth.AuthResponse;
import com.solofounder.horseracing.dto.auth.LoginRequest;
import com.solofounder.horseracing.dto.auth.RegisterRequest;
import com.solofounder.horseracing.dto.user.CreateInternalUserRequest;
import com.solofounder.horseracing.dto.user.UpdateUserStatusRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.UserRepository;
import com.solofounder.horseracing.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HorsesRacingApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private static final java.util.Set<Long> preExistingAdminIds = new java.util.HashSet<>();

    @BeforeEach
    void cleanDatabase() {
        if (preExistingAdminIds.isEmpty()) {
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .map(User::getUserId)
                    .forEach(preExistingAdminIds::add);
        }
        userRepository.findAll().stream()
                .filter(u -> !preExistingAdminIds.contains(u.getUserId()))
                .forEach(userRepository::delete);
        userRepository.flush();
    }

    // Helper to get JWT token by logging in default admin (or registering a user)
    private String getAdminToken() throws Exception {
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.getStatus() == UserStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active ADMIN user found in database"));
        return "Bearer " + jwtService.generateToken(admin);
    }

    private String getHorseOwnerToken() throws Exception {
        RegisterRequest register = RegisterRequest.builder()
                .fullName("Owner A")
                .email("owner@gmail.com")
                .password("123456")
                .role(Role.HORSE_OWNER)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
            );
        return "Bearer " + response.getToken();
    }

    // 1. Register HORSE_OWNER -> Expected: 200 OK and token returned
    @Test
    void testRegisterHorseOwner() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Owner A")
                .email("owner@gmail.com")
                .password("123456")
                .role(Role.HORSE_OWNER)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(response.getToken());
        assertEquals("owner@gmail.com", response.getEmail());
        assertEquals(Role.HORSE_OWNER, response.getRole());
    }

    // 2. Register JOCKEY -> Expected: 200 OK and token returned
    @Test
    void testRegisterJockey() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Jockey A")
                .email("jockey@gmail.com")
                .password("123456")
                .role(Role.JOCKEY)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(response.getToken());
        assertEquals("jockey@gmail.com", response.getEmail());
        assertEquals(Role.JOCKEY, response.getRole());
    }

    // 3. Register SPECTATOR -> Expected: 200 OK and token returned
    @Test
    void testRegisterSpectator() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Spectator A")
                .email("spectator@gmail.com")
                .password("123456")
                .role(Role.SPECTATOR)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(response.getToken());
        assertEquals("spectator@gmail.com", response.getEmail());
        assertEquals(Role.SPECTATOR, response.getRole());
    }

    // 4. Register ADMIN using /api/auth/register -> Expected: 400 Bad Request (restricted role)
    @Test
    void testRegisterAdminPubliclyFails() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Admin A")
                .email("admin_new@gmail.com")
                .password("123456")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // 5. Register RACE_REFEREE using /api/auth/register -> Expected: 400 Bad Request (restricted role)
    @Test
    void testRegisterRefereePubliclyFails() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Referee A")
                .email("referee_new@gmail.com")
                .password("123456")
                .role(Role.RACE_REFEREE)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // 6. Login with correct email/password -> Expected: 200 OK and token returned
    @Test
    void testLoginWithCorrectCredentials() throws Exception {
        // First register
        testRegisterHorseOwner();

        LoginRequest login = LoginRequest.builder()
                .email("owner@gmail.com")
                .password("123456")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(response.getToken());
        assertEquals("owner@gmail.com", response.getEmail());
    }

    // 7. Login with wrong password -> Expected: error (400 Bad Request or 401 Unauthorized depend on Security mapping, AuthController returns 400 Bad Request)
    @Test
    void testLoginWithWrongPassword() throws Exception {
        testRegisterHorseOwner();

        LoginRequest login = LoginRequest.builder()
                .email("owner@gmail.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }

    // 8. Call POST /api/admin/users without token -> Expected: 401 Unauthorized
    @Test
    void testAdminCreateUserWithoutToken() throws Exception {
        CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                .fullName("Referee A")
                .email("referee@gmail.com")
                .password("123456")
                .role(Role.RACE_REFEREE)
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // 9. Call POST /api/admin/users with HORSE_OWNER token -> Expected: 403 Forbidden
    @Test
    void testAdminCreateUserWithOwnerToken() throws Exception {
        String ownerToken = getHorseOwnerToken();

        CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                .fullName("Referee A")
                .email("referee@gmail.com")
                .password("123456")
                .role(Role.RACE_REFEREE)
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // 10. Call POST /api/admin/users with ADMIN token -> Expected: 200 OK and internal account created
    @Test
    void testAdminCreateUserWithAdminToken() throws Exception {
        String adminToken = getAdminToken();

        CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                .fullName("Referee A")
                .email("referee@gmail.com")
                .password("123456")
                .role(Role.RACE_REFEREE)
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
        assertNotNull(response.getUserId());
        assertEquals("referee@gmail.com", response.getEmail());
        assertEquals(Role.RACE_REFEREE, response.getRole());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
    }

    // 11. Admin creates RACE_REFEREE account -> Expected: account created successfully, password stored as BCrypt hash
    @Test
    void testAdminCreatesRefereeAndBCryptHash() throws Exception {
        testAdminCreateUserWithAdminToken();

        User savedUser = userRepository.findByEmail("referee@gmail.com").orElse(null);
        assertNotNull(savedUser);
        assertTrue(passwordEncoder.matches("123456", savedUser.getPasswordHash()));
        assertNotEquals("123456", savedUser.getPasswordHash());
    }

    // 12. Admin tries to create HORSE_OWNER using /api/admin/users -> Expected: error
    @Test
    void testAdminCreatesHorseOwnerFails() throws Exception {
        String adminToken = getAdminToken();

        CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                .fullName("Owner B")
                .email("ownerb@gmail.com")
                .password("123456")
                .role(Role.HORSE_OWNER)
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllUsersWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllUsersWithOwnerToken() throws Exception {
        String ownerToken = getHorseOwnerToken();

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllUsersWithAdminToken() throws Exception {
        String adminToken = getAdminToken();

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        String adminToken = getAdminToken();

        mockMvc.perform(get("/api/admin/users/{id}", 999999L)
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUserStatusWithAdminToken() throws Exception {
        String adminToken = getAdminToken();

        User user = User.builder()
                .fullName("Status User")
                .email("status-user@gmail.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .role(Role.SPECTATOR)
                .status(UserStatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(user);

        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder()
                .status(UserStatus.SUSPENDED)
                .build();

        MvcResult result = mockMvc.perform(patch("/api/admin/users/{id}/status", savedUser.getUserId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
        assertEquals(UserStatus.SUSPENDED, response.getStatus());
    }

    @Test
    void testAdminCreatesPublicRolesFails() throws Exception {
        String adminToken = getAdminToken();

        for (Role role : java.util.List.of(Role.HORSE_OWNER, Role.JOCKEY, Role.SPECTATOR)) {
            CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                    .fullName("Public Role " + role.name())
                    .email(role.name().toLowerCase() + "@gmail.com")
                    .password("123456")
                    .role(role)
                    .build();

            mockMvc.perform(post("/api/admin/users")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }
}
