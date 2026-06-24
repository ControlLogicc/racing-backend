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
import com.solofounder.horseracing.dto.staff.CreateStaffRequest;
import com.solofounder.horseracing.dto.staff.StaffResponse;
import com.solofounder.horseracing.dto.staff.StaffRegistrationResponse;
import com.solofounder.horseracing.dto.staff.UpdateStaffRequest;
import com.solofounder.horseracing.dto.jockey.JockeyResponse;
import com.solofounder.horseracing.dto.jockey.UpdateJockeyWeightRequest;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.dto.invitation.CreateInvitationRequest;
import com.solofounder.horseracing.dto.invitation.InvitationResponse;
import com.solofounder.horseracing.dto.referee.CreateRefereeRequest;
import com.solofounder.horseracing.dto.referee.UpdateRefereeRequest;
import com.solofounder.horseracing.dto.referee.RefereeResponse;
import com.solofounder.horseracing.dto.registration.CreateRegistrationRequest;
import com.solofounder.horseracing.dto.registration.RegistrationResponse;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.RefereeStatus;
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
        private com.solofounder.horseracing.repository.JockeyRepository jockeyRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RefereeRepository refereeRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RaceRepository raceRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RaceInvitationRepository raceInvitationRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RaceEntryRepository raceEntryRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RaceRegistrationRepository raceRegistrationRepository;

        @Autowired
        private com.solofounder.horseracing.repository.RefereeReportRepository refereeReportRepository;

        private static final java.util.Set<Long> preExistingUserIds = new java.util.HashSet<>();

        @BeforeEach
        void cleanDatabase() {
                refereeReportRepository.deleteAll();
                raceEntryRepository.deleteAll();
                raceInvitationRepository.deleteAll();
                raceRegistrationRepository.deleteAll();
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
                                .forEach(this::deleteTestProfiles);
                staffRepository.flush();
                jockeyRepository.flush();
                refereeRepository.flush();
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
                                .forEach(this::deleteTestProfiles);
                staffRepository.flush();
                jockeyRepository.flush();
                refereeRepository.flush();
                userRepository.findAll().stream()
                                .filter(u -> !preExistingUserIds.contains(u.getUserId()))
                                .forEach(userRepository::delete);
                userRepository.flush();
        }

        private void deleteTestProfiles(User user) {
                staffRepository.findByUserUserId(user.getUserId()).ifPresent(staffRepository::delete);
                jockeyRepository.findByUserUserId(user.getUserId()).ifPresent(jockeyRepository::delete);
                refereeRepository.findByUserUserId(user.getUserId()).ifPresent(refereeRepository::delete);
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

        // --- JOCKEY PROFILE MANAGEMENT TESTS ---

        @Test
        void testGetJockeysList() throws Exception {
                String adminToken = getAdminToken();
                Jockey jockey = createJockey("jockey.list@example.com", "Jockey List", BigDecimal.valueOf(52.5));

                MvcResult result = mockMvc.perform(get("/api/jockeys")
                                .header("Authorization", adminToken))
                                .andExpect(status().isOk())
                                .andReturn();

                JockeyResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), JockeyResponse[].class);
                assertTrue(java.util.Arrays.stream(responses)
                                .anyMatch(response -> response.getJockeyId().equals(jockey.getJockeyId())));
        }

        @Test
        void testJockeyUpdatesOwnWeight() throws Exception {
                Jockey jockey = createJockey("jockey.weight@example.com", "Jockey Weight", BigDecimal.valueOf(52.5));
                String jockeyToken = getTokenFor(jockey.getUser());

                UpdateJockeyWeightRequest request = UpdateJockeyWeightRequest.builder()
                                .weight(BigDecimal.valueOf(53.0))
                                .build();

                MvcResult result = mockMvc.perform(put("/api/jockeys/" + jockey.getJockeyId() + "/weight")
                                .header("Authorization", jockeyToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                JockeyResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), JockeyResponse.class);
                assertEquals(jockey.getJockeyId(), response.getJockeyId());
                assertEquals(0, BigDecimal.valueOf(53.0).compareTo(response.getWeight()));
                assertEquals((short) 3, response.getExperienceYears());
                assertEquals("available", response.getStatus());
        }

        @Test
        void testUpdateJockeyWeightWithoutTokenFails() throws Exception {
                Jockey jockey = createJockey("jockey.notoken@example.com", "Jockey No Token", BigDecimal.valueOf(52.5));

                UpdateJockeyWeightRequest request = UpdateJockeyWeightRequest.builder()
                                .weight(BigDecimal.valueOf(53.0))
                                .build();

                mockMvc.perform(put("/api/jockeys/" + jockey.getJockeyId() + "/weight")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testOwnerCannotUpdateJockeyWeight() throws Exception {
                Jockey jockey = createJockey("jockey.ownerforbidden@example.com", "Jockey Owner Forbidden", BigDecimal.valueOf(52.5));
                String ownerToken = getHorseOwnerToken();

                UpdateJockeyWeightRequest request = UpdateJockeyWeightRequest.builder()
                                .weight(BigDecimal.valueOf(53.0))
                                .build();

                mockMvc.perform(put("/api/jockeys/" + jockey.getJockeyId() + "/weight")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testJockeyCannotUpdateAnotherJockeyWeight() throws Exception {
                Jockey jockeyA = createJockey("jockey.ownera@example.com", "Jockey Owner A", BigDecimal.valueOf(52.5));
                Jockey jockeyB = createJockey("jockey.ownerb@example.com", "Jockey Owner B", BigDecimal.valueOf(54.0));
                String jockeyAToken = getTokenFor(jockeyA.getUser());

                UpdateJockeyWeightRequest request = UpdateJockeyWeightRequest.builder()
                                .weight(BigDecimal.valueOf(55.0))
                                .build();

                mockMvc.perform(put("/api/jockeys/" + jockeyB.getJockeyId() + "/weight")
                                .header("Authorization", jockeyAToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testUpdateJockeyWeightNotFoundFails() throws Exception {
                Jockey jockey = createJockey("jockey.notfound@example.com", "Jockey Not Found", BigDecimal.valueOf(52.5));
                String jockeyToken = getTokenFor(jockey.getUser());

                UpdateJockeyWeightRequest request = UpdateJockeyWeightRequest.builder()
                                .weight(BigDecimal.valueOf(53.0))
                                .build();

                mockMvc.perform(put("/api/jockeys/999999999/weight")
                                .header("Authorization", jockeyToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testUpdateJockeyWeightInvalidFails() throws Exception {
                Jockey jockey = createJockey("jockey.invalidweight@example.com", "Jockey Invalid Weight", BigDecimal.valueOf(52.5));
                String jockeyToken = getTokenFor(jockey.getUser());

                UpdateJockeyWeightRequest request = UpdateJockeyWeightRequest.builder()
                                .weight(BigDecimal.valueOf(20.0))
                                .build();

                mockMvc.perform(put("/api/jockeys/" + jockey.getJockeyId() + "/weight")
                                .header("Authorization", jockeyToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testJockeyAvailableRacesSuccess() throws Exception {
                Jockey jockey = createJockey("jockey.available@example.com", "Jockey Available", BigDecimal.valueOf(52.5));
                Staff staff = createStaff("jockey.avail.staff@example.com", "Jockey Available Staff", "JASF01");
                Referee referee = createReferee("jockey.avail.ref@example.com", "Jockey Available Referee", "JARF01");
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusMinutes(10), now.plusMinutes(50), now.plusHours(2));

                MvcResult result = mockMvc.perform(get("/api/jockeys/available-races")
                                .header("Authorization", getTokenFor(jockey.getUser())))
                                .andExpect(status().isOk())
                                .andReturn();

                RaceResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), RaceResponse[].class);
                assertTrue(java.util.Arrays.stream(responses)
                                .anyMatch(response -> response.getRaceId().equals(race.getRaceId())));
        }

        @Test
        void testJockeyAvailableRacesWithoutTokenFails() throws Exception {
                mockMvc.perform(get("/api/jockeys/available-races"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testOwnerCannotGetJockeyAvailableRaces() throws Exception {
                String ownerToken = getHorseOwnerToken();

                mockMvc.perform(get("/api/jockeys/available-races")
                                .header("Authorization", ownerToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testJockeyAvailableRacesWithoutProfileFails() throws Exception {
                User jockeyUser = createJockeyUser("jockey.noprofile@example.com", "Jockey No Profile");

                mockMvc.perform(get("/api/jockeys/available-races")
                                .header("Authorization", getTokenFor(jockeyUser)))
                                .andExpect(status().isNotFound());
        }

        // --- ADMIN RACE SETUP & LIFECYCLE TESTS ---

        private User createJockeyUser(String email, String fullName) {
                return userRepository.save(User.builder()
                                .fullName(fullName)
                                .email(email)
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0901234567")
                                .role(Role.JOCKEY)
                                .status(UserStatus.ACTIVE)
                                .build());
        }

        private Jockey createJockey(String email, String fullName, BigDecimal weight) {
                User user = createJockeyUser(email, fullName);
                return jockeyRepository.save(Jockey.builder()
                                .user(user)
                                .weight(weight)
                                .experienceYears((short) 3)
                                .status("available")
                                .createdAt(java.time.LocalDateTime.now())
                                .build());
        }

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
                                .status(RefereeStatus.ACTIVE)
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
        void testSameStatusTransitionFails() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("samestat.staff@example.com", "SameStat Staff", "SSSF01");
                Referee referee = createReferee("samestat.ref@example.com", "SameStat Referee", "SSRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "DRAFT", now.minusMinutes(10), now.plusMinutes(50),
                                now.plusHours(2));

                // DRAFT -> DRAFT should fail with 400 Bad Request
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"DRAFT\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testLaterTransitionsLifecycleSuccess() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("laterlife.staff@example.com", "LaterLife Staff", "LLSF01");
                Referee referee = createReferee("laterlife.ref@example.com", "LaterLife Referee", "LLRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "CLOSED_FOR_ENTRY", now.minusHours(2), now.minusHours(1),
                                now.plusHours(1));

                // 1. CLOSED_FOR_ENTRY -> RUNNING
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"RUNNING\"}"))
                                .andExpect(status().isOk());

                // 2. RUNNING -> RESULT_PENDING
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"RESULT_PENDING\"}"))
                                .andExpect(status().isOk());

                // 3. RESULT_PENDING -> OFFICIAL
                mockMvc.perform(patch("/api/race-management/races/" + race.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"OFFICIAL\"}"))
                                .andExpect(status().isOk());
        }

        @Test
        void testCancellationTransitionsSuccess() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("cancel.staff@example.com", "Cancel Staff", "CNSF01");
                Referee referee = createReferee("cancel.ref@example.com", "Cancel Referee", "CNRF01");
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                // Test DRAFT -> CANCELLED
                Race race1 = setupRaceHierarchy(staff, referee, "DRAFT", now.minusMinutes(10), now.plusMinutes(50), now.plusHours(2));
                mockMvc.perform(patch("/api/race-management/races/" + race1.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CANCELLED\"}"))
                                .andExpect(status().isOk());

                // Test SCHEDULED -> CANCELLED
                Race race2 = setupRaceHierarchy(staff, referee, "SCHEDULED", now.minusMinutes(10), now.plusMinutes(50), now.plusHours(2));
                mockMvc.perform(patch("/api/race-management/races/" + race2.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CANCELLED\"}"))
                                .andExpect(status().isOk());

                // Test OPEN_FOR_ENTRY -> CANCELLED
                Race race3 = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusMinutes(10), now.plusMinutes(50), now.plusHours(2));
                mockMvc.perform(patch("/api/race-management/races/" + race3.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CANCELLED\"}"))
                                .andExpect(status().isOk());

                // Test CLOSED_FOR_ENTRY -> CANCELLED
                Race race4 = setupRaceHierarchy(staff, referee, "CLOSED_FOR_ENTRY", now.minusMinutes(10), now.plusMinutes(50), now.plusHours(2));
                mockMvc.perform(patch("/api/race-management/races/" + race4.getRaceId() + "/status")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CANCELLED\"}"))
                                .andExpect(status().isOk());
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

        @Test
        void testCreateRefereeProfileSuccess() throws Exception {
                String adminToken = getAdminToken();
                User user = userRepository.save(User.builder()
                                .fullName("Test Referee User")
                                .email("ref.testcreate@example.com")
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0987654321")
                                .role(Role.REFEREE)
                                .status(UserStatus.ACTIVE)
                                .build());

                CreateRefereeRequest request = CreateRefereeRequest.builder()
                                .userId(user.getUserId())
                                .licenseNo("LIC-CREATE-SUCCESS")
                                .build();

                MvcResult result = mockMvc.perform(post("/api/referees")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                RefereeResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RefereeResponse.class);
                assertNotNull(response.getRefereeId());
                assertEquals(user.getUserId(), response.getUserId());
                assertEquals("Test Referee User", response.getFullName());
                assertEquals("ref.testcreate@example.com", response.getEmail());
                assertEquals("0987654321", response.getPhone());
                assertEquals("LIC-CREATE-SUCCESS", response.getLicenseNo());
                assertEquals("ACTIVE", response.getStatus());
                assertNotNull(response.getCreatedAt());
        }

        @Test
        void testCreateRefereeProfileUserRoleConstraint() throws Exception {
                String adminToken = getAdminToken();
                // Create user with OWNER role
                User user = userRepository.save(User.builder()
                                .fullName("Test Owner User")
                                .email("owner.testcreate@example.com")
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0987654321")
                                .role(Role.OWNER)
                                .status(UserStatus.ACTIVE)
                                .build());

                CreateRefereeRequest request = CreateRefereeRequest.builder()
                                .userId(user.getUserId())
                                .licenseNo("LIC-CREATE-ROLE")
                                .build();

                mockMvc.perform(post("/api/referees")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateRefereeProfileDuplicateUserRejected() throws Exception {
                String adminToken = getAdminToken();
                Referee existing = createReferee("ref.dupuser@example.com", "Dup User Referee", "LIC-DUP-USER-1");

                CreateRefereeRequest request = CreateRefereeRequest.builder()
                                .userId(existing.getUser().getUserId())
                                .licenseNo("LIC-DUP-USER-2")
                                .build();

                mockMvc.perform(post("/api/referees")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        @Test
        void testCreateRefereeProfileDuplicateLicenseRejected() throws Exception {
                String adminToken = getAdminToken();
                createReferee("ref.duplic1@example.com", "Dup Lic 1", "LIC-DUP-LIC");

                User user2 = userRepository.save(User.builder()
                                .fullName("Dup Lic 2 User")
                                .email("ref.duplic2@example.com")
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0987654321")
                                .role(Role.REFEREE)
                                .status(UserStatus.ACTIVE)
                                .build());

                CreateRefereeRequest request = CreateRefereeRequest.builder()
                                .userId(user2.getUserId())
                                .licenseNo("LIC-DUP-LIC")
                                .build();

                mockMvc.perform(post("/api/referees")
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        @Test
        void testUpdateRefereeProfileSuccess() throws Exception {
                String adminToken = getAdminToken();
                Referee existing = createReferee("ref.updatesuccess@example.com", "Update Success Referee", "LIC-UPDATE-OLD");

                UpdateRefereeRequest request = UpdateRefereeRequest.builder()
                                .licenseNo("LIC-UPDATE-NEW")
                                .status("INACTIVE")
                                .build();

                MvcResult result = mockMvc.perform(put("/api/referees/" + existing.getRefereeId())
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                RefereeResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RefereeResponse.class);
                assertEquals(existing.getRefereeId(), response.getRefereeId());
                assertEquals("LIC-UPDATE-NEW", response.getLicenseNo());
                assertEquals("INACTIVE", response.getStatus());
        }

        @Test
        void testUpdateRefereeProfileDuplicateLicenseRejected() throws Exception {
                String adminToken = getAdminToken();
                Referee referee1 = createReferee("ref.upddublic1@example.com", "Dup Lic Update 1", "LIC-DUP-LIC-UPD1");
                Referee referee2 = createReferee("ref.upddublic2@example.com", "Dup Lic Update 2", "LIC-DUP-LIC-UPD2");

                UpdateRefereeRequest request = UpdateRefereeRequest.builder()
                                .licenseNo("LIC-DUP-LIC-UPD1")
                                .status("ACTIVE")
                                .build();

                mockMvc.perform(put("/api/referees/" + referee2.getRefereeId())
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        @Test
        void testGetRefereesListPermissions() throws Exception {
                createReferee("ref.permissions@example.com", "Perms Referee", "LIC-PERMISSIONS");

                // 1. ADMIN can view list
                String adminToken = getAdminToken();
                MvcResult adminResult = mockMvc.perform(get("/api/referees")
                                .header("Authorization", adminToken))
                                .andExpect(status().isOk())
                                .andReturn();
                String adminBody = adminResult.getResponse().getContentAsString();
                assertTrue(adminBody.contains("LIC-PERMISSIONS"));
                assertFalse(adminBody.contains("passwordHash"));

                // 2. STAFF can view list
                String staffToken = getStaffToken();
                MvcResult staffResult = mockMvc.perform(get("/api/referees")
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andReturn();
                String staffBody = staffResult.getResponse().getContentAsString();
                assertTrue(staffBody.contains("LIC-PERMISSIONS"));

                // 3. OWNER gets 403 Forbidden
                String ownerToken = getHorseOwnerToken();
                mockMvc.perform(get("/api/referees")
                                .header("Authorization", ownerToken))
                                .andExpect(status().isForbidden());

                // 4. No token gets 401 Unauthorized
                mockMvc.perform(get("/api/referees"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testUpdateRefereeDoesNotChangeUserId() throws Exception {
                String adminToken = getAdminToken();
                Referee existing = createReferee("ref.updateuserid@example.com", "No Change UserID Referee", "LIC-NO-CHANGE");
                Long originalUserId = existing.getUser().getUserId();

                UpdateRefereeRequest request = UpdateRefereeRequest.builder()
                                .licenseNo("LIC-NO-CHANGE-UPDATED")
                                .status("ACTIVE")
                                .build();

                MvcResult result = mockMvc.perform(put("/api/referees/" + existing.getRefereeId())
                                .header("Authorization", adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                RefereeResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RefereeResponse.class);
                assertEquals(existing.getRefereeId(), response.getRefereeId());
                assertEquals(originalUserId, response.getUserId());
                assertEquals("LIC-NO-CHANGE-UPDATED", response.getLicenseNo());
        }

        private String getStaffToken() throws Exception {
                User staffUser = userRepository.save(User.builder()
                                .fullName("Staff User")
                                .email("staff.test@example.com")
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0987654321")
                                .role(Role.STAFF)
                                .status(UserStatus.ACTIVE)
                                .build());
                return "Bearer " + jwtService.generateToken(staffUser);
        }

        @Test
        void testOwnerSubmitRegistrationSuccess() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.staff@example.com", "Reg Staff", "RGSF01");
                Referee referee = createReferee("reg.ref@example.com", "Reg Referee", "RGRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();

                MvcResult result = mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                RegistrationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RegistrationResponse.class);
                assertNotNull(response.getRegistrationId());
                assertEquals(race.getRaceId(), response.getRaceId());
                assertEquals(horse.getHorseId(), response.getHorseId());
                assertEquals(owner.getUserId(), response.getOwnerId());
                assertEquals("PENDING", response.getStatus());
        }

        @Test
        void testOwnerSubmitDuplicateRegistrationConflict() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.dup.staff@example.com", "Reg Staff", "RGSF02");
                Referee referee = createReferee("reg.dup.ref@example.com", "Reg Referee", "RGRF02");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.dup.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();

                // First registration
                mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                // Second registration (duplicate)
                mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        @Test
        void testOwnerSubmitAnotherOwnerHorseNotFound() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.other.staff@example.com", "Reg Staff", "RGSF03");
                Referee referee = createReferee("reg.other.ref@example.com", "Reg Referee", "RGRF03");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User ownerA = createOwnerUser("reg.ownerA@example.com", "Owner A");
                User ownerB = createOwnerUser("reg.ownerB@example.com", "Owner B");
                String ownerBToken = getTokenFor(ownerB);

                Horse horse = createHorseForOwner(ownerA, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();

                // Owner B tries to register Owner A's horse
                mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerBToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testOwnerSubmitRegistrationRaceNotOpen() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.notopen.staff@example.com", "Reg Staff", "RGSF04");
                Referee referee = createReferee("reg.notopen.ref@example.com", "Reg Referee", "RGRF04");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "SCHEDULED", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.notopen.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();

                mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testOwnerSubmitRegistrationInvalidTimeWindow() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.time.staff@example.com", "Reg Staff", "RGSF05");
                Referee referee = createReferee("reg.time.ref@example.com", "Reg Referee", "RGRF05");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(5), now.minusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.time.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();

                mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testStaffViewRegistrationsSuccess() throws Exception {
                Staff staff = createStaff("reg.view.staff@example.com", "Reg Staff", "RGSF06");
                Referee referee = createReferee("reg.view.ref@example.com", "Reg Referee", "RGRF06");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.view.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();
                mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                String staffToken = getTokenFor(staff.getUser());
                MvcResult result = mockMvc.perform(get("/api/registrations/" + race.getRaceId())
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andReturn();

                String body = result.getResponse().getContentAsString();
                assertTrue(body.contains("Thunder"));
                assertFalse(body.contains("passwordHash"));
        }

        @Test
        void testStaffViewRegistrationsUnassignedForbidden() throws Exception {
                Staff staffA = createStaff("reg.viewA.staff@example.com", "Reg Staff A", "RGSF07");
                Staff staffB = createStaff("reg.viewB.staff@example.com", "Reg Staff B", "RGSF08");
                Referee referee = createReferee("reg.view.refB@example.com", "Reg Referee", "RGRF08");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staffA, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                String staffBToken = getTokenFor(staffB.getUser());
                mockMvc.perform(get("/api/registrations/" + race.getRaceId())
                                .header("Authorization", staffBToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testStaffGetOwnRacesSuccessAndFiltered() throws Exception {
                Staff staffA = createStaff("staff.races.a@example.com", "Staff Races A", "SRCA01");
                Staff staffB = createStaff("staff.races.b@example.com", "Staff Races B", "SRCB01");
                Referee referee = createReferee("staff.races.ref@example.com", "Staff Races Ref", "SRRF01");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race raceA = setupRaceHierarchy(staffA, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));
                Race raceB = setupRaceHierarchy(staffB, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                MvcResult result = mockMvc.perform(get("/api/staff/races")
                                .header("Authorization", getTokenFor(staffA.getUser())))
                                .andExpect(status().isOk())
                                .andReturn();

                RaceResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), RaceResponse[].class);
                assertTrue(java.util.Arrays.stream(responses).anyMatch(response -> response.getRaceId().equals(raceA.getRaceId())));
                assertTrue(java.util.Arrays.stream(responses).noneMatch(response -> response.getRaceId().equals(raceB.getRaceId())));
                assertTrue(java.util.Arrays.stream(responses).allMatch(response -> response.getStaffId().equals(staffA.getStaffId())));
        }

        @Test
        void testStaffGetOwnRacesSecurity() throws Exception {
                User owner = createOwnerUser("staff.races.owner@example.com", "Staff Races Owner");
                User staffUser = userRepository.save(User.builder()
                                .fullName("Staff Missing Profile")
                                .email("staff.races.noprofile@example.com")
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0912345678")
                                .role(Role.STAFF)
                                .status(UserStatus.ACTIVE)
                                .build());

                mockMvc.perform(get("/api/staff/races"))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(get("/api/staff/races")
                                .header("Authorization", getTokenFor(owner)))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/staff/races")
                                .header("Authorization", getTokenFor(staffUser)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testStaffGetOwnRegistrationsWithActionFlags() throws Exception {
                Staff staffA = createStaff("staff.regs.a@example.com", "Staff Regs A", "SRGA01");
                Staff staffB = createStaff("staff.regs.b@example.com", "Staff Regs B", "SRGB01");
                Referee referee = createReferee("staff.regs.ref@example.com", "Staff Regs Ref", "SRGR01");
                User owner = createOwnerUser("staff.regs.owner@example.com", "Staff Regs Owner");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race raceA = setupRaceHierarchy(staffA, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));
                Race raceB = setupRaceHierarchy(staffB, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));
                Horse pendingHorse = createHorseForOwner(owner, "Staff Pending Horse", "active");
                Horse approvedHorse = createHorseForOwner(owner, "Staff Approved Horse", "active");
                Horse otherHorse = createHorseForOwner(owner, "Other Staff Horse", "active");

                RaceRegistration pending = raceRegistrationRepository.save(RaceRegistration.builder()
                                .race(raceA)
                                .horse(pendingHorse)
                                .submittedBy(owner)
                                .status(RaceRegistrationStatus.PENDING)
                                .submittedAt(now)
                                .createdAt(now)
                                .build());
                RaceRegistration approved = raceRegistrationRepository.save(RaceRegistration.builder()
                                .race(raceA)
                                .horse(approvedHorse)
                                .submittedBy(owner)
                                .approvedBy(staffA)
                                .status(RaceRegistrationStatus.APPROVED)
                                .submittedAt(now.minusMinutes(5))
                                .reviewedAt(now)
                                .createdAt(now.minusMinutes(5))
                                .build());
                RaceRegistration otherStaff = raceRegistrationRepository.save(RaceRegistration.builder()
                                .race(raceB)
                                .horse(otherHorse)
                                .submittedBy(owner)
                                .status(RaceRegistrationStatus.PENDING)
                                .submittedAt(now)
                                .createdAt(now)
                                .build());

                MvcResult result = mockMvc.perform(get("/api/staff/registrations")
                                .header("Authorization", getTokenFor(staffA.getUser())))
                                .andExpect(status().isOk())
                                .andReturn();

                StaffRegistrationResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), StaffRegistrationResponse[].class);
                StaffRegistrationResponse pendingResponse = java.util.Arrays.stream(responses)
                                .filter(response -> response.getRegistrationId().equals(pending.getRegistrationId()))
                                .findFirst()
                                .orElseThrow();
                StaffRegistrationResponse approvedResponse = java.util.Arrays.stream(responses)
                                .filter(response -> response.getRegistrationId().equals(approved.getRegistrationId()))
                                .findFirst()
                                .orElseThrow();

                assertTrue(pendingResponse.getCanApprove());
                assertTrue(pendingResponse.getCanReject());
                assertFalse(approvedResponse.getCanApprove());
                assertFalse(approvedResponse.getCanReject());
                assertTrue(java.util.Arrays.stream(responses)
                                .noneMatch(response -> response.getRegistrationId().equals(otherStaff.getRegistrationId())));
        }

        @Test
        void testStaffGetOwnRegistrationsSecurity() throws Exception {
                User owner = createOwnerUser("staff.regs.ownerrole@example.com", "Staff Regs Owner Role");
                Jockey jockey = createJockey("staff.regs.jockey@example.com", "Staff Regs Jockey", BigDecimal.valueOf(52.5));

                mockMvc.perform(get("/api/staff/registrations"))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(get("/api/staff/registrations")
                                .header("Authorization", getTokenFor(owner)))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/staff/registrations")
                                .header("Authorization", getTokenFor(jockey.getUser())))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testStaffApproveRegistrationSuccess() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.app.staff@example.com", "Reg Staff", "RGSF09");
                Referee referee = createReferee("reg.app.ref@example.com", "Reg Referee", "RGRF09");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.app.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();
                MvcResult submitResult = mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                RegistrationResponse regResponse = objectMapper.readValue(submitResult.getResponse().getContentAsString(), RegistrationResponse.class);

                String staffToken = getTokenFor(staff.getUser());
                MvcResult appResult = mockMvc.perform(put("/api/registrations/" + regResponse.getRegistrationId() + "/approve")
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andReturn();

                RegistrationResponse appResponse = objectMapper.readValue(appResult.getResponse().getContentAsString(), RegistrationResponse.class);
                assertEquals("APPROVED", appResponse.getStatus());
        }

        @Test
        void testStaffRejectRegistrationSuccess() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.rej.staff@example.com", "Reg Staff", "RGSF10");
                Referee referee = createReferee("reg.rej.ref@example.com", "Reg Referee", "RGRF10");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.rej.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();
                MvcResult submitResult = mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                RegistrationResponse regResponse = objectMapper.readValue(submitResult.getResponse().getContentAsString(), RegistrationResponse.class);

                String staffToken = getTokenFor(staff.getUser());
                MvcResult rejResult = mockMvc.perform(put("/api/registrations/" + regResponse.getRegistrationId() + "/reject")
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andReturn();

                RegistrationResponse rejResponse = objectMapper.readValue(rejResult.getResponse().getContentAsString(), RegistrationResponse.class);
                assertEquals("REJECTED", rejResponse.getStatus());
        }

        @Test
        void testApproveOrRejectInvalidTransitions() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.trans.staff@example.com", "Reg Staff", "RGSF11");
                Referee referee = createReferee("reg.trans.ref@example.com", "Reg Referee", "RGRF11");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.trans.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();
                MvcResult submitResult = mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                RegistrationResponse regResponse = objectMapper.readValue(submitResult.getResponse().getContentAsString(), RegistrationResponse.class);

                String staffToken = getTokenFor(staff.getUser());
                mockMvc.perform(put("/api/registrations/" + regResponse.getRegistrationId() + "/approve")
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk());

                mockMvc.perform(put("/api/registrations/" + regResponse.getRegistrationId() + "/approve")
                                .header("Authorization", staffToken))
                                .andExpect(status().isBadRequest());

                mockMvc.perform(put("/api/registrations/" + regResponse.getRegistrationId() + "/reject")
                                .header("Authorization", staffToken))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testOwnerCannotApproveOrReject() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.ownerperms.staff@example.com", "Reg Staff", "RGSF12");
                Referee referee = createReferee("reg.ownerperms.ref@example.com", "Reg Referee", "RGRF12");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.ownerperms.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();
                MvcResult submitResult = mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                RegistrationResponse regResponse = objectMapper.readValue(submitResult.getResponse().getContentAsString(), RegistrationResponse.class);

                mockMvc.perform(put("/api/registrations/" + regResponse.getRegistrationId() + "/approve")
                                .header("Authorization", ownerToken))
                                .andExpect(status().isForbidden());

                mockMvc.perform(put("/api/registrations/" + regResponse.getRegistrationId() + "/reject")
                                .header("Authorization", ownerToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testNoTokenReturnsUnauthorized() throws Exception {
                mockMvc.perform(post("/api/registrations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(get("/api/registrations/1"))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(put("/api/registrations/1/approve"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testApprovedRegistrationDoesNotCreateRaceEntry() throws Exception {
                String adminToken = getAdminToken();
                Staff staff = createStaff("reg.entrycheck.staff@example.com", "Reg Staff", "RGSF13");
                Referee referee = createReferee("reg.entrycheck.ref@example.com", "Reg Referee", "RGRF13");

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusHours(1), now.plusHours(1), now.plusHours(5));

                User owner = createOwnerUser("reg.entrycheck.owner@example.com", "Reg Owner");
                String ownerToken = getTokenFor(owner);
                Horse horse = createHorseForOwner(owner, "Thunder", "active");

                CreateRegistrationRequest request = CreateRegistrationRequest.builder()
                                .raceId(race.getRaceId())
                                .horseId(horse.getHorseId())
                                .build();
                MvcResult submitResult = mockMvc.perform(post("/api/registrations")
                                .header("Authorization", ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                RegistrationResponse regResponse = objectMapper.readValue(submitResult.getResponse().getContentAsString(), RegistrationResponse.class);

                String staffToken = getTokenFor(staff.getUser());
                mockMvc.perform(put("/api/registrations/" + regResponse.getRegistrationId() + "/approve")
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk());

                long entryCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM dbo.race_entry WHERE horse_id = ?",
                                Long.class,
                                horse.getHorseId());
                assertEquals(0L, entryCount);
        }

        @Test
        void testOwnerCreateInvitationSuccess() throws Exception {
                User owner = createOwnerUser("invite.owner@example.com", "Invite Owner");
                Jockey jockey = createJockey("invite.jockey@example.com", "Invite Jockey", BigDecimal.valueOf(52.5));
                RaceRegistration registration = createApprovedRegistration(owner, "Invite Horse");

                CreateInvitationRequest request = CreateInvitationRequest.builder()
                                .raceRegistrationId(registration.getRegistrationId())
                                .jockeyId(jockey.getJockeyId())
                                .message("Please ride my horse")
                                .build();

                MvcResult result = mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(owner))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                InvitationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InvitationResponse.class);
                assertNotNull(response.getInvitationId());
                assertEquals(registration.getRegistrationId(), response.getRaceRegistrationId());
                assertEquals(jockey.getJockeyId(), response.getJockeyId());
                assertEquals("SENT", response.getStatus());
        }

        @Test
        void testCreateInvitationWithoutTokenFails() throws Exception {
                CreateInvitationRequest request = CreateInvitationRequest.builder()
                                .raceRegistrationId(1L)
                                .jockeyId(1L)
                                .build();

                mockMvc.perform(post("/api/invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testNonOwnerCreateInvitationFails() throws Exception {
                User owner = createOwnerUser("invite.role.owner@example.com", "Invite Role Owner");
                RaceRegistration registration = createApprovedRegistration(owner, "Invite Role Horse");
                Jockey jockey = createJockey("invite.role.jockey@example.com", "Invite Role Jockey", BigDecimal.valueOf(52.5));
                Staff staff = createStaff("invite.role.staff@example.com", "Invite Role Staff", "IRSF01");
                User spectator = createSpectatorUser("invite.role.spectator@example.com", "Invite Role Spectator");

                CreateInvitationRequest request = CreateInvitationRequest.builder()
                                .raceRegistrationId(registration.getRegistrationId())
                                .jockeyId(jockey.getJockeyId())
                                .build();

                mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(jockey.getUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(staff.getUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(spectator))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testOwnerCannotInviteForOtherOwnerRegistration() throws Exception {
                User ownerA = createOwnerUser("invite.ownera@example.com", "Invite Owner A");
                User ownerB = createOwnerUser("invite.ownerb@example.com", "Invite Owner B");
                RaceRegistration registration = createApprovedRegistration(ownerA, "Owner A Horse");
                Jockey jockey = createJockey("invite.otherowner.jockey@example.com", "Other Owner Jockey", BigDecimal.valueOf(52.5));

                CreateInvitationRequest request = CreateInvitationRequest.builder()
                                .raceRegistrationId(registration.getRegistrationId())
                                .jockeyId(jockey.getJockeyId())
                                .build();

                mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(ownerB))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testCreateInvitationNotFoundCases() throws Exception {
                User owner = createOwnerUser("invite.notfound.owner@example.com", "Invite NotFound Owner");
                RaceRegistration registration = createApprovedRegistration(owner, "Not Found Horse");
                Jockey jockey = createJockey("invite.notfound.jockey@example.com", "Invite NotFound Jockey", BigDecimal.valueOf(52.5));

                CreateInvitationRequest missingRegistration = CreateInvitationRequest.builder()
                                .raceRegistrationId(999999999L)
                                .jockeyId(jockey.getJockeyId())
                                .build();
                mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(owner))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(missingRegistration)))
                                .andExpect(status().isNotFound());

                CreateInvitationRequest missingJockey = CreateInvitationRequest.builder()
                                .raceRegistrationId(registration.getRegistrationId())
                                .jockeyId(999999999L)
                                .build();
                mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(owner))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(missingJockey)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testDuplicateOpenInvitationRejected() throws Exception {
                User owner = createOwnerUser("invite.dup.owner@example.com", "Invite Dup Owner");
                RaceRegistration registration = createApprovedRegistration(owner, "Dup Horse");
                Jockey jockey = createJockey("invite.dup.jockey@example.com", "Invite Dup Jockey", BigDecimal.valueOf(52.5));

                CreateInvitationRequest request = CreateInvitationRequest.builder()
                                .raceRegistrationId(registration.getRegistrationId())
                                .jockeyId(jockey.getJockeyId())
                                .build();

                mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(owner))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/invitations")
                                .header("Authorization", getTokenFor(owner))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        @Test
        void testOwnerAndJockeyGetOwnInvitations() throws Exception {
                User ownerA = createOwnerUser("invite.list.ownera@example.com", "Invite List Owner A");
                User ownerB = createOwnerUser("invite.list.ownerb@example.com", "Invite List Owner B");
                Jockey jockey = createJockey("invite.list.jockey@example.com", "Invite List Jockey", BigDecimal.valueOf(52.5));
                RaceInvitation invitationA = createInvitation(ownerA, jockey, "List Horse A", RaceInvitationStatus.SENT);
                createInvitation(ownerB, jockey, "List Horse B", RaceInvitationStatus.SENT);

                MvcResult ownerResult = mockMvc.perform(get("/api/invitations")
                                .header("Authorization", getTokenFor(ownerA)))
                                .andExpect(status().isOk())
                                .andReturn();
                InvitationResponse[] ownerResponses = objectMapper.readValue(ownerResult.getResponse().getContentAsString(), InvitationResponse[].class);
                assertTrue(ownerResponses.length > 0);
                assertTrue(java.util.Arrays.stream(ownerResponses).allMatch(response -> response.getOwnerId().equals(ownerA.getUserId())));

                MvcResult jockeyResult = mockMvc.perform(get("/api/invitations")
                                .header("Authorization", getTokenFor(jockey.getUser())))
                                .andExpect(status().isOk())
                                .andReturn();
                InvitationResponse[] jockeyResponses = objectMapper.readValue(jockeyResult.getResponse().getContentAsString(), InvitationResponse[].class);
                assertTrue(java.util.Arrays.stream(jockeyResponses)
                                .anyMatch(response -> response.getInvitationId().equals(invitationA.getInvitationId())));
                InvitationResponse sentInvitation = java.util.Arrays.stream(jockeyResponses)
                                .filter(response -> response.getInvitationId().equals(invitationA.getInvitationId()))
                                .findFirst()
                                .orElseThrow();
                assertTrue(sentInvitation.getCanAccept());
                assertTrue(sentInvitation.getCanDecline());
        }

        @Test
        void testJockeyAcceptAndDeclineOwnInvitation() throws Exception {
                User owner = createOwnerUser("invite.respond.owner@example.com", "Invite Respond Owner");
                Jockey jockey = createJockey("invite.respond.jockey@example.com", "Invite Respond Jockey", BigDecimal.valueOf(52.5));
                RaceInvitation acceptInvitation = createInvitation(owner, jockey, "Accept Horse", RaceInvitationStatus.SENT);
                RaceInvitation declineInvitation = createInvitation(owner, jockey, "Decline Horse", RaceInvitationStatus.SENT);

                MvcResult acceptResult = mockMvc.perform(put("/api/invitations/" + acceptInvitation.getInvitationId() + "/accept")
                                .header("Authorization", getTokenFor(jockey.getUser())))
                                .andExpect(status().isOk())
                                .andReturn();
                InvitationResponse accepted = objectMapper.readValue(acceptResult.getResponse().getContentAsString(), InvitationResponse.class);
                assertEquals("ACCEPTED", accepted.getStatus());
                assertNotNull(accepted.getRespondedAt());
                assertFalse(accepted.getCanAccept());
                assertFalse(accepted.getCanDecline());

                MvcResult declineResult = mockMvc.perform(put("/api/invitations/" + declineInvitation.getInvitationId() + "/decline")
                                .header("Authorization", getTokenFor(jockey.getUser())))
                                .andExpect(status().isOk())
                                .andReturn();
                InvitationResponse declined = objectMapper.readValue(declineResult.getResponse().getContentAsString(), InvitationResponse.class);
                assertEquals("DECLINED", declined.getStatus());
                assertNotNull(declined.getRespondedAt());
                assertFalse(declined.getCanAccept());
                assertFalse(declined.getCanDecline());
        }

        @Test
        void testOtherJockeyCannotAcceptInvitation() throws Exception {
                User owner = createOwnerUser("invite.otherj.owner@example.com", "Invite Other J Owner");
                Jockey invitedJockey = createJockey("invite.otherj.invited@example.com", "Invited Jockey", BigDecimal.valueOf(52.5));
                Jockey otherJockey = createJockey("invite.otherj.other@example.com", "Other Jockey", BigDecimal.valueOf(53.5));
                RaceInvitation invitation = createInvitation(owner, invitedJockey, "Other J Horse", RaceInvitationStatus.SENT);

                mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                                .header("Authorization", getTokenFor(otherJockey.getUser())))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testAcceptInvitationNotFoundFails() throws Exception {
                Jockey jockey = createJockey("invite.accept.notfound@example.com", "Accept NotFound Jockey", BigDecimal.valueOf(52.5));

                mockMvc.perform(put("/api/invitations/999999999/accept")
                                .header("Authorization", getTokenFor(jockey.getUser())))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testAcceptOrDeclineAlreadyRespondedInvitationFails() throws Exception {
                User owner = createOwnerUser("invite.responded.owner@example.com", "Invite Responded Owner");
                Jockey jockey = createJockey("invite.responded.jockey@example.com", "Invite Responded Jockey", BigDecimal.valueOf(52.5));
                RaceInvitation acceptedInvitation = createInvitation(owner, jockey, "Accepted Horse", RaceInvitationStatus.ACCEPTED);
                RaceInvitation declinedInvitation = createInvitation(owner, jockey, "Declined Horse", RaceInvitationStatus.DECLINED);

                mockMvc.perform(put("/api/invitations/" + declinedInvitation.getInvitationId() + "/accept")
                                .header("Authorization", getTokenFor(jockey.getUser())))
                                .andExpect(status().isConflict());

                mockMvc.perform(put("/api/invitations/" + acceptedInvitation.getInvitationId() + "/decline")
                                .header("Authorization", getTokenFor(jockey.getUser())))
                                .andExpect(status().isConflict());
        }

        @Test
        void testOwnerCannotAcceptOrDeclineInvitation() throws Exception {
                User owner = createOwnerUser("invite.ownerrespond.owner@example.com", "Invite Owner Respond");
                Jockey jockey = createJockey("invite.ownerrespond.jockey@example.com", "Invite Owner Respond Jockey", BigDecimal.valueOf(52.5));
                RaceInvitation invitation = createInvitation(owner, jockey, "Owner Respond Horse", RaceInvitationStatus.SENT);

                mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                                .header("Authorization", getTokenFor(owner)))
                                .andExpect(status().isForbidden());

                mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/decline")
                                .header("Authorization", getTokenFor(owner)))
                                .andExpect(status().isForbidden());
        }

        private User createOwnerUser(String email, String fullName) {
                return userRepository.save(User.builder()
                                .fullName(fullName)
                                .email(email)
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0987654321")
                                .role(Role.OWNER)
                                .status(UserStatus.ACTIVE)
                                .build());
        }

        private User createSpectatorUser(String email, String fullName) {
                return userRepository.save(User.builder()
                                .fullName(fullName)
                                .email(email)
                                .passwordHash(passwordEncoder.encode("123456"))
                                .phone("0987654321")
                                .role(Role.SPECTATOR)
                                .status(UserStatus.ACTIVE)
                                .build());
        }

        private RaceRegistration createApprovedRegistration(User owner, String horseName) {
                Staff staff = createStaff(horseName.toLowerCase().replace(" ", ".") + ".staff@example.com", horseName + " Staff", "INV" + Math.abs(horseName.hashCode() % 10000));
                Referee referee = createReferee(horseName.toLowerCase().replace(" ", ".") + ".ref@example.com", horseName + " Referee", "INV-RF-" + Math.abs(horseName.hashCode() % 10000));
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                Race race = setupRaceHierarchy(staff, referee, "OPEN_FOR_ENTRY", now.minusMinutes(10), now.plusMinutes(50), now.plusHours(2));
                Horse horse = createHorseForOwner(owner, horseName, "active");

                return raceRegistrationRepository.save(RaceRegistration.builder()
                                .race(race)
                                .horse(horse)
                                .submittedBy(owner)
                                .approvedBy(staff)
                                .status(RaceRegistrationStatus.APPROVED)
                                .submittedAt(now.minusMinutes(5))
                                .reviewedAt(now)
                                .createdAt(now.minusMinutes(5))
                                .build());
        }

        private RaceInvitation createInvitation(User owner, Jockey jockey, String horseName, RaceInvitationStatus status) {
                RaceRegistration registration = createApprovedRegistration(owner, horseName);
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                return raceInvitationRepository.save(RaceInvitation.builder()
                                .raceRegistration(registration)
                                .jockey(jockey)
                                .invitationStatus(status)
                                .message("Test invitation")
                                .sentAt(now)
                                .respondedAt(status == RaceInvitationStatus.ACCEPTED || status == RaceInvitationStatus.DECLINED ? now : null)
                                .createdAt(now)
                                .build());
        }

        private Horse createHorseForOwner(User owner, String name, String status) {
                return horseRepository.save(Horse.builder()
                                .owner(owner)
                                .horseName(name)
                                .color("Brown")
                                .age((short) 4)
                                .gender("M")
                                .currentScore(BigDecimal.ZERO)
                                .horseClass((short) 5)
                                .status(status)
                                .build());
        }

        private String getTokenFor(User user) {
                return "Bearer " + jwtService.generateToken(user);
        }
}
