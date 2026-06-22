package com.solofounder.horseracing;

import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.admin.CreateInternalAccountRequest;
import com.solofounder.horseracing.dto.admin.InternalAccountResponse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
import com.solofounder.horseracing.repository.StaffRepository;
import com.solofounder.horseracing.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InternalAccountIntegrationTests {

    private static final String TEST_DOMAIN = "@internal-account-test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private JockeyRepository jockeyRepository;

    @Autowired
    private RefereeRepository refereeRepository;

    private String suffix;
    private String adminToken;
    private String ownerToken;
    private String jockeyToken;
    private String staffToken;

    @BeforeEach
    void setup() {
        cleanup();
        suffix = String.valueOf(System.nanoTime());

        User admin = createUser("admin-" + suffix + TEST_DOMAIN, "Internal Admin", Role.ADMIN);
        adminToken = "Bearer " + jwtService.generateToken(admin);

        User owner = createUser("owner-" + suffix + TEST_DOMAIN, "Internal Owner", Role.OWNER);
        ownerToken = "Bearer " + jwtService.generateToken(owner);

        User jockey = createUser("jockey-token-" + suffix + TEST_DOMAIN, "Internal Jockey Token", Role.JOCKEY);
        jockeyToken = "Bearer " + jwtService.generateToken(jockey);

        User staff = createUser("staff-token-" + suffix + TEST_DOMAIN, "Internal Staff Token", Role.STAFF);
        staffRepository.save(Staff.builder()
                .user(staff)
                .staffCode("TOKEN-STF-" + suffix)
                .department("Operations")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());
        staffToken = "Bearer " + jwtService.generateToken(staff);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void testAdminCreateStaffWithProfileSuccess() throws Exception {
        CreateInternalAccountRequest request = baseRequest("new-staff-" + suffix + TEST_DOMAIN, Role.STAFF)
                .staffCode("STF-" + suffix)
                .department("Operations")
                .status("active")
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        InternalAccountResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InternalAccountResponse.class);
        assertNotNull(response.getUserId());
        assertNotNull(response.getProfileId());
        assertEquals(Role.STAFF, response.getRole());
        assertEquals(request.getEmail(), response.getEmail());
        assertTrue(staffRepository.findById(response.getProfileId()).isPresent());
    }

    @Test
    void testAdminCreateJockeyWithProfileSuccess() throws Exception {
        CreateInternalAccountRequest request = baseRequest("new-jockey-" + suffix + TEST_DOMAIN, Role.JOCKEY)
                .weight(new BigDecimal("52.50"))
                .experienceYears((short) 3)
                .status("available")
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        InternalAccountResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InternalAccountResponse.class);
        assertNotNull(response.getUserId());
        assertNotNull(response.getProfileId());
        assertEquals(Role.JOCKEY, response.getRole());
        assertEquals(request.getEmail(), response.getEmail());
        Jockey jockey = jockeyRepository.findById(response.getProfileId()).orElseThrow();
        assertEquals(0, new BigDecimal("52.50").compareTo(jockey.getWeight()));
    }

    @Test
    void testAdminCreateRefereeWithProfileSuccess() throws Exception {
        CreateInternalAccountRequest request = baseRequest("new-referee-" + suffix + TEST_DOMAIN, Role.REFEREE)
                .licenseNumber("LIC-" + suffix)
                .status("active")
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        InternalAccountResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InternalAccountResponse.class);
        assertNotNull(response.getUserId());
        assertNotNull(response.getProfileId());
        assertEquals(Role.REFEREE, response.getRole());
        assertEquals(request.getEmail(), response.getEmail());
        assertTrue(refereeRepository.findById(response.getProfileId()).isPresent());
    }

    @Test
    void testDuplicateEmailReturns400() throws Exception {
        String email = "duplicate-" + suffix + TEST_DOMAIN;
        createUser(email, "Duplicate User", Role.STAFF);

        CreateInternalAccountRequest request = baseRequest(email, Role.STAFF)
                .staffCode("DUP-STF-" + suffix)
                .department("Operations")
                .build();

        mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnsupportedRolesReturn400() throws Exception {
        assertUnsupportedRole(Role.OWNER);
        assertUnsupportedRole(Role.SPECTATOR);
        assertUnsupportedRole(Role.ADMIN);
    }

    @Test
    void testCreateInternalAccountWithoutTokenReturns401() throws Exception {
        CreateInternalAccountRequest request = baseRequest("no-token-" + suffix + TEST_DOMAIN, Role.JOCKEY)
                .weight(new BigDecimal("51.00"))
                .build();

        mockMvc.perform(post("/api/admin/internal-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testNonAdminRolesReturn403() throws Exception {
        CreateInternalAccountRequest request = baseRequest("forbidden-" + suffix + TEST_DOMAIN, Role.JOCKEY)
                .weight(new BigDecimal("51.00"))
                .build();

        mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", jockeyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testProfileFailureRollsBackUser() throws Exception {
        String email = "rollback-" + suffix + TEST_DOMAIN;
        CreateInternalAccountRequest request = baseRequest(email, Role.STAFF)
                .staffCode("STF-" + suffix)
                .department("Operations")
                .build();

        mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        CreateInternalAccountRequest duplicateProfileRequest = baseRequest(email.replace("rollback", "rollback-fail"), Role.STAFF)
                .staffCode("STF-" + suffix)
                .department("Operations")
                .build();

        mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateProfileRequest)))
                .andExpect(status().isBadRequest());

        assertTrue(userRepository.findByEmail(duplicateProfileRequest.getEmail()).isEmpty());
    }

    private void assertUnsupportedRole(Role role) throws Exception {
        CreateInternalAccountRequest request = baseRequest("invalid-" + role.name().toLowerCase() + "-" + suffix + TEST_DOMAIN, role)
                .build();

        mockMvc.perform(post("/api/admin/internal-accounts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private CreateInternalAccountRequest.CreateInternalAccountRequestBuilder baseRequest(String email, Role role) {
        return CreateInternalAccountRequest.builder()
                .fullName("Internal Account")
                .email(email)
                .password("123456")
                .phone("0900000000")
                .role(role);
    }

    private User createUser(String email, String fullName, Role role) {
        return userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("0900000000")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private void cleanup() {
        userRepository.findAll().stream()
                .filter(user -> user.getEmail() != null && user.getEmail().endsWith(TEST_DOMAIN))
                .forEach(user -> {
                    staffRepository.findByUserUserId(user.getUserId()).ifPresent(staffRepository::delete);
                    jockeyRepository.findByUserUserId(user.getUserId()).ifPresent(jockeyRepository::delete);
                    refereeRepository.findByUserUserId(user.getUserId()).ifPresent(refereeRepository::delete);
                });
        userRepository.findAll().stream()
                .filter(user -> user.getEmail() != null && user.getEmail().endsWith(TEST_DOMAIN))
                .forEach(userRepository::delete);
    }
}
