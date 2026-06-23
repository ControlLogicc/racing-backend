package com.solofounder.horseracing;

import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.registration.ApprovedRegistrationForInvitationResponse;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.*;
import com.solofounder.horseracing.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnerRegistrationIntegrationTests {

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

    private String ownerToken;
    private String otherOwnerToken;
    private String staffToken;
    private User owner;
    private User otherOwner;
    private Staff staff;
    private Jockey jockey;
    private Race race;
    private int horseCounter;

    @BeforeEach
    void setup() {
        String suffix = String.valueOf(System.nanoTime());

        owner = createUser("owner-" + suffix + "@owner-registration-test.com", "Owner Registration", Role.OWNER);
        ownerToken = "Bearer " + jwtService.generateToken(owner);

        otherOwner = createUser("other-owner-" + suffix + "@owner-registration-test.com", "Other Owner", Role.OWNER);
        otherOwnerToken = "Bearer " + jwtService.generateToken(otherOwner);

        User staffUser = createUser("staff-" + suffix + "@owner-registration-test.com", "Staff Registration", Role.STAFF);
        staffToken = "Bearer " + jwtService.generateToken(staffUser);
        staff = staffRepository.save(Staff.builder()
                .user(staffUser)
                .staffCode("ORG-STF-" + suffix)
                .department("Operations")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        User jockeyUser = createUser("jockey-" + suffix + "@owner-registration-test.com", "Jockey Registration", Role.JOCKEY);
        jockey = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser)
                .weight(new BigDecimal("52.00"))
                .experienceYears((short) 3)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());

        race = createRace(suffix, LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(6));
    }

    @Test
    void testOwnerGetsOnlyOwnApprovedRegistrations() throws Exception {
        RaceRegistration approved = createRegistration(owner, RaceRegistrationStatus.APPROVED, "Approved Horse");
        createRegistration(owner, RaceRegistrationStatus.PENDING, "Pending Horse");
        createRegistration(owner, RaceRegistrationStatus.REJECTED, "Rejected Horse");
        createRegistration(otherOwner, RaceRegistrationStatus.APPROVED, "Other Owner Horse");

        ApprovedRegistrationForInvitationResponse[] responses = performApprovedRegistrations(ownerToken);

        assertTrue(Arrays.stream(responses).anyMatch(response -> response.getRegistrationId().equals(approved.getRegistrationId())));
        assertTrue(Arrays.stream(responses).allMatch(response -> "APPROVED".equals(response.getRegistrationStatus())));
        assertTrue(Arrays.stream(responses).noneMatch(response -> "Pending Horse".equals(response.getHorseName())));
        assertTrue(Arrays.stream(responses).noneMatch(response -> "Rejected Horse".equals(response.getHorseName())));
        assertTrue(Arrays.stream(responses).noneMatch(response -> "Other Owner Horse".equals(response.getHorseName())));

        ApprovedRegistrationForInvitationResponse approvedResponse = findResponse(responses, approved.getRegistrationId());
        assertEquals(race.getRaceId(), approvedResponse.getRaceId());
        assertEquals("Approved Horse", approvedResponse.getHorseName());
        assertNull(approvedResponse.getInvitationId());
        assertNull(approvedResponse.getEntryId());
        assertTrue(approvedResponse.getCanInviteJockey());
    }

    @Test
    void testRegistrationWithRaceEntryCannotInviteJockey() throws Exception {
        RaceRegistration registration = createRegistration(owner, RaceRegistrationStatus.APPROVED, "Entry Horse");
        RaceInvitation invitation = createInvitation(registration, RaceInvitationStatus.USED);
        RaceEntry entry = raceEntryRepository.save(RaceEntry.builder()
                .race(race)
                .registration(registration)
                .invitation(invitation)
                .horse(registration.getHorse())
                .jockey(jockey)
                .confirmedByStaff(staff)
                .gateNumber((short) 1)
                .handicapWeight(new BigDecimal("55.00"))
                .entryStatus("declared")
                .build());

        ApprovedRegistrationForInvitationResponse response = findResponse(
                performApprovedRegistrations(ownerToken),
                registration.getRegistrationId());

        assertEquals(entry.getEntryId(), response.getEntryId());
        assertEquals(invitation.getInvitationId(), response.getInvitationId());
        assertEquals("USED", response.getInvitationStatus());
        assertFalse(response.getCanInviteJockey());
    }

    @Test
    void testRegistrationWithActiveInvitationCannotInviteJockey() throws Exception {
        RaceRegistration sent = createRegistration(owner, RaceRegistrationStatus.APPROVED, "Sent Invitation Horse");
        RaceRegistration pendingResponse = createRegistration(owner, RaceRegistrationStatus.APPROVED, "Pending Invitation Horse");
        RaceRegistration accepted = createRegistration(owner, RaceRegistrationStatus.APPROVED, "Accepted Invitation Horse");

        createInvitation(sent, RaceInvitationStatus.SENT);
        createInvitation(pendingResponse, RaceInvitationStatus.PENDING_RESPONSE);
        createInvitation(accepted, RaceInvitationStatus.ACCEPTED);

        ApprovedRegistrationForInvitationResponse[] responses = performApprovedRegistrations(ownerToken);

        assertFalse(findResponse(responses, sent.getRegistrationId()).getCanInviteJockey());
        assertFalse(findResponse(responses, pendingResponse.getRegistrationId()).getCanInviteJockey());
        assertFalse(findResponse(responses, accepted.getRegistrationId()).getCanInviteJockey());
        assertEquals("SENT", findResponse(responses, sent.getRegistrationId()).getInvitationStatus());
        assertEquals("PENDING_RESPONSE", findResponse(responses, pendingResponse.getRegistrationId()).getInvitationStatus());
        assertEquals("ACCEPTED", findResponse(responses, accepted.getRegistrationId()).getInvitationStatus());
    }

    @Test
    void testExpiredRegistrationCloseAtCannotInviteJockey() throws Exception {
        Race expiredRace = createRace("expired-" + System.nanoTime(), LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
        RaceRegistration registration = createRegistration(owner, expiredRace, RaceRegistrationStatus.APPROVED, "Expired Horse");

        ApprovedRegistrationForInvitationResponse response = findResponse(
                performApprovedRegistrations(ownerToken),
                registration.getRegistrationId());

        assertFalse(response.getCanInviteJockey());
    }

    @Test
    void testApprovedRegistrationsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/owner/registrations/approved"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testApprovedRegistrationsWrongRoleReturns403() throws Exception {
        createRegistration(owner, RaceRegistrationStatus.APPROVED, "Forbidden Horse");

        mockMvc.perform(get("/api/owner/registrations/approved")
                        .header("Authorization", staffToken))
                .andExpect(status().isForbidden());
    }

    private ApprovedRegistrationForInvitationResponse[] performApprovedRegistrations(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/owner/registrations/approved")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), ApprovedRegistrationForInvitationResponse[].class);
    }

    private ApprovedRegistrationForInvitationResponse findResponse(ApprovedRegistrationForInvitationResponse[] responses, Long registrationId) {
        return Arrays.stream(responses)
                .filter(response -> response.getRegistrationId().equals(registrationId))
                .findFirst()
                .orElseThrow();
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

    private Race createRace(String suffix, LocalDateTime registrationCloseAt, LocalDateTime scheduledTime) {
        Season season = seasonRepository.save(Season.builder()
                .seasonName("Owner Registration Season " + suffix)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        Racecourse racecourse = racecourseRepository.save(Racecourse.builder()
                .racecourseName("Owner Registration Course " + suffix)
                .location("HCMC")
                .surfaceType("turf")
                .capacity(10000)
                .createdAt(LocalDateTime.now())
                .build());

        RaceMeeting meeting = raceMeetingRepository.save(RaceMeeting.builder()
                .season(season)
                .racecourse(racecourse)
                .meetingName("Owner Registration Meeting " + suffix)
                .meetingDate(scheduledTime.toLocalDate())
                .status("scheduled")
                .createdAt(LocalDateTime.now())
                .build());

        RaceCondition condition = raceConditionRepository.save(RaceCondition.builder()
                .conditionName("Owner Registration Condition " + suffix)
                .distance(1200)
                .trackType("turf")
                .minEntries((short) 1)
                .maxEntries((short) 10)
                .classRequirement("5")
                .createdAt(LocalDateTime.now())
                .build());

        return raceRepository.save(Race.builder()
                .raceMeeting(meeting)
                .raceCondition(condition)
                .staff(staff)
                .raceName("Owner Registration Race " + suffix)
                .raceNo((short) 1)
                .scheduledTime(scheduledTime)
                .registrationOpenAt(LocalDateTime.now().minusDays(1))
                .registrationCloseAt(registrationCloseAt)
                .status(RaceStatus.OPEN_FOR_ENTRY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private RaceRegistration createRegistration(User registrationOwner, RaceRegistrationStatus status, String horseName) {
        return createRegistration(registrationOwner, race, status, horseName);
    }

    private RaceRegistration createRegistration(User registrationOwner, Race targetRace, RaceRegistrationStatus status, String horseName) {
        Horse horse = horseRepository.save(Horse.builder()
                .owner(registrationOwner)
                .horseName(horseName)
                .color("Bay")
                .age((short) 4)
                .gender("M")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .status("active")
                .totalWins(0)
                .build());
        horseCounter++;

        return raceRegistrationRepository.save(RaceRegistration.builder()
                .race(targetRace)
                .horse(horse)
                .submittedBy(registrationOwner)
                .approvedBy(staff)
                .status(status)
                .submittedAt(LocalDateTime.now().minusMinutes(30L + horseCounter))
                .reviewedAt(status == RaceRegistrationStatus.APPROVED || status == RaceRegistrationStatus.REJECTED
                        ? LocalDateTime.now().minusMinutes(10)
                        : null)
                .createdAt(LocalDateTime.now().minusMinutes(30L + horseCounter))
                .build());
    }

    private RaceInvitation createInvitation(RaceRegistration registration, RaceInvitationStatus status) {
        return raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(registration)
                .jockey(jockey)
                .invitationStatus(status)
                .sentAt(LocalDateTime.now().minusMinutes(5))
                .respondedAt(status == RaceInvitationStatus.ACCEPTED ? LocalDateTime.now() : null)
                .message("Please join this race")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build());
    }
}
