package com.solofounder.horseracing;

import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.entry.JockeyWeightCheckRequest;
import com.solofounder.horseracing.dto.entry.JockeyWeightCheckResponse;
import com.solofounder.horseracing.dto.entry.RaceEntryResponse;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceCondition;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.RaceMeeting;
import com.solofounder.horseracing.model.RaceRegistration;
import com.solofounder.horseracing.model.Racecourse;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.Season;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.RaceConditionRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceMeetingRepository;
import com.solofounder.horseracing.repository.RaceRegistrationRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RacecourseRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
import com.solofounder.horseracing.repository.SeasonRepository;
import com.solofounder.horseracing.repository.StaffRepository;
import com.solofounder.horseracing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RefereeRaceIntegrationTests {

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
    private HorseRepository horseRepository;

    @Autowired
    private JockeyRepository jockeyRepository;

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
    private RaceEntryRepository raceEntryRepository;

    private String assignedRefereeToken;
    private String otherRefereeToken;
    private String staffToken;
    private Referee assignedReferee;
    private Referee otherReferee;
    private Race assignedRace;
    private Race otherRace;
    private RaceEntry assignedEntry;

    @BeforeEach
    void setupData() {
        String suffix = String.valueOf(System.nanoTime());

        User staffUser = createUser("ref-race-staff-" + suffix + "@example.com", "Referee Race Staff", Role.STAFF);
        User assignedRefereeUser = createUser("ref-race-referee-" + suffix + "@example.com", "Assigned Referee", Role.REFEREE);
        User otherRefereeUser = createUser("ref-race-other-referee-" + suffix + "@example.com", "Other Referee", Role.REFEREE);
        User owner = createUser("ref-race-owner-" + suffix + "@example.com", "Owner", Role.OWNER);

        Staff staff = staffRepository.save(Staff.builder()
                .user(staffUser)
                .staffCode("RFR-STF-" + suffix)
                .department("Race Ops")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        assignedReferee = refereeRepository.save(Referee.builder()
                .user(assignedRefereeUser)
                .licenseNo("RFR-REF-" + suffix)
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
        otherReferee = refereeRepository.save(Referee.builder()
                .user(otherRefereeUser)
                .licenseNo("RFR-OREF-" + suffix)
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        assignedRace = createRace(staff, assignedReferee, "Assigned Referee Race " + suffix);
        otherRace = createRace(staff, otherReferee, "Other Referee Race " + suffix);
        assignedEntry = createEntry(assignedRace, owner, suffix);
        createEntry(otherRace, owner, suffix + "-other");

        assignedRefereeToken = token(assignedRefereeUser);
        otherRefereeToken = token(otherRefereeUser);
        staffToken = token(staffUser);
    }

    @Test
    void assignedRefereeGetsOnlyAssignedRaces() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/referee/races")
                        .header("Authorization", assignedRefereeToken))
                .andExpect(status().isOk())
                .andReturn();

        List<RaceResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<RaceResponse>>() {
                });
        assertTrue(response.stream().anyMatch(race -> race.getRaceId().equals(assignedRace.getRaceId())));
        assertFalse(response.stream().anyMatch(race -> race.getRaceId().equals(otherRace.getRaceId())));
        RaceResponse assigned = response.stream()
                .filter(race -> race.getRaceId().equals(assignedRace.getRaceId()))
                .findFirst()
                .orElseThrow();
        assertEquals(assignedReferee.getRefereeId(), assigned.getRefereeId());
        assertEquals("Assigned Referee", assigned.getRefereeName());
    }

    @Test
    void getAssignedRacesNoTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/referee/races"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAssignedRacesWrongRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/referee/races")
                        .header("Authorization", staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedRefereeGetsEntriesForAssignedRace() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/referee/races/" + assignedRace.getRaceId() + "/entries")
                        .header("Authorization", assignedRefereeToken))
                .andExpect(status().isOk())
                .andReturn();

        List<RaceEntryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<RaceEntryResponse>>() {
                });
        assertEquals(1, response.size());
        assertEquals(assignedEntry.getEntryId(), response.get(0).getEntryId());
        assertEquals(assignedRace.getRaceId(), response.get(0).getRaceId());
        assertEquals("declared", response.get(0).getEntryStatus());
    }

    @Test
    void assignedRefereeCannotGetEntriesForUnassignedRace() throws Exception {
        mockMvc.perform(get("/api/referee/races/" + otherRace.getRaceId() + "/entries")
                        .header("Authorization", assignedRefereeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEntriesRaceNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/referee/races/999999999/entries")
                        .header("Authorization", assignedRefereeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEntriesNoTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/referee/races/" + assignedRace.getRaceId() + "/entries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEntriesWrongRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/referee/races/" + assignedRace.getRaceId() + "/entries")
                        .header("Authorization", staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void weightCheckJockeyLessWeightAddsLead() throws Exception {
        assignedEntry.setHandicapWeight(new BigDecimal("55.00"));
        raceEntryRepository.save(assignedEntry);

        JockeyWeightCheckRequest request = JockeyWeightCheckRequest.builder()
                .jockeyActualWeight(new BigDecimal("52.00"))
                .build();

        MvcResult result = mockMvc.perform(put("/api/referee/race-entries/" + assignedEntry.getEntryId() + "/weight-check")
                        .header("Authorization", assignedRefereeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JockeyWeightCheckResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), JockeyWeightCheckResponse.class);
        assertEquals(0, new BigDecimal("55.00").compareTo(response.getHandicapWeight()));
        assertEquals(0, new BigDecimal("52.00").compareTo(response.getJockeyActualWeight()));
        assertEquals(0, new BigDecimal("3.00").compareTo(response.getLeadWeight()));
        assertEquals(0, new BigDecimal("55.00").compareTo(response.getCarriedWeight()));
        assertEquals("PASSED", response.getWeightCheckStatus());

        // Verify database entry was updated
        RaceEntry updated = raceEntryRepository.findById(assignedEntry.getEntryId()).orElseThrow();
        assertEquals(0, new BigDecimal("52.00").compareTo(updated.getJockeyActualWeight()));
        assertEquals(0, new BigDecimal("3.00").compareTo(updated.getLeadWeight()));
        assertEquals(0, new BigDecimal("55.00").compareTo(updated.getCarriedWeight()));
        assertEquals("PASSED", updated.getWeightCheckStatus());
        assertEquals(assignedReferee.getRefereeId(), updated.getWeightCheckedBy().getRefereeId());
        assertTrue(updated.getWeightCheckedAt().isBefore(LocalDateTime.now().plusSeconds(5)));
    }

    @Test
    void weightCheckJockeyMoreWeightKeepsActual() throws Exception {
        assignedEntry.setHandicapWeight(new BigDecimal("55.00"));
        raceEntryRepository.save(assignedEntry);

        JockeyWeightCheckRequest request = JockeyWeightCheckRequest.builder()
                .jockeyActualWeight(new BigDecimal("57.00"))
                .build();

        MvcResult result = mockMvc.perform(put("/api/referee/race-entries/" + assignedEntry.getEntryId() + "/weight-check")
                        .header("Authorization", assignedRefereeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JockeyWeightCheckResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), JockeyWeightCheckResponse.class);
        assertEquals(0, new BigDecimal("55.00").compareTo(response.getHandicapWeight()));
        assertEquals(0, new BigDecimal("57.00").compareTo(response.getJockeyActualWeight()));
        assertEquals(0, new BigDecimal("0.00").compareTo(response.getLeadWeight()));
        assertEquals(0, new BigDecimal("57.00").compareTo(response.getCarriedWeight()));
        assertEquals("PASSED", response.getWeightCheckStatus());

        // Verify database entry was updated
        RaceEntry updated = raceEntryRepository.findById(assignedEntry.getEntryId()).orElseThrow();
        assertEquals(0, new BigDecimal("57.00").compareTo(updated.getJockeyActualWeight()));
        assertEquals(0, new BigDecimal("0.00").compareTo(updated.getLeadWeight()));
        assertEquals(0, new BigDecimal("57.00").compareTo(updated.getCarriedWeight()));
        assertEquals("PASSED", updated.getWeightCheckStatus());
    }

    @Test
    void weightCheckForbiddenForUnassignedReferee() throws Exception {
        assignedEntry.setHandicapWeight(new BigDecimal("55.00"));
        raceEntryRepository.save(assignedEntry);

        JockeyWeightCheckRequest request = JockeyWeightCheckRequest.builder()
                .jockeyActualWeight(new BigDecimal("52.00"))
                .build();

        mockMvc.perform(put("/api/referee/race-entries/" + assignedEntry.getEntryId() + "/weight-check")
                        .header("Authorization", otherRefereeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void weightCheckWrongRoleReturns403() throws Exception {
        JockeyWeightCheckRequest request = JockeyWeightCheckRequest.builder()
                .jockeyActualWeight(new BigDecimal("52.00"))
                .build();

        mockMvc.perform(put("/api/referee/race-entries/" + assignedEntry.getEntryId() + "/weight-check")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void weightCheckNoTokenReturns401() throws Exception {
        JockeyWeightCheckRequest request = JockeyWeightCheckRequest.builder()
                .jockeyActualWeight(new BigDecimal("52.00"))
                .build();

        mockMvc.perform(put("/api/referee/race-entries/" + assignedEntry.getEntryId() + "/weight-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void weightCheckInvalidWeightsReturns400() throws Exception {
        assignedEntry.setHandicapWeight(new BigDecimal("55.00"));
        raceEntryRepository.save(assignedEntry);

        JockeyWeightCheckRequest request = JockeyWeightCheckRequest.builder()
                .jockeyActualWeight(new BigDecimal("-1.00"))
                .build();

        mockMvc.perform(put("/api/referee/race-entries/" + assignedEntry.getEntryId() + "/weight-check")
                        .header("Authorization", assignedRefereeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void weightCheckEntryNotFoundReturns404() throws Exception {
        JockeyWeightCheckRequest request = JockeyWeightCheckRequest.builder()
                .jockeyActualWeight(new BigDecimal("52.00"))
                .build();

        mockMvc.perform(put("/api/referee/race-entries/9999999/weight-check")
                        .header("Authorization", assignedRefereeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    private RaceEntry createEntry(Race race, User owner, String suffix) {
        Horse horse = horseRepository.save(Horse.builder()
                .owner(owner)
                .horseName("Referee Race Horse " + suffix)
                .color("Bay")
                .age((short) 4)
                .gender("M")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .status("active")
                .build());
        User jockeyUser = createUser("ref-race-jockey-" + suffix + "@example.com", "Referee Race Jockey " + suffix, Role.JOCKEY);
        Jockey jockey = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser)
                .weight(new BigDecimal("50.00"))
                .experienceYears((short) 2)
                .status("available")
                .build());
        RaceRegistration registration = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(race)
                .horse(horse)
                .submittedBy(owner)
                .status(RaceRegistrationStatus.APPROVED)
                .build());
        return raceEntryRepository.save(RaceEntry.builder()
                .race(race)
                .registration(registration)
                .horse(horse)
                .jockey(jockey)
                .gateNumber((short) 1)
                .entryStatus("declared")
                .build());
    }

    private Race createRace(Staff staff, Referee referee, String raceName) {
        Season season = seasonRepository.save(Season.builder()
                .seasonName(raceName + " Season")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .status("active")
                .build());

        Racecourse racecourse = racecourseRepository.save(Racecourse.builder()
                .racecourseName(raceName + " Course")
                .location("Hanoi")
                .surfaceType("turf")
                .capacity(10000)
                .build());

        RaceMeeting meeting = raceMeetingRepository.save(RaceMeeting.builder()
                .meetingName(raceName + " Meeting")
                .season(season)
                .racecourse(racecourse)
                .meetingDate(LocalDate.now().plusDays(7))
                .status("scheduled")
                .build());

        RaceCondition condition = raceConditionRepository.save(RaceCondition.builder()
                .conditionName(raceName + " Condition")
                .distance(1200)
                .trackType("turf")
                .minEntries((short) 1)
                .maxEntries((short) 10)
                .classRequirement("1-5")
                .build());

        return raceRepository.save(Race.builder()
                .raceMeeting(meeting)
                .raceCondition(condition)
                .staff(staff)
                .referee(referee)
                .raceName(raceName)
                .raceNo((short) 1)
                .scheduledTime(LocalDateTime.now().plusDays(7))
                .registrationOpenAt(LocalDateTime.now().minusDays(1))
                .registrationCloseAt(LocalDateTime.now().plusDays(2))
                .status(RaceStatus.RUNNING)
                .build());
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

    private String token(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
