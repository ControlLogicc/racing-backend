package com.solofounder.horseracing;

import tools.jackson.databind.ObjectMapper;
import com.solofounder.horseracing.dto.auth.AuthResponse;
import com.solofounder.horseracing.dto.auth.RegisterRequest;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String ownerToken;
    private String jockeyToken;
    private User ownerUser;
    private User jockeyUser;
    private Jockey jockeyProfile;
    private RaceRegistration approvedRegistration;
    private Horse ownerHorse;
    private Race testRace;

    @BeforeEach
    void setupData() throws Exception {
        // Clear all invitation records
        raceInvitationRepository.deleteAll();
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
        jockeyProfile = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser)
                .weight(new BigDecimal("54.50"))
                .experienceYears((short) 5)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());

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
                .status(RaceStatus.OPEN_FOR_ENTRY)
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
    }

    @Test
    void testOwnerSendsInvitationSuccessfully() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .registrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockeyProfile.getJockeyId())
                .message("Ride my horse please")
                .build();

        MvcResult result = mockMvc.perform(post("/api/invitations")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        InvitationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), InvitationResponse.class);
        assertNotNull(response.getInvitationId());
        assertEquals(approvedRegistration.getRegistrationId(), response.getRegistrationId());
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
                .registrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockeyProfile.getJockeyId())
                .build();

        // Should return 404 convention (hidden resource)
        mockMvc.perform(post("/api/invitations")
                .header("Authorization", owner2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
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
                .registrationId(pendingReg.getRegistrationId())
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
                .registrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockeyProfile.getJockeyId())
                .build();

        // First invitation
        mockMvc.perform(post("/api/invitations")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

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
                .registration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Retrieve my invitations as jockey
        MvcResult result = mockMvc.perform(get("/api/invitations/my")
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
                .registration(approvedRegistration)
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
    }

    @Test
    void testDifferentJockeyCannotAccept() throws Exception {
        // Create invitation for jockeyProfile
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .registration(approvedRegistration)
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

        // Accept using jockey 2 token -> should return 404 (hidden resource)
        mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                .header("Authorization", jockey2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void testJockeyDeclinesInvitationSuccessfully() throws Exception {
        // Create invitation
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .registration(approvedRegistration)
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
                .registration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .build());

        // Try to decline -> bad request
        mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/decline")
                .header("Authorization", jockeyToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testOnlyOneAcceptedInvitationPerRegistration() throws Exception {
        // Create invitation 1 (accepted)
        RaceInvitation invitation1 = raceInvitationRepository.save(RaceInvitation.builder()
                .registration(approvedRegistration)
                .jockey(jockeyProfile)
                .invitationStatus(RaceInvitationStatus.ACCEPTED)
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
        User jockeyUser2 = userRepository.findByEmail("jockey2@test-invitation.com").orElseThrow();
        Jockey jockeyProfile2 = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser2)
                .weight(new BigDecimal("53.50"))
                .experienceYears((short) 4)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());

        // Create invitation 2 (sent)
        RaceInvitation invitation2 = raceInvitationRepository.save(RaceInvitation.builder()
                .registration(approvedRegistration)
                .jockey(jockeyProfile2)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build());

        // Try to accept invitation 2 -> should return 409 Conflict
        mockMvc.perform(put("/api/invitations/" + invitation2.getInvitationId() + "/accept")
                .header("Authorization", "Bearer " + jockeyAuth2.getToken()))
                .andExpect(status().isConflict());
    }
}
