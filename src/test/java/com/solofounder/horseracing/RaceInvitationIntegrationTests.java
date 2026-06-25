package com.solofounder.horseracing;

import tools.jackson.databind.ObjectMapper;
import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.auth.AuthResponse;
import com.solofounder.horseracing.dto.auth.RegisterRequest;
import com.solofounder.horseracing.dto.entry.CreateRaceEntryRequest;
import com.solofounder.horseracing.dto.entry.RaceEntryResponse;
import com.solofounder.horseracing.dto.invitation.CreateInvitationRequest;
import com.solofounder.horseracing.dto.invitation.InvitationResponse;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.*;
import com.solofounder.horseracing.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RaceInvitationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HorseRepository horseRepository;

    @Autowired
    private JockeyRepository jockeyRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private RacecourseRepository racecourseRepository;

    @Autowired
    private RaceMeetingRepository raceMeetingRepository;

    @Autowired
    private RaceConditionRepository raceConditionRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private RaceRegistrationRepository raceRegistrationRepository;

    @Autowired
    private RaceInvitationRepository raceInvitationRepository;

    @Autowired
    private RaceEntryRepository raceEntryRepository;

    @Autowired
    private RaceResultRepository raceResultRepository;

    @Autowired
    private JockeyRaceRegistrationRepository jockeyRaceRegistrationRepository;

    @Autowired
    private RefereeReportRepository refereeReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String ownerToken;
    private String jockeyToken;
    private String adminToken;
    private User ownerUser;
    private User jockeyUser;
    private Jockey jockeyProfile;
    private RaceRegistration approvedRegistration;
    private Horse ownerHorse;
    private Race testRace;

    @BeforeEach
    void setupData() throws Exception {
        // Clear all invitation records
        refereeReportRepository.deleteAll();
        raceResultRepository.deleteAll();
        raceEntryRepository.deleteAll();
        raceInvitationRepository.deleteAll();
        jockeyRaceRegistrationRepository.deleteAll();
        raceRegistrationRepository.deleteAll();
        raceRepository.deleteAll();
        
        // Clean test users
        userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith("@test-invitation.com"))
                .forEach(u -> {
                    List<Horse> horses = horseRepository.findByOwnerUserId(u.getUserId());
                    if (!horses.isEmpty()) {
                        horseRepository.deleteAll(horses);
                    }
                    jockeyRepository.findByUserUserId(u.getUserId()).ifPresent(jockeyRepository::delete);
                });
        userRepository.flush();

        userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith("@test-invitation.com"))
                .forEach(userRepository::delete);
        userRepository.flush();

        // Create Owner
        RegisterRequest registerOwner = RegisterRequest.builder()
                .fullName("Test Owner")
                .email("owner@test-invitation.com")
                .password("123456")
                .phone("0900000001")
                .role(Role.OWNER)
                .build();

        MvcResult ownerRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerOwner)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse ownerAuth = objectMapper.readValue(ownerRes.getResponse().getContentAsString(), AuthResponse.class);
        ownerToken = "Bearer " + ownerAuth.getToken();
        ownerUser = userRepository.findByEmail("owner@test-invitation.com").orElseThrow();

        User adminUser = userRepository.save(User.builder()
                .fullName("Test Admin")
                .email("admin@test-invitation.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("0900000003")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
        adminToken = "Bearer " + jwtService.generateToken(adminUser);

        // Create Jockey
        RegisterRequest registerJockey = RegisterRequest.builder()
                .fullName("Test Jockey")
                .email("jockey@test-invitation.com")
                .password("123456")
                .phone("0900000002")
                .role(Role.JOCKEY)
                .build();

        MvcResult jockeyRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerJockey)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse jockeyAuth = objectMapper.readValue(jockeyRes.getResponse().getContentAsString(), AuthResponse.class);
        jockeyToken = "Bearer " + jockeyAuth.getToken();
        jockeyUser = userRepository.findByEmail("jockey@test-invitation.com").orElseThrow();

        // Create Jockey Profile
        jockeyProfile = jockeyRepository.findByUserUserId(jockeyUser.getUserId())
                .orElseGet(() -> Jockey.builder().user(jockeyUser).build());
        jockeyProfile.setWeight(new BigDecimal("54.50"));
        jockeyProfile.setExperienceYears((short) 5);
        jockeyProfile.setStatus("available");
        jockeyProfile = jockeyRepository.save(jockeyProfile);

        // Setup Season, Racecourse, Meeting, Condition, Race
        Season season = seasonRepository.save(Season.builder()
                .seasonName("Test Season")
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(6))
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        Racecourse racecourse = racecourseRepository.save(Racecourse.builder()
                .racecourseName("Test Course")
                .location("Hanoi")
                .surfaceType("turf")
                .capacity(10000)
                .createdAt(LocalDateTime.now())
                .build());

        RaceMeeting meeting = raceMeetingRepository.save(RaceMeeting.builder()
                .season(season)
                .racecourse(racecourse)
                .meetingDate(java.time.LocalDate.now().plusDays(5))
                .status("scheduled")
                .createdAt(LocalDateTime.now())
                .build());

        RaceCondition condition = raceConditionRepository.save(RaceCondition.builder()
                .conditionName("Test Condition")
                .distance(1200)
                .trackType("turf")
                .minEntries((short) 3)
                .maxEntries((short) 10)
                .classRequirement("1-5")
                .createdAt(LocalDateTime.now())
                .build());

        testRace = raceRepository.save(Race.builder()
                .raceMeeting(meeting)
                .raceCondition(condition)
                .raceName("Test Race")
                .raceNo((short) 1)
                .scheduledTime(LocalDateTime.now().plusDays(5))
                .registrationOpenAt(LocalDateTime.now().minusDays(1))
                .registrationCloseAt(LocalDateTime.now().plusDays(2))
                .status(RaceStatus.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Create Horse for Owner
        ownerHorse = horseRepository.save(Horse.builder()
                .owner(ownerUser)
                .horseName("Owner Horse")
                .color("Black")
                .age((short) 4)
                .gender("M")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .healthNote("Fine")
                .status("active")
                .build());

        // Create APPROVED Registration
        approvedRegistration = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(testRace)
                .horse(ownerHorse)
                .submittedBy(ownerUser)
                .status(RaceRegistrationStatus.APPROVED)
                .submittedAt(LocalDateTime.now())
                .reviewedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());

        jockeyRaceRegistrationRepository.save(JockeyRaceRegistration.builder()
                .race(testRace)
                .jockey(jockeyProfile)
                .status(JockeyRaceRegistrationStatus.REGISTERED)
                .note("Available for invitation tests")
                .build());
    }

    @Test
    void testOwnerSendsInvitationSuccessfully() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .raceRegistrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockeyProfile.getJockeyId())
                .message("Ride my horse please")
                .build();

        MvcResult result = mockMvc.perform(post("/api/invitations")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        InvitationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InvitationResponse.class);
        assertNotNull(response.getInvitationId());
        assertEquals(approvedRegistration.getRegistrationId(), response.getRaceRegistrationId());
        assertEquals(jockeyProfile.getJockeyId(), response.getJockeyId());
        assertEquals("SENT", response.getStatus());
        assertNotNull(response.getSentAt());
        assertEquals("Ride my horse please", response.getMessage());
    }

    @Test
    void testOwnerCannotInviteForAnotherOwnersRegistration() throws Exception {
        // Register another owner
        RegisterRequest registerOwner2 = RegisterRequest.builder()
                .fullName("Test Owner 2")
                .email("owner2@test-invitation.com")
                .password("123456")
                .phone("0900000003")
                .role(Role.OWNER)
                .build();

        MvcResult ownerRes2 = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerOwner2)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse ownerAuth2 = objectMapper.readValue(ownerRes2.getResponse().getContentAsString(), AuthResponse.class);
        String owner2Token = "Bearer " + ownerAuth2.getToken();

        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .raceRegistrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockeyProfile.getJockeyId())
                .build();

        // Should return 403 Forbidden
        mockMvc.perform(post("/api/invitations")
                .header("Authorization", owner2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPendingOrRejectedRegistrationCannotCreateInvitation() throws Exception {
        // Create another horse for the owner to prevent UQ (race_id, horse_id) violation
        Horse secondHorse = horseRepository.save(Horse.builder()
                .owner(ownerUser)
                .horseName("Second Horse")
                .color("White")
                .age((short) 5)
                .gender("F")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .status("active")
                .build());

        // Create a pending registration
        RaceRegistration pendingReg = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(testRace)
                .horse(secondHorse)
                .submittedBy(ownerUser)
                .status(RaceRegistrationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());

        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .raceRegistrationId(pendingReg.getRegistrationId())
                .jockeyId(jockeyProfile.getJockeyId())
                .build();

        mockMvc.perform(post("/api/invitations")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDuplicateRegistrationJockeyInvitationIsRejected() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .raceRegistrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockeyProfile.getJockeyId())
                .build();

        // First invitation
        mockMvc.perform(post("/api/invitations")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Second invitation (duplicate) -> should return 409 Conflict
        mockMvc.perform(post("/api/invitations")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void testJockeyCanViewOnlyTheirOwnInvitations() throws Exception {
        // Create invitation for jockey
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Retrieve my invitations as jockey
        MvcResult result = mockMvc.perform(get("/api/invitations")
                .header("Authorization", jockeyToken))
                .andExpect(status().isOk())
                .andReturn();

        InvitationResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), InvitationResponse[].class);
        assertEquals(1, responses.length);
        assertEquals(invitation.getInvitationId(), responses[0].getInvitationId());
    }

    @Test
    void testJockeyAcceptsInvitationSuccessfully() throws Exception {
        // Create invitation
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Accept
        MvcResult result = mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                .header("Authorization", jockeyToken))
                .andExpect(status().isOk())
                .andReturn();

        InvitationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InvitationResponse.class);
        assertEquals("ACCEPTED", response.getStatus());
        assertNotNull(response.getRespondedAt());

        // Verify in database
        RaceInvitation dbInvitation = raceInvitationRepository.findById(invitation.getInvitationId()).orElseThrow();
        assertEquals(RaceInvitationStatus.ACCEPTED, dbInvitation.getInvitationStatus());
        assertFalse(raceEntryRepository.existsByRegistrationRegistrationId(approvedRegistration.getRegistrationId()));
    }

    @Test
    void testDifferentJockeyCannotAccept() throws Exception {
        // Create invitation for jockeyProfile
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Register jockey 2
        RegisterRequest registerJockey2 = RegisterRequest.builder()
                .fullName("Test Jockey 2")
                .email("jockey2@test-invitation.com")
                .password("123456")
                .phone("0900000004")
                .role(Role.JOCKEY)
                .build();

        MvcResult jockeyRes2 = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerJockey2)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse jockeyAuth2 = objectMapper.readValue(jockeyRes2.getResponse().getContentAsString(), AuthResponse.class);
        String jockey2Token = "Bearer " + jockeyAuth2.getToken();

        // Accept using jockey 2 token -> should return 403 Forbidden
        mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                .header("Authorization", jockey2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testJockeyDeclinesInvitationSuccessfully() throws Exception {
        // Create invitation
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Decline
        MvcResult result = mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/decline")
                .header("Authorization", jockeyToken))
                .andExpect(status().isOk())
                .andReturn();

        InvitationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InvitationResponse.class);
        assertEquals("DECLINED", response.getStatus());
        assertNotNull(response.getRespondedAt());

        // Verify in database
        RaceInvitation dbInvitation = raceInvitationRepository.findById(invitation.getInvitationId()).orElseThrow();
        assertEquals(RaceInvitationStatus.DECLINED, dbInvitation.getInvitationStatus());
    }

    @Test
    void testAcceptedCannotBecomeDeclined() throws Exception {
        // Create invitation
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .build());

        // Try to decline -> conflict
        mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/decline")
                .header("Authorization", jockeyToken))
                .andExpect(status().isConflict());
    }

    @Test
    void testRaceEntryConflictOnlyHappensWhenCreatingEntry() throws Exception {
        // Create invitation 1 (accepted)
        RaceInvitation invitation1 = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .build());

        CreateRaceEntryRequest createFirstEntryRequest = CreateRaceEntryRequest.builder()
                .invitationId(invitation1.getInvitationId())
                .build();
        mockMvc.perform(post("/api/entries")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createFirstEntryRequest)))
                .andExpect(status().isOk());

        // Register jockey 2
        RegisterRequest registerJockey2 = RegisterRequest.builder()
                .fullName("Test Jockey 2")
                .email("jockey2@test-invitation.com")
                .password("123456")
                .phone("0900000004")
                .role(Role.JOCKEY)
                .build();

        MvcResult jockeyRes2 = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerJockey2)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse jockeyAuth2 = objectMapper.readValue(jockeyRes2.getResponse().getContentAsString(), AuthResponse.class);
        User jockeyUser2 = userRepository.findByEmail("jockey2@test-invitation.com").orElseThrow();
        Jockey jockeyProfile2 = jockeyRepository.findByUserUserId(jockeyUser2.getUserId())
                .orElseGet(() -> Jockey.builder().user(jockeyUser2).build());
        jockeyProfile2.setWeight(new BigDecimal("53.50"));
        jockeyProfile2.setExperienceYears((short) 4);
        jockeyProfile2.setStatus("available");
        jockeyProfile2 = jockeyRepository.save(jockeyProfile2);
        Jockey jockeyProfile2 = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser2)
                .weight(new BigDecimal("53.50"))
                .experienceYears((short) 4)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());
        jockeyRaceRegistrationRepository.save(JockeyRaceRegistration.builder()
                .race(testRace)
                .jockey(jockeyProfile2)
                .status(JockeyRaceRegistrationStatus.REGISTERED)
                .note("Available for invitation conflict test")
                .build());

        // Create invitation 2 (sent)
        RaceInvitation invitation2 = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile2)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        mockMvc.perform(put("/api/invitations/" + invitation2.getInvitationId() + "/accept")
                .header("Authorization", "Bearer " + jockeyAuth2.getToken()))
                .andExpect(status().isOk());
        assertEquals(RaceInvitationStatus.ACCEPTED,
                raceInvitationRepository.findById(invitation2.getInvitationId()).orElseThrow().getInvitationStatus());

        CreateRaceEntryRequest duplicateEntryRequest = CreateRaceEntryRequest.builder()
                .invitationId(invitation2.getInvitationId())
                .build();
        mockMvc.perform(post("/api/entries")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEntryRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void testOwnerCannotInviteJockeyAfterRegistrationCloseAt() throws Exception {
        // Change testRace registrationCloseAt to past
        testRace.setRegistrationCloseAt(LocalDateTime.now().minusMinutes(1));
        raceRepository.save(testRace);

        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .raceRegistrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockeyProfile.getJockeyId())
                .message("Ride my horse please")
                .build();

        // Should return 400 Bad Request
        mockMvc.perform(post("/api/invitations")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAcceptedInvitationThenStaffCreatesRaceEntry() throws Exception {
        // Create invitation
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Accept invitation
        MvcResult result = mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                .header("Authorization", jockeyToken))
                .andExpect(status().isOk())
                .andReturn();

        InvitationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InvitationResponse.class);
        assertEquals("ACCEPTED", response.getStatus());
        assertNull(response.getEntryId());
        assertFalse(raceEntryRepository.existsByRegistrationRegistrationId(approvedRegistration.getRegistrationId()));

        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .invitationId(invitation.getInvitationId())
                .build();

        MvcResult createEntryResult = mockMvc.perform(post("/api/entries")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        RaceEntryResponse entryResponse = objectMapper.readValue(
                createEntryResult.getResponse().getContentAsString(),
                RaceEntryResponse.class);

        Optional<RaceEntry> entryOpt = raceEntryRepository.findById(entryResponse.getEntryId());
        assertTrue(entryOpt.isPresent());
        RaceEntry entry = entryOpt.get();
        assertEquals(testRace.getRaceId(), entry.getRace().getRaceId());
        assertEquals(ownerHorse.getHorseId(), entry.getHorse().getHorseId());
        assertEquals(jockeyProfile.getJockeyId(), entry.getJockey().getJockeyId());
        assertNull(entry.getGateNumber());
        assertEquals(0, new BigDecimal("50.00").compareTo(entry.getHandicapWeight()));
        assertEquals(RaceInvitationStatus.USED,
                raceInvitationRepository.findById(invitation.getInvitationId()).orElseThrow().getInvitationStatus());

        // Accepting same invitation again (or trying to) returns conflict because it's already responded
        mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                .header("Authorization", jockeyToken))
                .andExpect(status().isConflict());
    }

    @Test
    void testJockeyCannotAcceptInvitationAfterRegistrationCloseAt() throws Exception {
        // Create invitation
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Change registrationCloseAt to past
        testRace.setRegistrationCloseAt(LocalDateTime.now().minusMinutes(1));
        raceRepository.save(testRace);

        // Try to accept -> should return 400 Bad Request
        mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                .header("Authorization", jockeyToken))
                .andExpect(status().isBadRequest());

        // Verify invitation status is EXPIRED
        RaceInvitation dbInvitation = raceInvitationRepository.findById(invitation.getInvitationId()).orElseThrow();
        assertEquals(RaceInvitationStatus.EXPIRED, dbInvitation.getInvitationStatus());
    }

    @Test
    void testGetInvitationsAfterRegistrationCloseAtLazyExpires() throws Exception {
        // Create invitation
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Change registrationCloseAt to past
        testRace.setRegistrationCloseAt(LocalDateTime.now().minusMinutes(1));
        raceRepository.save(testRace);

        // Call GET invitations as jockey
        MvcResult result = mockMvc.perform(get("/api/invitations")
                .header("Authorization", jockeyToken))
                .andExpect(status().isOk())
                .andReturn();

        InvitationResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), InvitationResponse[].class);
        assertEquals(1, responses.length);
        assertEquals("EXPIRED", responses[0].getStatus());
        assertFalse(responses[0].getCanAccept());
        assertFalse(responses[0].getCanDecline());
        assertNull(responses[0].getEntryId());

        // Verify database value updated to EXPIRED
        RaceInvitation dbInvitation = raceInvitationRepository.findById(invitation.getInvitationId()).orElseThrow();
        assertEquals(RaceInvitationStatus.EXPIRED, dbInvitation.getInvitationStatus());
    }
}
