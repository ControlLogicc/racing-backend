package com.solofounder.horseracing;

import tools.jackson.databind.ObjectMapper;
import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.entry.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RaceEntryIntegrationTests {

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
    private RefereeRepository refereeRepository;

    @Autowired
    private JockeyRepository jockeyRepository;

    @Autowired
    private HorseRepository horseRepository;

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

    private String adminToken;
    private String staffToken;
    private String otherStaffToken;
    private String refereeToken;
    private String ownerToken;
    private String jockeyToken;
    private Staff staffProfile;
    private Referee refereeProfile;
    private Jockey jockeyProfile1;
    private Jockey jockeyProfile2;
    private Race testRace;
    private Horse horse1;
    private Horse horse2;
    private RaceRegistration approvedReg1;
    private RaceRegistration approvedReg2;
    private RaceInvitation acceptedInv1;
    private RaceInvitation acceptedInv2;

    @BeforeEach
    void setupData() {
        refereeReportRepository.deleteAll();
        raceResultRepository.deleteAll();
        raceEntryRepository.deleteAll();
        raceInvitationRepository.deleteAll();
        jockeyRaceRegistrationRepository.deleteAll();
        raceRegistrationRepository.deleteAll();
        raceRepository.deleteAll();

        String suffix = String.valueOf(System.nanoTime());

        // Users
        User adminUser = userRepository.save(User.builder()
                .fullName("Admin Entry")
                .email("admin-" + suffix + "@entry-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("098888")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(adminUser);

        User staffUser = userRepository.save(User.builder()
                .fullName("Staff Entry")
                .email("staff-" + suffix + "@entry-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("098889")
                .role(Role.STAFF)
                .status(UserStatus.ACTIVE)
                .build());
        staffToken = "Bearer " + jwtService.generateToken(staffUser);

        staffProfile = staffRepository.save(Staff.builder()
                .user(staffUser)
                .staffCode("STF-" + suffix)
                .department("Operations")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        User otherStaffUser = userRepository.save(User.builder()
                .fullName("Other Staff Entry")
                .email("other-staff-" + suffix + "@entry-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("098880")
                .role(Role.STAFF)
                .status(UserStatus.ACTIVE)
                .build());
        otherStaffToken = "Bearer " + jwtService.generateToken(otherStaffUser);

        staffRepository.save(Staff.builder()
                .user(otherStaffUser)
                .staffCode("OSTF-" + suffix)
                .department("Operations")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        User refereeUser = userRepository.save(User.builder()
                .fullName("Referee Entry")
                .email("referee-" + suffix + "@entry-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("098881")
                .role(Role.REFEREE)
                .status(UserStatus.ACTIVE)
                .build());
        refereeToken = "Bearer " + jwtService.generateToken(refereeUser);

        refereeProfile = refereeRepository.save(Referee.builder()
                .user(refereeUser)
                .licenseNo("REF-" + suffix)
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        User ownerUser = userRepository.save(User.builder()
                .fullName("Owner Entry")
                .email("owner-" + suffix + "@entry-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("098890")
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .build());
        ownerToken = "Bearer " + jwtService.generateToken(ownerUser);

        User jockeyUser1 = userRepository.save(User.builder()
                .fullName("Jockey Entry One")
                .email("jockey1-" + suffix + "@entry-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("098891")
                .role(Role.JOCKEY)
                .status(UserStatus.ACTIVE)
                .build());
        jockeyToken = "Bearer " + jwtService.generateToken(jockeyUser1);

        jockeyProfile1 = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser1)
                .weight(new BigDecimal("50.0"))
                .experienceYears((short) 5)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());

        User jockeyUser2 = userRepository.save(User.builder()
                .fullName("Jockey Entry Two")
                .email("jockey2-" + suffix + "@entry-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("098892")
                .role(Role.JOCKEY)
                .status(UserStatus.ACTIVE)
                .build());
        jockeyProfile2 = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser2)
                .weight(new BigDecimal("52.0"))
                .experienceYears((short) 4)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());

        // Horses
        horse1 = horseRepository.save(Horse.builder()
                .owner(ownerUser)
                .horseName("Horse One")
                .color("Bay")
                .age((short) 3)
                .gender("M")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .status("active")
                .totalWins(0)
                .build());

        horse2 = horseRepository.save(Horse.builder()
                .owner(ownerUser)
                .horseName("Horse Two")
                .color("Grey")
                .age((short) 4)
                .gender("F")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .status("active")
                .totalWins(0)
                .build());

        // Season, Course, Meeting, Condition, Race
        Season season = seasonRepository.save(Season.builder()
                .seasonName("Entry Season")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        Racecourse course = racecourseRepository.save(Racecourse.builder()
                .racecourseName("Entry Course")
                .location("HCMC")
                .surfaceType("turf")
                .capacity(8000)
                .createdAt(LocalDateTime.now())
                .build());

        RaceMeeting meeting = raceMeetingRepository.save(RaceMeeting.builder()
                .season(season)
                .racecourse(course)
                .meetingDate(LocalDate.now().plusDays(5))
                .status("scheduled")
                .createdAt(LocalDateTime.now())
                .build());

        RaceCondition condition = raceConditionRepository.save(RaceCondition.builder()
                .conditionName("Entry Condition")
                .distance(1200)
                .trackType("turf")
                .minEntries((short) 2)
                .maxEntries((short) 10)
                .classRequirement("5")
                .createdAt(LocalDateTime.now())
                .build());

        testRace = raceRepository.save(Race.builder()
                .raceMeeting(meeting)
                .raceCondition(condition)
                .staff(staffProfile)
                .referee(refereeProfile)
                .raceName("Entry Derby")
                .raceNo((short) 1)
                .scheduledTime(LocalDateTime.now().plusDays(5))
                .registrationOpenAt(LocalDateTime.now().minusDays(2))
                .registrationCloseAt(LocalDateTime.now().plusDays(2))
                .status(RaceStatus.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .build());

        // Approved Registrations
        approvedReg1 = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(testRace)
                .horse(horse1)
                .submittedBy(ownerUser)
                .status(RaceRegistrationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build());

        approvedReg2 = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(testRace)
                .horse(horse2)
                .submittedBy(ownerUser)
                .status(RaceRegistrationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build());

        // Accepted Invitations
        acceptedInv1 = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedReg1)
                .jockey(jockeyProfile1)
                .invitationStatus(RaceInvitationStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .build());

        acceptedInv2 = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedReg2)
                .jockey(jockeyProfile2)
                .invitationStatus(RaceInvitationStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .build());

        jockeyRaceRegistrationRepository.save(JockeyRaceRegistration.builder()
                .race(testRace)
                .jockey(jockeyProfile1)
                .status(JockeyRaceRegistrationStatus.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .build());

        jockeyRaceRegistrationRepository.save(JockeyRaceRegistration.builder()
                .race(testRace)
                .jockey(jockeyProfile2)
                .status(JockeyRaceRegistrationStatus.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .build());
    }

    @Test
    void testCreateEntrySuccess() throws Exception {
        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        RaceEntryResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RaceEntryResponse.class);
        assertNotNull(response.getEntryId());
        assertEquals("declared", response.getEntryStatus());
        assertNull(response.getGateNumber());
        assertNull(response.getHandicapWeight());
        assertEquals(staffProfile.getStaffId(), response.getConfirmedByStaffId());

        // Verify Invitation is now USED
        RaceInvitation dbInv = raceInvitationRepository.findById(acceptedInv1.getInvitationId()).orElseThrow();
        assertEquals(RaceInvitationStatus.USED, dbInv.getInvitationStatus());
    }

    @Test
    void testCreateEntryAdminSuccess() throws Exception {
        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg2.getRegistrationId())
                .invitationId(acceptedInv2.getInvitationId())
                .gateNumber((short) 2)
                .handicapWeight(new BigDecimal("52.0"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/entries")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        RaceEntryResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RaceEntryResponse.class);
        assertNotNull(response.getEntryId());
        assertNull(response.getConfirmedByStaffId());
    }

    @Test
    void testCreateSecondEntryInSameRaceDoesNotConflictWithNullGateNumber() throws Exception {
        CreateRaceEntryRequest firstRequest = CreateRaceEntryRequest.builder()
                .invitationId(acceptedInv1.getInvitationId())
                .build();

        MvcResult firstResult = mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andReturn();
        RaceEntryResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(),
                RaceEntryResponse.class);
        assertNull(firstResponse.getGateNumber());

        CreateRaceEntryRequest secondRequest = CreateRaceEntryRequest.builder()
                .invitationId(acceptedInv2.getInvitationId())
                .build();

        MvcResult secondResult = mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andReturn();
        RaceEntryResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(),
                RaceEntryResponse.class);
        assertNotNull(secondResponse.getEntryId());
    }

    @Test
    void testCreateEntryNotApprovedRegistrationFails() throws Exception {
        // Change registration to PENDING
        approvedReg1.setStatus(RaceRegistrationStatus.PENDING);
        raceRegistrationRepository.save(approvedReg1);

        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateEntryNotAcceptedInvitationFails() throws Exception {
        // Change invitation to SENT
        acceptedInv1.setInvitationStatus(RaceInvitationStatus.SENT);
        raceInvitationRepository.save(acceptedInv1);

        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateEntryDuplicateRegistrationFails() throws Exception {
        CreateRaceEntryRequest request1 = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        acceptedInv1.setInvitationStatus(RaceInvitationStatus.ACCEPTED);
        raceInvitationRepository.save(acceptedInv1);

        // Try to create again for same registration
        CreateRaceEntryRequest request2 = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 2)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void testGetAcceptedInvitationCandidates() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/entries/race/" + testRace.getRaceId() + "/candidates")
                .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andReturn();

        AcceptedInvitationCandidateResponse[] responses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AcceptedInvitationCandidateResponse[].class);
        assertEquals(2, responses.length);
        assertTrue(responses[0].getCanCreateEntry());
        assertNull(responses[0].getReason());
    }

    @Test
    void testCreateEntryDuplicateJockeyFails() throws Exception {
        CreateRaceEntryRequest request1 = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 4)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Assign same jockey to the second entry and try to create
        acceptedInv2.setJockey(jockeyProfile1); // jockey1 is now on both accepted invitations
        raceInvitationRepository.save(acceptedInv2);

        CreateRaceEntryRequest request2 = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg2.getRegistrationId())
                .invitationId(acceptedInv2.getInvitationId())
                .gateNumber((short) 5)
                .handicapWeight(new BigDecimal("52.0"))
                .build();

        mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void testGetEntryAndEntriesForRace() throws Exception {
        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        RaceEntryResponse created = objectMapper.readValue(result.getResponse().getContentAsString(), RaceEntryResponse.class);

        // Get single entry detail
        mockMvc.perform(get("/api/entries/" + created.getEntryId())
                .header("Authorization", ownerToken))
                .andExpect(status().isOk());

        // Get entries for race
        mockMvc.perform(get("/api/entries/race/" + testRace.getRaceId())
                .header("Authorization", jockeyToken))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateWeightSuccess() throws Exception {
        CreateRaceEntryRequest createReq = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();
        RaceEntryResponse created = objectMapper.readValue(result.getResponse().getContentAsString(), RaceEntryResponse.class);

        UpdateWeightRequest weightReq = UpdateWeightRequest.builder()
                .actualWeight(new BigDecimal("51.0"))
                .weightCheckStatus("passed")
                .build();

        MvcResult updatedResult = mockMvc.perform(put("/api/entries/" + created.getEntryId() + "/weight")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(weightReq)))
                .andExpect(status().isOk())
                .andReturn();

        RaceEntryResponse response = objectMapper.readValue(updatedResult.getResponse().getContentAsString(), RaceEntryResponse.class);
        assertEquals(new BigDecimal("51.0"), response.getActualWeight());
        assertEquals("passed", response.getWeightCheckStatus());
    }

    @Test
    void testBatchWeightCheckPassedAndFailedSuccess() throws Exception {
        RaceEntry entry1 = saveDeclaredEntry(approvedReg1, acceptedInv1, horse1, jockeyProfile1, (short) 1);
        RaceEntry entry2 = saveDeclaredEntry(approvedReg2, acceptedInv2, horse2, jockeyProfile2, (short) 2);

        BatchWeightCheckRequest request = BatchWeightCheckRequest.builder()
                .handicapWeight(new BigDecimal("55.00"))
                .checks(List.of(
                        WeightCheckItemRequest.builder()
                                .entryId(entry1.getEntryId())
                                .actualWeight(new BigDecimal("55.20"))
                                .passed(true)
                                .note("Passed")
                                .build(),
                        WeightCheckItemRequest.builder()
                                .entryId(entry2.getEntryId())
                                .actualWeight(new BigDecimal("58.50"))
                                .passed(false)
                                .note("Over weight")
                                .build()
                ))
                .build();

        MvcResult result = mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/weight-check")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        RaceEntryResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), RaceEntryResponse[].class);
        assertEquals(2, responses.length);

        RaceEntry refreshedEntry1 = raceEntryRepository.findById(entry1.getEntryId()).orElseThrow();
        RaceEntry refreshedEntry2 = raceEntryRepository.findById(entry2.getEntryId()).orElseThrow();
        assertEquals(0, new BigDecimal("55.00").compareTo(refreshedEntry1.getHandicapWeight()));
        assertEquals(0, new BigDecimal("55.20").compareTo(refreshedEntry1.getActualWeight()));
        assertEquals("passed", refreshedEntry1.getWeightCheckStatus());
        assertEquals("ready", refreshedEntry1.getEntryStatus());
        assertEquals(0, new BigDecimal("55.00").compareTo(refreshedEntry2.getHandicapWeight()));
        assertEquals(0, new BigDecimal("58.50").compareTo(refreshedEntry2.getActualWeight()));
        assertEquals("failed", refreshedEntry2.getWeightCheckStatus());
        assertEquals("scratched", refreshedEntry2.getEntryStatus());
    }

    @Test
    void testBatchWeightCheckInvalidWeightsReturn400() throws Exception {
        RaceEntry entry = saveDeclaredEntry(approvedReg1, acceptedInv1, horse1, jockeyProfile1, (short) 1);

        BatchWeightCheckRequest invalidHandicap = BatchWeightCheckRequest.builder()
                .handicapWeight(BigDecimal.ZERO)
                .checks(List.of(WeightCheckItemRequest.builder()
                        .entryId(entry.getEntryId())
                        .actualWeight(new BigDecimal("55.20"))
                        .passed(true)
                        .build()))
                .build();

        mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/weight-check")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidHandicap)))
                .andExpect(status().isBadRequest());

        BatchWeightCheckRequest invalidActual = BatchWeightCheckRequest.builder()
                .handicapWeight(new BigDecimal("55.00"))
                .checks(List.of(WeightCheckItemRequest.builder()
                        .entryId(entry.getEntryId())
                        .actualWeight(BigDecimal.ZERO)
                        .passed(true)
                        .build()))
                .build();

        mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/weight-check")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidActual)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBatchWeightCheckEntryNotBelongingToRaceReturns400() throws Exception {
        RaceEntry entry = saveDeclaredEntry(approvedReg1, acceptedInv1, horse1, jockeyProfile1, (short) 1);
        Race otherRace = raceRepository.save(Race.builder()
                .raceMeeting(testRace.getRaceMeeting())
                .raceCondition(testRace.getRaceCondition())
                .staff(staffProfile)
                .referee(refereeProfile)
                .raceName("Other Entry Race")
                .raceNo((short) 2)
                .scheduledTime(LocalDateTime.now().plusDays(5))
                .registrationOpenAt(LocalDateTime.now().minusDays(2))
                .registrationCloseAt(LocalDateTime.now().plusDays(2))
                .status(RaceStatus.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .build());

        BatchWeightCheckRequest request = validWeightCheckRequest(entry.getEntryId(), true);

        mockMvc.perform(put("/api/entries/race/" + otherRace.getRaceId() + "/weight-check")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBatchWeightCheckSecurityRules() throws Exception {
        RaceEntry entry = saveDeclaredEntry(approvedReg1, acceptedInv1, horse1, jockeyProfile1, (short) 1);
        BatchWeightCheckRequest request = validWeightCheckRequest(entry.getEntryId(), true);

        mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/weight-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/weight-check")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/weight-check")
                .header("Authorization", jockeyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/weight-check")
                .header("Authorization", otherStaffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBatchWeightCheckAssignedRefereeSuccess() throws Exception {
        RaceEntry entry = saveDeclaredEntry(approvedReg1, acceptedInv1, horse1, jockeyProfile1, (short) 1);
        BatchWeightCheckRequest request = validWeightCheckRequest(entry.getEntryId(), true);

        mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/weight-check")
                .header("Authorization", refereeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateStatusSuccess() throws Exception {
        CreateRaceEntryRequest createReq = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/entries")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();
        RaceEntryResponse created = objectMapper.readValue(result.getResponse().getContentAsString(), RaceEntryResponse.class);

        UpdateStatusRequest statusReq = UpdateStatusRequest.builder()
                .status("scratched")
                .build();

        MvcResult updatedResult = mockMvc.perform(put("/api/entries/" + created.getEntryId() + "/status")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andReturn();

        RaceEntryResponse response = objectMapper.readValue(updatedResult.getResponse().getContentAsString(), RaceEntryResponse.class);
        assertEquals("scratched", response.getEntryStatus());
    }

    @Test
    void testUnauthorizedRoleAccessFails() throws Exception {
        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .registrationId(approvedReg1.getRegistrationId())
                .invitationId(acceptedInv1.getInvitationId())
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("50.5"))
                .build();

        // Owner/Jockey cannot create entries
        mockMvc.perform(post("/api/entries")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/entries")
                .header("Authorization", jockeyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private RaceEntry saveDeclaredEntry(RaceRegistration registration, RaceInvitation invitation, Horse horse, Jockey jockey, Short gateNumber) {
        return raceEntryRepository.save(RaceEntry.builder()
                .race(testRace)
                .registration(registration)
                .invitation(invitation)
                .horse(horse)
                .jockey(jockey)
                .confirmedByStaff(staffProfile)
                .gateNumber(gateNumber)
                .handicapWeight(new BigDecimal("55.00"))
                .entryStatus("declared")
                .build());
    }

    private BatchWeightCheckRequest validWeightCheckRequest(Long entryId, boolean passed) {
        return BatchWeightCheckRequest.builder()
                .handicapWeight(new BigDecimal("55.00"))
                .checks(List.of(WeightCheckItemRequest.builder()
                        .entryId(entryId)
                        .actualWeight(new BigDecimal("55.20"))
                        .passed(passed)
                        .build()))
                .build();
    }
}
