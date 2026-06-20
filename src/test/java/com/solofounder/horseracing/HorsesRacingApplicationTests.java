package com.solofounder.horseracing;

import tools.jackson.databind.ObjectMapper;
import com.solofounder.horseracing.dto.auth.AuthResponse;
import com.solofounder.horseracing.dto.auth.LoginRequest;
import com.solofounder.horseracing.dto.auth.RegisterRequest;
import com.solofounder.horseracing.dto.user.CreateInternalUserRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.dto.horse.CreateHorseRequest;
import com.solofounder.horseracing.dto.horse.UpdateHorseRequest;
import com.solofounder.horseracing.dto.horse.HorseResponse;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.UserRepository;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.service.HorseService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        private HorseRepository horseRepository;

        @Autowired
        private HorseService horseService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

        @Autowired
        private jakarta.persistence.EntityManager entityManager;

        @Autowired
        private com.solofounder.horseracing.repository.SeasonRepository seasonRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RacecourseRepository racecourseRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RaceMeetingRepository raceMeetingRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RaceConditionRepository raceConditionRepository;

        @Autowired
        private com.solofounder.horseracing.repository.StaffRepository staffRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RefereeRepository refereeRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RaceRepository raceRepository;

        private static final java.util.Set<Long> preExistingUserIds = new java.util.HashSet<>();

        @BeforeEach
        void cleanDatabase() {
                // Clean up test horses owned by test users to prevent foreign key errors
                userRepository.findAll().stream()
                                .filter(u -> u.getEmail().endsWith("@example.com"))
                                .forEach(u -> {
                                        List<Horse> horses = horseRepository.findByOwnerUserId(u.getUserId());
                                        if (!horses.isEmpty()) {
                                                horseRepository.deleteAll(horses);
                                        }
                                });
                horseRepository.flush();

                // Clean up test users
                userRepository.findAll().stream()
                                .filter(u -> u.getEmail().endsWith("@example.com"))
                                .forEach(userRepository::delete);
                userRepository.flush();

                if (preExistingUserIds.isEmpty()) {
                        userRepository.findAll().stream()
                                        .map(User::getUserId)
                                        .forEach(preExistingUserIds::add);
                }
                userRepository.findAll().stream()
                                .filter(u -> !preExistingUserIds.contains(u.getUserId()))
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
                return getHorseOwnerTokenWithEmail("owner@example.com");
        }

        private String getHorseOwnerTokenWithEmail(String email) throws Exception {
                RegisterRequest register = RegisterRequest.builder()
                                .fullName("Owner A")
                                .email(email)
                                .password("123456")
                                .phone("0900000000")
                                .role(Role.OWNER)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                                .andExpect(status().isOk())
                                .andReturn();

                AuthResponse response = objectMapper.readValue(
                                result.getResponse().getContentAsString(),
                                AuthResponse.class);
                return "Bearer " + response.getToken();
        }

        // 1. Register OWNER -> Expected: 200 OK and token returned
        @Test
        void testRegisterHorseOwner() throws Exception {
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Owner A")
                                .email("owner@example.com")
                                .password("123456")
                                .phone("0900000000")
                                .role(Role.OWNER)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andReturn();

                AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                AuthResponse.class);
                assertNotNull(response.getToken());
                assertEquals("owner@example.com", response.getEmail());
                assertEquals("0900000000", response.getPhone());
                assertEquals(Role.OWNER, response.getRole());
        }

        // 2. Register JOCKEY -> Expected: 200 OK and token returned
        @Test
        void testRegisterJockey() throws Exception {
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Jockey A")
                                .email("jockey@example.com")
                                .password("123456")
                                .phone("0901111111")
                                .role(Role.JOCKEY)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andReturn();

                AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                AuthResponse.class);
                assertNotNull(response.getToken());
                assertEquals("jockey@example.com", response.getEmail());
                assertEquals("0901111111", response.getPhone());
                assertEquals(Role.JOCKEY, response.getRole());
        }

        // 3. Register SPECTATOR -> Expected: 200 OK and token returned
        @Test
        void testRegisterSpectator() throws Exception {
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Spectator A")
                                .email("spectator@example.com")
                                .password("123456")
                                .phone("0902222222")
                                .role(Role.SPECTATOR)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andReturn();

                AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                AuthResponse.class);
                assertNotNull(response.getToken());
                assertEquals("spectator@example.com", response.getEmail());
                assertEquals("0902222222", response.getPhone());
                assertEquals(Role.SPECTATOR, response.getRole());
        }

        // 4. Register ADMIN using /api/auth/register -> Expected: 400 Bad Request
        // (restricted role)
        @Test
        void testRegisterAdminPubliclyFails() throws Exception {
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Admin A")
                                .email("admin_new@example.com")
                                .password("123456")
                                .phone("0903333333")
                                .role(Role.ADMIN)
                                .build();

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        // 5. Register REFEREE using /api/auth/register -> Expected: 400 Bad Request
        // (restricted role)
        @Test
        void testRegisterRefereePubliclyFails() throws Exception {
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Referee A")
                                .email("referee_new@example.com")
                                .password("123456")
                                .phone("0904444444")
                                .role(Role.REFEREE)
                                .build();

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        // 6. Login with correct email/password -> Expected: 200 OK and token returned
        @Test
        void testLoginWithCorrectCredentials() throws Exception {
                testRegisterHorseOwner();

                LoginRequest login = LoginRequest.builder()
                                .email("owner@example.com")
                                .password("123456")
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();

                AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                AuthResponse.class);
                assertNotNull(response.getToken());
                assertEquals("owner@example.com", response.getEmail());
                assertEquals("0900000000", response.getPhone());
                assertEquals(Role.OWNER, response.getRole());
        }

        // 7. Login with wrong password -> Expected: error
        @Test
        void testLoginWithWrongPassword() throws Exception {
                testRegisterHorseOwner();

                LoginRequest login = LoginRequest.builder()
                                .email("owner@example.com")
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
                                .email("referee@example.com")
                                .password("123456")
                                .phone("0922222222")
                                .role(Role.REFEREE)
                                .build();

                mockMvc.perform(post("/api/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isUnauthorized());
        }

        // 9. Call POST /api/admin/users with OWNER token -> Expected: 403 Forbidden
        @Test
        void testAdminCreateUserWithOwnerToken() throws Exception {
                String ownerToken = getHorseOwnerToken();

                CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                                .fullName("Referee A")
                                .email("referee@example.com")
                                .password("123456")
                                .phone("0922222222")
                                .role(Role.REFEREE)
                                .build();

                mockMvc.perform(post("/api/admin/users")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isForbidden());
        }

        // 10. Call POST /api/admin/users with ADMIN token -> Expected: 200 OK and
        // internal account created
        @Test
        void testAdminCreateUserWithAdminToken() throws Exception {
                String adminToken = getAdminToken();

                CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                                .fullName("Referee A")
                                .email("referee@example.com")
                                .password("123456")
                                .phone("0922222222")
                                .role(Role.REFEREE)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/admin/users")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andReturn();

                UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                UserResponse.class);
                assertNotNull(response.getUserId());
                assertEquals("referee@example.com", response.getEmail());
                assertEquals("0922222222", response.getPhone());
                assertEquals(Role.REFEREE, response.getRole());
                assertEquals(UserStatus.ACTIVE, response.getStatus());
        }

        // 11. Admin creates REFEREE account -> Expected: account created successfully,
        // password stored as BCrypt hash
        @Test
        void testAdminCreatesRefereeAndBCryptHash() throws Exception {
                testAdminCreateUserWithAdminToken();

                User savedUser = userRepository.findByEmail("referee@example.com").orElse(null);
                assertNotNull(savedUser);
                assertTrue(passwordEncoder.matches("123456", savedUser.getPasswordHash()));
                assertNotEquals("123456", savedUser.getPasswordHash());
        }

        // 12. Admin tries to create OWNER using /api/admin/users -> Expected: error
        @Test
        void testAdminCreatesHorseOwnerFails() throws Exception {
                String adminToken = getAdminToken();

                CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                                .fullName("Owner B")
                                .email("ownerb@example.com")
                                .password("123456")
                                .phone("0900000001")
                                .role(Role.OWNER)
                                .build();

                mockMvc.perform(post("/api/admin/users")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        // 13. Public register STAFF -> Expected: 400 Bad Request
        @Test
        void testRegisterStaffPubliclyFails() throws Exception {
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Staff A")
                                .email("staff-public@example.com")
                                .password("123456")
                                .phone("0911111111")
                                .role(Role.STAFF)
                                .build();

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        // 14. Admin creates STAFF -> Expected: 200 OK, STAFF user created, password
        // stored as BCrypt hash, status ACTIVE
        @Test
        void testAdminCreatesStaffSuccessfully() throws Exception {
                String adminToken = getAdminToken();

                CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                                .fullName("Staff A")
                                .email("staff@example.com")
                                .password("123456")
                                .phone("0911111111")
                                .role(Role.STAFF)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/admin/users")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andReturn();

                UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                UserResponse.class);
                assertNotNull(response.getUserId());
                assertEquals("staff@example.com", response.getEmail());
                assertEquals("0911111111", response.getPhone());
                assertEquals(Role.STAFF, response.getRole());
                assertEquals(UserStatus.ACTIVE, response.getStatus());

                // Verify stored in DB with BCrypt password hash
                User savedUser = userRepository.findByEmail("staff@example.com").orElse(null);
                assertNotNull(savedUser);
                assertTrue(passwordEncoder.matches("123456", savedUser.getPasswordHash()));
                assertNotEquals("123456", savedUser.getPasswordHash());
        }

        // 15. Non-admin tries to create STAFF -> Expected: 403 Forbidden
        @Test
        void testNonAdminCreatesStaffFails() throws Exception {
                String ownerToken = getHorseOwnerToken();

                CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                                .fullName("Staff A")
                                .email("staff@example.com")
                                .password("123456")
                                .phone("0911111111")
                                .role(Role.STAFF)
                                .build();

                mockMvc.perform(post("/api/admin/users")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isForbidden());
        }

        // 16. Verify database still stores lowercase values (admin, staff, owner,
        // referee, jockey, spectator)
        @Test
        void testDatabaseStoresLowercaseValues() throws Exception {
                testRegisterHorseOwner();
                testAdminCreateUserWithAdminToken();

                java.util.Map<String, Object> dbOwner = jdbcTemplate.queryForMap(
                                "SELECT role, status FROM [user] WHERE email = 'owner@example.com'");
                assertEquals("owner", dbOwner.get("role"));
                assertEquals("active", dbOwner.get("status"));

                java.util.Map<String, Object> dbReferee = jdbcTemplate.queryForMap(
                                "SELECT role, status FROM [user] WHERE email = 'referee@example.com'");
                assertEquals("referee", dbReferee.get("role"));
                assertEquals("active", dbReferee.get("status"));
        }

        // 17. Register with invalid email format -> Expected: 400 Bad Request and
        // validation message
        @Test
        void testRegisterWithInvalidEmailFails() throws Exception {
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Test User")
                                .email("invalid-email-format")
                                .password("123456")
                                .phone("0900000000")
                                .role(Role.OWNER)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                assertTrue(content.contains("Email is invalid"));
        }

        // 18. Register with password < 6 characters -> Expected: 400 Bad Request and
        // validation message
        @Test
        void testRegisterWithShortPasswordFails() throws Exception {
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Test User")
                                .email("test.valid@example.com")
                                .password("123")
                                .phone("0900000000")
                                .role(Role.OWNER)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                assertTrue(content.contains("Password must be at least 6 characters"));
        }

        // 19. Login with blank password -> Expected: 400 Bad Request and validation
        // message
        @Test
        void testLoginWithBlankPasswordFails() throws Exception {
                LoginRequest req = LoginRequest.builder()
                                .email("test.valid@example.com")
                                .password("")
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                assertTrue(content.contains("Password is required"));
        }

        // 20. Access /v3/api-docs -> Expected: 200 OK
        @Test
        void testGetApiDocs() throws Exception {
                mockMvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk());
        }

        // --- OWNER HORSE MANAGEMENT FLOW INTEGRATION TESTS ---

        // Test 2: OWNER creates horse
        @Test
        void testOwnerCreatesHorseSuccessfully() throws Exception {
                String ownerToken = getHorseOwnerToken();

                CreateHorseRequest request = CreateHorseRequest.builder()
                                .horseName("Owner Horse Test")
                                .color("Brown")
                                .age((short) 4)
                                .gender("M")
                                .healthNote("Healthy")
                                .build();

                MvcResult result = mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                HorseResponse.class);
                assertNotNull(response.getHorseId());
                assertEquals("Owner Horse Test", response.getHorseName());
                assertEquals("Brown", response.getColor());
                assertEquals((short) 4, response.getAge());
                assertEquals("M", response.getGender());
                assertEquals("ACTIVE", response.getStatus()); // Default: ACTIVE
                assertEquals("Healthy", response.getHealthNote());
                assertEquals(0, BigDecimal.ZERO.compareTo(response.getCurrentScore())); // Default: 0
                assertEquals((short) 5, response.getHorseClass()); // Default: 5
                assertNotNull(response.getOwnerId());
        }

        // Test 3: OWNER gets own horses
        @Test
        void testOwnerGetsOwnHorses() throws Exception {
                String ownerToken = getHorseOwnerToken();

                CreateHorseRequest request = CreateHorseRequest.builder()
                                .horseName("Owner Horse A")
                                .color("White")
                                .age((short) 3)
                                .gender("F")
                                .healthNote("Superb")
                                .build();

                mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Get own horses list
                MvcResult getResult = mockMvc.perform(get("/api/owner/horses")
                                .header("Authorization", ownerToken))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse[] horses = objectMapper.readValue(getResult.getResponse().getContentAsString(),
                                HorseResponse[].class);
                assertTrue(horses.length > 0);
                assertEquals("Owner Horse A", horses[0].getHorseName());
                assertEquals("White", horses[0].getColor());
                assertEquals("ACTIVE", horses[0].getStatus());
        }

        // Test 4: OWNER gets own horse detail
        @Test
        void testOwnerGetsOwnHorseDetail() throws Exception {
                String ownerToken = getHorseOwnerToken();

                CreateHorseRequest request = CreateHorseRequest.builder()
                                .horseName("Owner Horse B")
                                .color("Grey")
                                .age((short) 4)
                                .gender("M")
                                .healthNote("Fit")
                                .build();

                MvcResult createResult = mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                HorseResponse.class);

                MvcResult detailResult = mockMvc.perform(get("/api/owner/horses/" + created.getHorseId())
                                .header("Authorization", ownerToken))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse detail = objectMapper.readValue(detailResult.getResponse().getContentAsString(),
                                HorseResponse.class);
                assertEquals(created.getHorseId(), detail.getHorseId());
                assertEquals("Owner Horse B", detail.getHorseName());
                assertEquals("Grey", detail.getColor());
                assertEquals("ACTIVE", detail.getStatus());
        }

        // Test 5: OWNER updates own horse
        @Test
        void testOwnerUpdatesOwnHorse() throws Exception {
                String ownerToken = getHorseOwnerToken();

                CreateHorseRequest request = CreateHorseRequest.builder()
                                .horseName("Original Name")
                                .color("Black")
                                .age((short) 3)
                                .gender("F")
                                .healthNote("Original Note")
                                .build();

                MvcResult createResult = mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                HorseResponse.class);

                UpdateHorseRequest updateRequest = UpdateHorseRequest.builder()
                                .horseName("Updated Name")
                                .color("Red")
                                .age((short) 4)
                                .gender("F")
                                .healthNote("Recovered")
                                .status("INJURED") // Uppercase status value
                                .build();

                MvcResult updateResult = mockMvc.perform(put("/api/owner/horses/" + created.getHorseId())
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse updated = objectMapper.readValue(updateResult.getResponse().getContentAsString(),
                                HorseResponse.class);
                assertEquals("Updated Name", updated.getHorseName());
                assertEquals("Red", updated.getColor());
                assertEquals((short) 4, updated.getAge());
                assertEquals("INJURED", updated.getStatus()); // uppercase response
                assertEquals("Recovered", updated.getHealthNote());
        }

        // Test 6: Another OWNER cannot view the horse
        @Test
        void testAnotherOwnerCannotViewHorse() throws Exception {
                String ownerAToken = getHorseOwnerTokenWithEmail("ownera@example.com");
                String ownerBToken = getHorseOwnerTokenWithEmail("ownerb@example.com");

                CreateHorseRequest request = CreateHorseRequest.builder()
                                .horseName("Owner A Horse")
                                .color("Brown")
                                .age((short) 5)
                                .gender("M")
                                .healthNote("None")
                                .build();

                MvcResult createResult = mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerAToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                HorseResponse.class);

                // Owner B tries to get Owner A's horse
                mockMvc.perform(get("/api/owner/horses/" + created.getHorseId())
                                .header("Authorization", ownerBToken))
                                .andExpect(status().isNotFound());
        }

        // Test 7: Another OWNER cannot update the horse
        @Test
        void testAnotherOwnerCannotUpdateHorse() throws Exception {
                String ownerAToken = getHorseOwnerTokenWithEmail("ownera@example.com");
                String ownerBToken = getHorseOwnerTokenWithEmail("ownerb@example.com");

                CreateHorseRequest request = CreateHorseRequest.builder()
                                .horseName("Owner A Horse")
                                .color("Brown")
                                .age((short) 5)
                                .gender("M")
                                .healthNote("None")
                                .build();

                MvcResult createResult = mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerAToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                HorseResponse.class);

                // Owner B tries to update Owner A's horse
                UpdateHorseRequest updateRequest = UpdateHorseRequest.builder()
                                .horseName("Hacked Name")
                                .color("Brown")
                                .age((short) 2)
                                .gender("F")
                                .status("RETIRED")
                                .build();

                mockMvc.perform(put("/api/owner/horses/" + created.getHorseId())
                                .header("Authorization", ownerBToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isNotFound());

                // Verify that database record was not modified
                Horse dbHorse = horseRepository.findById(created.getHorseId()).orElseThrow();
                assertEquals("Owner A Horse", dbHorse.getHorseName());
                assertEquals((short) 5, dbHorse.getAge());
                assertEquals("active", dbHorse.getStatus()); // remains lowercase active in DB
        }

        // Test 8: No token
        @Test
        void testNoTokenGetOwnHorses() throws Exception {
                mockMvc.perform(get("/api/owner/horses"))
                                .andExpect(status().isUnauthorized());
        }

        // Test 9: Non-OWNER token
        @Test
        void testNonOwnerTokenGetOwnHorses() throws Exception {
                // Register jockey
                RegisterRequest req = RegisterRequest.builder()
                                .fullName("Jockey Test")
                                .email("test.jockey2@example.com")
                                .password("123456")
                                .phone("0901111112")
                                .role(Role.JOCKEY)
                                .build();

                MvcResult result = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andReturn();

                AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                AuthResponse.class);
                String jockeyToken = "Bearer " + response.getToken();

                mockMvc.perform(get("/api/owner/horses")
                                .header("Authorization", jockeyToken))
                                .andExpect(status().isForbidden());
        }

        // Test Validation boundaries
        @Test
        void testOwnerHorseValidationFails() throws Exception {
                String ownerToken = getHorseOwnerToken();

                // 1. Blank name
                CreateHorseRequest blankName = CreateHorseRequest.builder()
                                .horseName(" ")
                                .color("Brown")
                                .age((short) 4)
                                .gender("M")
                                .build();

                mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(blankName)))
                                .andExpect(status().isBadRequest());

                // 2. Blank color
                CreateHorseRequest blankColor = CreateHorseRequest.builder()
                                .horseName("Thunder")
                                .color(" ")
                                .age((short) 4)
                                .gender("M")
                                .build();

                mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(blankColor)))
                                .andExpect(status().isBadRequest());

                // 3. Negative age
                CreateHorseRequest negativeAge = CreateHorseRequest.builder()
                                .horseName("Negative Age")
                                .color("Brown")
                                .age((short) -1)
                                .gender("M")
                                .build();

                mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(negativeAge)))
                                .andExpect(status().isBadRequest());

                // 4. Age exceeds 30
                CreateHorseRequest ageExceeds30 = CreateHorseRequest.builder()
                                .horseName("Old Horse")
                                .color("Brown")
                                .age((short) 31)
                                .gender("M")
                                .build();

                mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(ageExceeds30)))
                                .andExpect(status().isBadRequest());

                // 5. Age = 0 (invalid, must be from 1 to 30)
                CreateHorseRequest ageIsZero = CreateHorseRequest.builder()
                                .horseName("Baby Horse")
                                .color("Brown")
                                .age((short) 0)
                                .gender("M")
                                .build();

                mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(ageIsZero)))
                                .andExpect(status().isBadRequest());

                // 6. Invalid gender
                CreateHorseRequest invalidGender = CreateHorseRequest.builder()
                                .horseName("Invalid Gender")
                                .color("Brown")
                                .age((short) 4)
                                .gender("X")
                                .build();

                mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidGender)))
                                .andExpect(status().isBadRequest());

                // 7. Invalid status (on Update)
                CreateHorseRequest validRequest = CreateHorseRequest.builder()
                                .horseName("Temp Horse")
                                .color("Brown")
                                .age((short) 4)
                                .gender("M")
                                .build();

                MvcResult createResult = mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                HorseResponse.class);

                UpdateHorseRequest invalidStatus = UpdateHorseRequest.builder()
                                .horseName("Invalid Status")
                                .color("Brown")
                                .age((short) 4)
                                .gender("M")
                                .status("invalid_status")
                                .build();

                mockMvc.perform(put("/api/owner/horses/" + created.getHorseId())
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidStatus)))
                                .andExpect(status().isBadRequest());
        }

        // Test 9.1: Creating a horse ignores user-defined custom horseClass and
        // currentScore
        @Test
        void testCreateHorseIgnoresCustomScoreAndClass() throws Exception {
                String ownerToken = getHorseOwnerToken();

                // Construct raw JSON body with custom score and class
                String payload = "{"
                                + "\"horseName\":\"Custom test\","
                                + "\"color\":\"Brown\","
                                + "\"age\":5,"
                                + "\"gender\":\"M\","
                                + "\"currentScore\":100,"
                                + "\"horseClass\":1"
                                + "}";

                MvcResult result = mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                HorseResponse.class);
                assertEquals(0, BigDecimal.ZERO.compareTo(response.getCurrentScore())); // ignored, default 0
                assertEquals((short) 5, response.getHorseClass()); // ignored, default 5
        }

        // Test 9.2: Owner updates own horse and does not modify horseClass or
        // currentScore
        @Test
        void testOwnerUpdatePreservesScoreAndClass() throws Exception {
                String ownerToken = getHorseOwnerToken();

                // 1. Create horse
                CreateHorseRequest request = CreateHorseRequest.builder()
                                .horseName("Update Preserve")
                                .color("White")
                                .age((short) 4)
                                .gender("M")
                                .build();

                MvcResult createResult = mockMvc.perform(post("/api/owner/horses")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                HorseResponse.class);

                // 2. Direct database update to score and class
                jdbcTemplate.update(
                                "UPDATE dbo.horse SET current_score = 55.00, horse_class = 3 WHERE horse_id = ?",
                                created.getHorseId());
                entityManager.flush();
                entityManager.clear();

                // 3. Perform owner update
                UpdateHorseRequest updateRequest = UpdateHorseRequest.builder()
                                .horseName("Update Preserve Updated")
                                .color("Gold")
                                .age((short) 5)
                                .gender("M")
                                .status("ACTIVE")
                                .build();

                MvcResult updateResult = mockMvc.perform(put("/api/owner/horses/" + created.getHorseId())
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                HorseResponse response = objectMapper.readValue(updateResult.getResponse().getContentAsString(),
                                HorseResponse.class);
                assertEquals("Update Preserve Updated", response.getHorseName());
                assertEquals("Gold", response.getColor());
                assertEquals(0, BigDecimal.valueOf(55).compareTo(response.getCurrentScore())); // score preserved!
                assertEquals((short) 3, response.getHorseClass()); // class preserved!
        }

        // Test 9.3: Score classification helper returns correct values based on
        // boundaries
        @Test
        void testCalculateHorseClassHelper() {
                assertEquals((short) 5, horseService.calculateHorseClass(BigDecimal.valueOf(0)));
                assertEquals((short) 5, horseService.calculateHorseClass(BigDecimal.valueOf(19)));
                assertEquals((short) 5, horseService.calculateHorseClass(BigDecimal.valueOf(19.99)));
                assertEquals((short) 4, horseService.calculateHorseClass(BigDecimal.valueOf(20)));
                assertEquals((short) 4, horseService.calculateHorseClass(BigDecimal.valueOf(39)));
                assertEquals((short) 4, horseService.calculateHorseClass(BigDecimal.valueOf(39.5)));
                assertEquals((short) 3, horseService.calculateHorseClass(BigDecimal.valueOf(40)));
                assertEquals((short) 3, horseService.calculateHorseClass(BigDecimal.valueOf(59)));
                assertEquals((short) 2, horseService.calculateHorseClass(BigDecimal.valueOf(60)));
                assertEquals((short) 2, horseService.calculateHorseClass(BigDecimal.valueOf(79)));
                assertEquals((short) 1, horseService.calculateHorseClass(BigDecimal.valueOf(80)));
                assertEquals((short) 1, horseService.calculateHorseClass(BigDecimal.valueOf(100)));

                // Null or negative values return Class 5
                assertEquals((short) 5, horseService.calculateHorseClass(null));
                assertEquals((short) 5, horseService.calculateHorseClass(BigDecimal.valueOf(-5)));
        }

        // --- ADMIN RACE SETUP & LIFECYCLE TESTS ---

        private Staff createStaff(String email, String fullName, String staffCode) {
                User user = userRepository.save(User.builder()
                                .fullName(fullName)
                                .email(email)
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0912345678")
                                .role(Role.STAFF)
                                .status(UserStatus.ACTIVE)
                                .build());

                return staffRepository.save(Staff.builder()
                                .user(user)
                                .staffCode(staffCode)
                                .department("Racing Operations")
                                .status("active")
                                .createdAt(java.time.LocalDateTime.now())
                                .build());
        }

        private Referee createReferee(String email, String fullName, String licenseNo) {
                User user = userRepository.save(User.builder()
                                .fullName(fullName)
                                .email(email)
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0987654321")
                                .role(Role.REFEREE)
                                .status(UserStatus.ACTIVE)
                                .build());

                return refereeRepository.save(Referee.builder()
                                .user(user)
                                .licenseNo(licenseNo)
                                .status("active")
                                .createdAt(java.time.LocalDateTime.now())
                                .build());
        }

        private Race setupRaceHierarchy(Staff staff, Referee referee, String statusStr, java.time.LocalDateTime openAt,
                        java.time.LocalDateTime closeAt, java.time.LocalDateTime scheduledAt) {
                Season season = seasonRepository.save(Season.builder()
                                .seasonName("Test Season")
                                .startDate(java.time.LocalDate.now().minusMonths(3))
                                .endDate(java.time.LocalDate.now().plusMonths(3))
                                .status("active")
                                .build());

                Racecourse racecourse = racecourseRepository.save(Racecourse.builder()
                                .racecourseName("Test Course")
                                .location("Test Location")
                                .surfaceType("Turf")
                                .capacity(10000)
                                .build());

                RaceMeeting meeting = raceMeetingRepository.save(RaceMeeting.builder()
                                .meetingName("Test Meeting")
                                .season(season)
                                .racecourse(racecourse)
                                .meetingDate(scheduledAt.toLocalDate())
                                .status("scheduled")
                                .build());

                RaceCondition condition = raceConditionRepository.save(RaceCondition.builder()
                                .conditionName("Test Condition")
                                .distance(1200)
                                .trackType("Turf")
                                .minEntries((short) 3)
                                .maxEntries((short) 10)
                                .classRequirement("Class 5")
                                .build());

                RaceStatus status = RaceStatus.DRAFT;
                if (statusStr != null) {
                        status = RaceStatus.valueOf(statusStr.toUpperCase());
                }

                return raceRepository.save(Race.builder()
                                .raceMeeting(meeting)
                                .raceCondition(condition)
                                .staff(staff)
                                .referee(referee)
                                .raceName("Test Race")
                                .raceNo((short) 1)
                                .scheduledTime(scheduledAt)
                                .registrationOpenAt(openAt)
                                .registrationCloseAt(closeAt)
                                .status(status)
                                .build());
        }

        @Test
        void testAdminCanUpdateRaceStatusLifecycle() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("lifecycle.staff@example.com", "Lifecycle Staff", "LCSF01");
                Referee referee = createReferee("lifecycle.ref@example.com", "Lifecycle Referee", "LCRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime openAt = now.minusMinutes(10);
                java.time.LocalDateTime closeAt = now.plusMinutes(50);
                java.time.LocalDateTime scheduledAt = now.plusHours(2);

                Race race = setupRaceHierarchy(staff, referee, "DRAFT", openAt, closeAt, scheduledAt);

                // 1. Transition: DRAFT -> SCHEDULED
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"SCHEDULED\"}"))
                                .andExpect(status().isOk());

                // 2. Transition: SCHEDULED -> OPEN_FOR_ENTRY
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"OPEN_FOR_ENTRY\"}"))
                                .andExpect(status().isOk());

                // 3. Transition: OPEN_FOR_ENTRY -> CLOSED_FOR_ENTRY
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CLOSED_FOR_ENTRY\"}"))
                                .andExpect(status().isOk());
        }

        @Test
        void testStaffOwnershipCheck() throws Exception {
                Staff staffA = createStaff("staffa@example.com", "Staff A", "STFA01");
                Staff staffB = createStaff("staffb@example.com", "Staff B", "STFB01");
                Referee referee = createReferee("ref@example.com", "Referee A", "RF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staffA, referee, "DRAFT", now.minusMinutes(10), now.plusMinutes(50),
                                now.plusHours(2));

                String tokenStaffA = "Bearer " + jwtService.generateToken(staffA.getUser());
                String tokenStaffB = "Bearer " + jwtService.generateToken(staffB.getUser());

                // Staff B (not owner) tries to change status -> 403 Forbidden
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", tokenStaffB)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"SCHEDULED\"}"))
                                .andExpect(status().isForbidden());

                // Staff A (owner) changes status -> 200 OK
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", tokenStaffA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"SCHEDULED\"}"))
                                .andExpect(status().isOk());
        }

        @Test
        void testInvalidTransitionsFail() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("trans.staff@example.com", "Trans Staff", "TSF01");
                Referee referee = createReferee("trans.ref@example.com", "Trans Referee", "TRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "DRAFT", now.minusMinutes(10), now.plusMinutes(50),
                                now.plusHours(2));

                // DRAFT -> OPEN_FOR_ENTRY directly should fail with 400 Bad Request
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"OPEN_FOR_ENTRY\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testOpenForEntryDateCheck() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("datecheck.staff@example.com", "DateCheck Staff", "DCSF01");
                Referee referee = createReferee("datecheck.ref@example.com", "DateCheck Referee", "DCRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                // Registration is in the past
                java.time.LocalDateTime openAt = now.minusHours(3);
                java.time.LocalDateTime closeAt = now.minusHours(2);
                java.time.LocalDateTime scheduledAt = now.plusHours(2);

                Race race = setupRaceHierarchy(staff, referee, "SCHEDULED", openAt, closeAt, scheduledAt);

                // SCHEDULED -> OPEN_FOR_ENTRY should fail since current time is after
                // registrationCloseAt
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"OPEN_FOR_ENTRY\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testOpenRacesVisibility() throws Exception {
                String ownerToken = getHorseOwnerToken();
                Staff staff = createStaff("vis.staff@example.com", "Vis Staff", "VSF01");
                Referee referee = createReferee("vis.ref@example.com", "Vis Referee", "VRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                // Race 1: OPEN_FOR_ENTRY, currently open
                Race race1 = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusMinutes(10),
                                now.plusMinutes(50), now.plusHours(2));

                // Race 2: SCHEDULED
                Race race2 = setupRaceHierarchy(staff, referee, "SCHEDULED", now.minusMinutes(10), now.plusMinutes(50),
                                now.plusHours(2));

                MvcResult result = mockMvc.perform(get("/api/races/open")
                                .header("Authorization", ownerToken))
                                .andExpect(status().isOk())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                assertTrue(responseBody.contains(race1.getRaceName()));
                assertFalse(responseBody.contains("\"raceId\":" + race2.getRaceId() + ","));
        }

        @Test
        void testDatabaseFormatStatusParsing() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("dbstatus.staff@example.com", "DBStatus Staff", "DBSF01");
                Referee referee = createReferee("dbstatus.ref@example.com", "DBStatus Referee", "DBRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime openAt = now.minusMinutes(10);
                java.time.LocalDateTime closeAt = now.plusMinutes(50);
                java.time.LocalDateTime scheduledAt = now.plusHours(2);

                Race race = setupRaceHierarchy(staff, referee, "SCHEDULED", openAt, closeAt, scheduledAt);

                // Transition: SCHEDULED -> registration_open (database format equivalent to OPEN_FOR_ENTRY)
                MvcResult mvcResult = mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"registration_open\"}"))
                                .andReturn();
                if (mvcResult.getResponse().getStatus() != 200) {
                        System.out.println("FAIL BODY: " + mvcResult.getResponse().getContentAsString());
                }
                assertEquals(200, mvcResult.getResponse().getStatus());

                // Transition: OPEN_FOR_ENTRY -> registration_closed (database format equivalent to CLOSED_FOR_ENTRY)
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"registration_closed\"}"))
                                .andExpect(status().isOk());
        }
}
