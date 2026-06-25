package com.solofounder.horseracing;

import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.entry.CreateRaceEntryRequest;
import com.solofounder.horseracing.dto.invitation.CreateInvitationRequest;
import com.solofounder.horseracing.dto.jockey.CreateJockeyRaceRegistrationRequest;
import com.solofounder.horseracing.dto.jockey.EligibleJockeyForInvitationResponse;
import com.solofounder.horseracing.dto.jockey.JockeyRaceRegistrationResponse;
import com.solofounder.horseracing.dto.invitation.InvitationResponse;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.JockeyRaceRegistration;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceCondition;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.RaceInvitation;
import com.solofounder.horseracing.model.RaceMeeting;
import com.solofounder.horseracing.model.RaceRegistration;
import com.solofounder.horseracing.model.Racecourse;
import com.solofounder.horseracing.model.Season;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.JockeyRaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.JockeyRaceRegistrationRepository;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.RaceConditionRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceInvitationRepository;
import com.solofounder.horseracing.repository.RaceMeetingRepository;
import com.solofounder.horseracing.repository.RaceRegistrationRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RacecourseRepository;
import com.solofounder.horseracing.repository.SeasonRepository;
import com.solofounder.horseracing.repository.UserRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JockeyRaceRegistrationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private JockeyRepository jockeyRepository;
    @Autowired private JockeyRaceRegistrationRepository jockeyRaceRegistrationRepository;
    @Autowired private SeasonRepository seasonRepository;
    @Autowired private RacecourseRepository racecourseRepository;
    @Autowired private RaceMeetingRepository raceMeetingRepository;
    @Autowired private RaceConditionRepository raceConditionRepository;
    @Autowired private RaceRepository raceRepository;
    @Autowired private HorseRepository horseRepository;
    @Autowired private RaceRegistrationRepository raceRegistrationRepository;
    @Autowired private RaceInvitationRepository raceInvitationRepository;
    @Autowired private RaceEntryRepository raceEntryRepository;

    private User owner;
    private User otherOwner;
    private User admin;
    private User staffUser;
    private Jockey jockey;
    private Jockey otherJockey;
    private Race openRace;
    private Race otherRace;
    private Race closedRace;
    private RaceRegistration approvedRegistration;
    private RaceRegistration otherOwnerRegistration;
    private RaceRegistration pendingRegistration;

    @BeforeEach
    void setup() {
        String suffix = String.valueOf(System.nanoTime());
        owner = createUser("owner-" + suffix + "@jrr.test", "Owner", Role.OWNER);
        otherOwner = createUser("owner-other-" + suffix + "@jrr.test", "Other Owner", Role.OWNER);
        admin = createUser("admin-" + suffix + "@jrr.test", "Admin", Role.ADMIN);
        staffUser = createUser("staff-" + suffix + "@jrr.test", "Staff", Role.STAFF);
        User jockeyUser = createUser("jockey-" + suffix + "@jrr.test", "Jockey", Role.JOCKEY);
        User otherJockeyUser = createUser("jockey-other-" + suffix + "@jrr.test", "Other Jockey", Role.JOCKEY);

        jockey = createJockey(jockeyUser, "available", "52.50");
        otherJockey = createJockey(otherJockeyUser, "available", "53.00");

        Season season = seasonRepository.save(Season.builder()
                .seasonName("JRR Season " + suffix)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .status("active")
                .build());
        Racecourse racecourse = racecourseRepository.save(Racecourse.builder()
                .racecourseName("JRR Course " + suffix)
                .location("Test")
                .surfaceType("turf")
                .capacity(1000)
                .build());
        RaceMeeting meeting = raceMeetingRepository.save(RaceMeeting.builder()
                .season(season)
                .racecourse(racecourse)
                .meetingDate(LocalDate.now().plusDays(3))
                .status("scheduled")
                .build());
        RaceCondition condition = raceConditionRepository.save(RaceCondition.builder()
                .conditionName("JRR Condition " + suffix)
                .distance(1200)
                .trackType("turf")
                .minEntries((short) 1)
                .maxEntries((short) 10)
                .classRequirement("1-5")
                .build());

        LocalDateTime now = LocalDateTime.now();
        openRace = createRace(meeting, condition, "Open Race " + suffix, (short) 1,
                RaceStatus.OPEN_FOR_ENTRY, now.minusHours(1), now.plusHours(4));
        otherRace = createRace(meeting, condition, "Other Race " + suffix, (short) 2,
                RaceStatus.OPEN_FOR_ENTRY, now.minusHours(1), now.plusHours(4));
        closedRace = createRace(meeting, condition, "Closed Race " + suffix, (short) 3,
                RaceStatus.CLOSED_FOR_ENTRY, now.minusHours(4), now.minusHours(1));

        approvedRegistration = createOwnerRegistration(owner, openRace, "Owner Horse " + suffix,
                RaceRegistrationStatus.APPROVED);
        otherOwnerRegistration = createOwnerRegistration(otherOwner, openRace, "Other Horse " + suffix,
                RaceRegistrationStatus.APPROVED);
        pendingRegistration = createOwnerRegistration(owner, otherRace, "Pending Horse " + suffix,
                RaceRegistrationStatus.PENDING);
    }

    @Test
    void jockeyRegistersForOpenRaceSuccessfully() throws Exception {
        MvcResult result = registerForRace(jockey, openRace, "I am available")
                .andExpect(status().isOk())
                .andReturn();

        JockeyRaceRegistrationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                JockeyRaceRegistrationResponse.class);
        assertNotNull(response.getJockeyRaceRegistrationId());
        assertEquals(openRace.getRaceId(), response.getRaceId());
        assertEquals(jockey.getJockeyId(), response.getJockeyId());
        assertEquals("REGISTERED", response.getRegistrationStatus());
    }

    @Test
    void jockeyRegistrationSecurityAndValidation() throws Exception {
        CreateJockeyRaceRegistrationRequest request = CreateJockeyRaceRegistrationRequest.builder()
                .raceId(openRace.getRaceId())
                .build();

        mockMvc.perform(post("/api/jockey/race-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/jockey/race-registrations")
                        .header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        request.setRaceId(999999999L);
        mockMvc.perform(post("/api/jockey/race-registrations")
                        .header("Authorization", token(jockey.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        request.setRaceId(closedRace.getRaceId());
        mockMvc.perform(post("/api/jockey/race-registrations")
                        .header("Authorization", token(jockey.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateRaceRegistrationReturns409() throws Exception {
        registerForRace(jockey, openRace, null).andExpect(status().isOk());
        registerForRace(jockey, openRace, null).andExpect(status().isConflict());
    }

    @Test
    void unavailableJockeyCannotRegister() throws Exception {
        jockey.setStatus("unavailable");
        jockeyRepository.save(jockey);
        registerForRace(jockey, openRace, null).andExpect(status().isBadRequest());
    }

    @Test
    void jockeySeesOnlyOwnRaceRegistrations() throws Exception {
        saveJockeyRegistration(jockey, openRace);
        saveJockeyRegistration(otherJockey, otherRace);

        MvcResult result = mockMvc.perform(get("/api/jockey/race-registrations/my")
                        .header("Authorization", token(jockey.getUser())))
                .andExpect(status().isOk())
                .andReturn();

        JockeyRaceRegistrationResponse[] responses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                JockeyRaceRegistrationResponse[].class);
        assertEquals(1, responses.length);
        assertEquals(jockey.getJockeyId(), responses[0].getJockeyId());
        assertEquals(openRace.getRaceId(), responses[0].getRaceId());
    }

    @Test
    void myRegistrationsRequiresJockeyRole() throws Exception {
        mockMvc.perform(get("/api/jockey/race-registrations/my"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/jockey/race-registrations/my")
                        .header("Authorization", token(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerGetsOnlyEligibleJockeysForRegistrationRace() throws Exception {
        JockeyRaceRegistration eligible = saveJockeyRegistration(jockey, openRace);
        saveJockeyRegistration(otherJockey, otherRace);

        MvcResult result = mockMvc.perform(get("/api/owner/registrations/"
                        + approvedRegistration.getRegistrationId() + "/eligible-jockeys")
                        .header("Authorization", token(owner)))
                .andExpect(status().isOk())
                .andReturn();

        EligibleJockeyForInvitationResponse[] responses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                EligibleJockeyForInvitationResponse[].class);
        assertEquals(1, responses.length);
        assertEquals(eligible.getJockeyRaceRegistrationId(), responses[0].getJockeyRaceRegistrationId());
        assertEquals(jockey.getJockeyId(), responses[0].getJockeyId());
        assertTrue(responses[0].getCanInvite());
    }

    @Test
    void eligibleJockeyEndpointValidatesOwnerAndRegistrationStatus() throws Exception {
        saveJockeyRegistration(jockey, openRace);

        mockMvc.perform(get("/api/owner/registrations/"
                        + approvedRegistration.getRegistrationId() + "/eligible-jockeys")
                        .header("Authorization", token(otherOwner)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/owner/registrations/"
                        + pendingRegistration.getRegistrationId() + "/eligible-jockeys")
                        .header("Authorization", token(owner)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/owner/registrations/999999999/eligible-jockeys")
                        .header("Authorization", token(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eligibleJockeysEmptyWhenEntryInvitationOrDeadlineBlocksInvite() throws Exception {
        saveJockeyRegistration(jockey, openRace);
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockey)
                .invitationStatus(RaceInvitationStatus.SENT)
                .build());

        assertEligibleListEmpty(approvedRegistration);

        raceInvitationRepository.delete(invitation);
        raceEntryRepository.save(RaceEntry.builder()
                .race(openRace)
                .registration(approvedRegistration)
                .horse(approvedRegistration.getHorse())
                .jockey(jockey)
                .entryStatus("declared")
                .build());
        assertEligibleListEmpty(approvedRegistration);
    }

    @Test
    void expiredRaceReturnsEmptyEligibleList() throws Exception {
        saveJockeyRegistration(jockey, openRace);
        openRace.setRegistrationCloseAt(LocalDateTime.now().minusMinutes(1));
        raceRepository.save(openRace);
        assertEligibleListEmpty(approvedRegistration);
    }

    @Test
    void ownerCanInviteOnlyRegisteredJockey() throws Exception {
        saveJockeyRegistration(jockey, openRace);

        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .raceRegistrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockey.getJockeyId())
                .message("Please join")
                .build();
        mockMvc.perform(post("/api/invitations")
                        .header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void ownerCannotInviteUnregisteredJockey() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .raceRegistrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockey.getJockeyId())
                .build();
        mockMvc.perform(post("/api/invitations")
                        .header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Jockey has not registered for this race"));
    }

    @Test
    void acceptingInvitationOnlyMarksAcceptedAndDoesNotCreateRaceEntry() throws Exception {
        saveJockeyRegistration(jockey, openRace);
        CreateInvitationRequest request = CreateInvitationRequest.builder()
                .raceRegistrationId(approvedRegistration.getRegistrationId())
                .jockeyId(jockey.getJockeyId())
                .build();
        MvcResult createResult = mockMvc.perform(post("/api/invitations")
                        .header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        InvitationResponse invitation = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                InvitationResponse.class);

        MvcResult acceptResult = mockMvc.perform(put("/api/invitations/"
                        + invitation.getInvitationId() + "/accept")
                        .header("Authorization", token(jockey.getUser())))
                .andExpect(status().isOk())
                .andReturn();
        InvitationResponse accepted = objectMapper.readValue(
                acceptResult.getResponse().getContentAsString(),
                InvitationResponse.class);

        assertEquals("ACCEPTED", accepted.getStatus());
        assertFalse(raceEntryRepository.existsByRegistrationRegistrationId(
                approvedRegistration.getRegistrationId()));
    }

    @Test
    void acceptingInvitationRequiresActiveJockeyRaceRegistration() throws Exception {
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockey)
                .invitationStatus(RaceInvitationStatus.SENT)
                .build());

        mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                        .header("Authorization", token(jockey.getUser())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Jockey has not registered for this race"));

        assertFalse(raceEntryRepository.existsByRegistrationRegistrationId(
                approvedRegistration.getRegistrationId()));
        assertEquals(RaceInvitationStatus.SENT,
                raceInvitationRepository.findById(invitation.getInvitationId()).orElseThrow().getInvitationStatus());
    }

    @Test
    void createEntryConflictDoesNotMarkInvitationUsed() throws Exception {
        saveJockeyRegistration(jockey, openRace);
        raceEntryRepository.save(RaceEntry.builder()
                .race(openRace)
                .registration(otherOwnerRegistration)
                .horse(otherOwnerRegistration.getHorse())
                .jockey(jockey)
                .entryStatus("declared")
                .build());
        RaceInvitation invitation = raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(approvedRegistration)
                .jockey(jockey)
                .invitationStatus(RaceInvitationStatus.SENT)
                .build());

        mockMvc.perform(put("/api/invitations/" + invitation.getInvitationId() + "/accept")
                .header("Authorization", token(jockey.getUser())))
                .andExpect(status().isOk());

        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .invitationId(invitation.getInvitationId())
                .build();

        mockMvc.perform(post("/api/entries")
                        .header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertEquals(RaceInvitationStatus.ACCEPTED,
                raceInvitationRepository.findById(invitation.getInvitationId()).orElseThrow().getInvitationStatus());
        assertFalse(raceEntryRepository.existsByRegistrationRegistrationId(
                approvedRegistration.getRegistrationId()));
    }

    private org.springframework.test.web.servlet.ResultActions registerForRace(
            Jockey targetJockey,
            Race race,
            String note) throws Exception {
        CreateJockeyRaceRegistrationRequest request = CreateJockeyRaceRegistrationRequest.builder()
                .raceId(race.getRaceId())
                .note(note)
                .build();
        return mockMvc.perform(post("/api/jockey/race-registrations")
                .header("Authorization", token(targetJockey.getUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private void assertEligibleListEmpty(RaceRegistration registration) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/owner/registrations/"
                        + registration.getRegistrationId() + "/eligible-jockeys")
                        .header("Authorization", token(owner)))
                .andExpect(status().isOk())
                .andReturn();
        EligibleJockeyForInvitationResponse[] responses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                EligibleJockeyForInvitationResponse[].class);
        assertEquals(0, responses.length);
    }

    private User createUser(String email, String name, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .fullName(name)
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("0900000000")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Jockey createJockey(User user, String status, String weight) {
        return jockeyRepository.save(Jockey.builder()
                .user(user)
                .weight(new BigDecimal(weight))
                .experienceYears((short) 3)
                .status(status)
                .build());
    }

    private Race createRace(
            RaceMeeting meeting,
            RaceCondition condition,
            String name,
            short raceNo,
            RaceStatus status,
            LocalDateTime openAt,
            LocalDateTime closeAt) {
        return raceRepository.save(Race.builder()
                .raceMeeting(meeting)
                .raceCondition(condition)
                .raceName(name)
                .raceNo(raceNo)
                .scheduledTime(LocalDateTime.now().plusDays(3))
                .registrationOpenAt(openAt)
                .registrationCloseAt(closeAt)
                .status(status)
                .build());
    }

    private RaceRegistration createOwnerRegistration(
            User targetOwner,
            Race race,
            String horseName,
            RaceRegistrationStatus status) {
        Horse horse = horseRepository.save(Horse.builder()
                .owner(targetOwner)
                .horseName(horseName)
                .color("Brown")
                .age((short) 4)
                .gender("M")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 3)
                .status("active")
                .build());
        return raceRegistrationRepository.save(RaceRegistration.builder()
                .race(race)
                .horse(horse)
                .submittedBy(targetOwner)
                .status(status)
                .build());
    }

    private JockeyRaceRegistration saveJockeyRegistration(Jockey targetJockey, Race race) {
        return jockeyRaceRegistrationRepository.save(JockeyRaceRegistration.builder()
                .race(race)
                .jockey(targetJockey)
                .status(JockeyRaceRegistrationStatus.REGISTERED)
                .note("Available")
                .build());
    }

    private String token(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
