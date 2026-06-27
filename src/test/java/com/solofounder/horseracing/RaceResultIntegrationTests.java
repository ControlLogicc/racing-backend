package com.solofounder.horseracing;

import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.race.CreateRaceResultRequest;
import com.solofounder.horseracing.dto.race.RaceResultResponse;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.PrizeStructure;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceCondition;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.RaceMeeting;
import com.solofounder.horseracing.model.RaceRegistration;
import com.solofounder.horseracing.model.RaceResult;
import com.solofounder.horseracing.model.Racecourse;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.Season;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.PrizeStructureRepository;
import com.solofounder.horseracing.repository.RaceConditionRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceMeetingRepository;
import com.solofounder.horseracing.repository.RaceRegistrationRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RaceResultRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RaceResultIntegrationTests {

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
    private HorseRepository horseRepository;

    @Autowired
    private JockeyRepository jockeyRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private RacecourseRepository racecourseRepository;

    @Autowired
    private RefereeRepository refereeRepository;

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

    @Autowired
    private RaceResultRepository raceResultRepository;

    @Autowired
    private PrizeStructureRepository prizeStructureRepository;

    private String adminToken;
    private String assignedStaffToken;
    private String otherStaffToken;
    private String assignedRefereeToken;
    private String otherRefereeToken;
    private String ownerToken;
    private String jockeyToken;
    private Race race;
    private final List<RaceEntry> entries = new ArrayList<>();
    private Horse firstHorse;

    @BeforeEach
    void setupData() {
        String suffix = String.valueOf(System.nanoTime());

        User admin = createUser("result-admin-" + suffix + "@example.com", "Result Admin", Role.ADMIN);
        User assignedStaffUser = createUser("result-staff-" + suffix + "@example.com", "Result Staff", Role.STAFF);
        User otherStaffUser = createUser("result-other-staff-" + suffix + "@example.com", "Other Staff", Role.STAFF);
        User assignedRefereeUser = createUser("result-referee-" + suffix + "@example.com", "Result Referee", Role.REFEREE);
        User otherRefereeUser = createUser("result-other-referee-" + suffix + "@example.com", "Other Referee", Role.REFEREE);
        User owner = createUser("result-owner-" + suffix + "@example.com", "Result Owner", Role.OWNER);
        User jockeyRoleUser = createUser("result-jockey-token-" + suffix + "@example.com", "Result Jockey Token", Role.JOCKEY);

        Staff assignedStaff = staffRepository.save(Staff.builder()
                .user(assignedStaffUser)
                .staffCode("RES-STF-" + suffix)
                .department("Race Ops")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());
        staffRepository.save(Staff.builder()
                .user(otherStaffUser)
                .staffCode("RES-OSTF-" + suffix)
                .department("Race Ops")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        Referee assignedReferee = refereeRepository.save(Referee.builder()
                .user(assignedRefereeUser)
                .licenseNo("RES-REF-" + suffix)
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
        refereeRepository.save(Referee.builder()
                .user(otherRefereeUser)
                .licenseNo("RES-OREF-" + suffix)
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        race = createRace(assignedStaff, assignedReferee, RaceStatus.RUNNING, "Race Result API Race " + suffix);
        createPrizeStructures(race);
        createEntries(race, owner, suffix);

        adminToken = token(admin);
        assignedStaffToken = token(assignedStaffUser);
        otherStaffToken = token(otherStaffUser);
        assignedRefereeToken = token(assignedRefereeUser);
        otherRefereeToken = token(otherRefereeUser);
        ownerToken = token(owner);
        jockeyToken = token(jockeyRoleUser);
    }

    @Test
    void adminRecordResultSuccess() throws Exception {
        RaceResultResponse response = readResult(postResult(adminToken, entries.get(0).getEntryId(), (short) 1, "OFFICIAL")
                .andExpect(status().isOk())
                .andReturn());

        assertNotNull(response.getResultId());
        assertEquals(entries.get(0).getEntryId(), response.getEntryId());
        assertEquals(race.getRaceId(), response.getRaceId());
        assertEquals(firstHorse.getHorseId(), response.getHorseId());
        assertEquals("OFFICIAL", response.getResultStatus());
        assertEquals(new BigDecimal("50000000.00"), response.getPrizeAmount());
        assertEquals(new BigDecimal("50.00"), response.getScoreAwarded());
    }

    @Test
    void assignedStaffRecordResultSuccess() throws Exception {
        RaceResultResponse response = readResult(postResult(assignedStaffToken, entries.get(1).getEntryId(), (short) 2, null)
                .andExpect(status().isOk())
                .andReturn());

        assertEquals("PROVISIONAL", response.getResultStatus());
        assertEquals(BigDecimal.ZERO, response.getPrizeAmount());
        assertEquals(BigDecimal.ZERO, response.getScoreAwarded());
    }

    @Test
    void assignedRefereeRecordResultSuccess() throws Exception {
        RaceResultResponse response = readResult(postResult(assignedRefereeToken, entries.get(1).getEntryId(), (short) 2, null)
                .andExpect(status().isOk())
                .andReturn());

        assertEquals(entries.get(1).getEntryId(), response.getEntryId());
        assertEquals(race.getRaceId(), response.getRaceId());
        assertEquals("PROVISIONAL", response.getResultStatus());
    }

    @Test
    void staffNotAssignedReturns403() throws Exception {
        postResult(otherStaffToken, entries.get(0).getEntryId(), (short) 1, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void refereeNotAssignedReturns403() throws Exception {
        postResult(otherRefereeToken, entries.get(0).getEntryId(), (short) 1, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenPostReturns401() throws Exception {
        CreateRaceResultRequest request = validRequest(entries.get(0).getEntryId(), (short) 1, null);

        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerAndJockeyPostReturns403() throws Exception {
        postResult(ownerToken, entries.get(0).getEntryId(), (short) 1, null)
                .andExpect(status().isForbidden());
        postResult(jockeyToken, entries.get(0).getEntryId(), (short) 1, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void entryIdNotFoundReturns404() throws Exception {
        postResult(adminToken, 999999999L, (short) 1, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void positionLessThanOneReturns400() throws Exception {
        postResult(adminToken, entries.get(0).getEntryId(), (short) 0, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void positionGreaterThanEntryCountReturns400() throws Exception {
        postResult(adminToken, entries.get(0).getEntryId(), (short) 4, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEntryIdReturns409() throws Exception {
        postResult(adminToken, entries.get(0).getEntryId(), (short) 1, null)
                .andExpect(status().isOk());

        postResult(adminToken, entries.get(0).getEntryId(), (short) 2, null)
                .andExpect(status().isConflict());
    }

    @Test
    void duplicatePositionReturns409() throws Exception {
        postResult(adminToken, entries.get(0).getEntryId(), (short) 1, null)
                .andExpect(status().isOk());

        postResult(adminToken, entries.get(1).getEntryId(), (short) 1, null)
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRaceStatusReturns400() throws Exception {
        Race scheduledRace = createRace(race.getStaff(), race.getReferee(), RaceStatus.SCHEDULED, "Scheduled Result Race " + System.nanoTime());
        RaceEntry scheduledEntry = createEntry(scheduledRace, entries.get(0).getHorse(), entries.get(0).getJockey(), entries.get(0).getRegistration().getSubmittedBy(), (short) 9);

        postResult(adminToken, scheduledEntry.getEntryId(), (short) 1, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void failedEntryCannotHaveResult() throws Exception {
        RaceEntry entry = entries.get(0);
        entry.setEntryStatus("FAILED");
        raceEntryRepository.save(entry);

        postResult(adminToken, entry.getEntryId(), (short) 1, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignedRefereeCannotRecordScratchedEntry() throws Exception {
        RaceEntry entry = entries.get(0);
        entry.setEntryStatus("scratched");
        raceEntryRepository.save(entry);

        postResult(assignedRefereeToken, entry.getEntryId(), (short) 1, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getResultsByRaceReturnsSortedByPosition() throws Exception {
        createResult(entries.get(1), (short) 2, RaceResultStatus.PROVISIONAL);
        createResult(entries.get(0), (short) 1, RaceResultStatus.PROVISIONAL);

        MvcResult result = mockMvc.perform(get("/api/results/" + race.getRaceId())
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andReturn();

        List<RaceResultResponse> response = readResultList(result);
        assertEquals(2, response.size());
        assertEquals((short) 1, response.get(0).getPosition());
        assertEquals((short) 2, response.get(1).getPosition());
    }

    @Test
    void getResultsByHorseReturns200() throws Exception {
        createResult(entries.get(0), (short) 1, RaceResultStatus.PROVISIONAL);

        MvcResult result = mockMvc.perform(get("/api/results/horse/" + firstHorse.getHorseId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn();

        List<RaceResultResponse> response = readResultList(result);
        assertEquals(1, response.size());
        assertEquals(firstHorse.getHorseId(), response.get(0).getHorseId());
    }

    @Test
    void horseIdNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/results/horse/999999999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions postResult(String token, Long entryId, Short position, String status) throws Exception {
        CreateRaceResultRequest request = validRequest(entryId, position, status);
        return mockMvc.perform(post("/api/results")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private CreateRaceResultRequest validRequest(Long entryId, Short position, String status) {
        return CreateRaceResultRequest.builder()
                .entryId(entryId)
                .position(position)
                .finishTime(LocalTime.of(0, 1, 12, 345_000_000))
                .resultStatus(status)
                .build();
    }

    private RaceResultResponse readResult(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), RaceResultResponse.class);
    }

    private List<RaceResultResponse> readResultList(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<RaceResultResponse>>() {
        });
    }

    private RaceResult createResult(RaceEntry entry, Short position, RaceResultStatus status) {
        return raceResultRepository.save(RaceResult.builder()
                .race(entry.getRace())
                .entry(entry)
                .position(position)
                .finishTime(LocalTime.of(0, 1, 20))
                .resultStatus(status)
                .prizeAmount(BigDecimal.ZERO)
                .scoreAwarded(BigDecimal.ZERO)
                .build());
    }

    private void createPrizeStructures(Race targetRace) {
        prizeStructureRepository.save(PrizeStructure.builder()
                .race(targetRace)
                .position((short) 1)
                .amount(new BigDecimal("50000000.00"))
                .score(new BigDecimal("50.00"))
                .build());
        prizeStructureRepository.save(PrizeStructure.builder()
                .race(targetRace)
                .position((short) 2)
                .amount(new BigDecimal("30000000.00"))
                .score(new BigDecimal("30.00"))
                .build());
    }

    private void createEntries(Race targetRace, User owner, String suffix) {
        for (int i = 1; i <= 3; i++) {
            Horse horse = horseRepository.save(Horse.builder()
                    .owner(owner)
                    .horseName("Result Horse " + i + " " + suffix)
                    .color("Bay")
                    .age((short) (3 + i))
                    .gender(i % 2 == 0 ? "F" : "M")
                    .currentScore(BigDecimal.ZERO)
                    .horseClass((short) 5)
                    .status("active")
                    .totalWins(0)
                    .build());
            User jockeyUser = createUser("result-jockey-" + i + "-" + suffix + "@example.com", "Result Jockey " + i, Role.JOCKEY);
            Jockey jockey = jockeyRepository.save(Jockey.builder()
                    .user(jockeyUser)
                    .weight(new BigDecimal("50.00"))
                    .experienceYears((short) i)
                    .status("available")
                    .build());
            entries.add(createEntry(targetRace, horse, jockey, owner, (short) i));
            if (i == 1) {
                firstHorse = horse;
            }
        }
    }

    private RaceEntry createEntry(Race targetRace, Horse horse, Jockey jockey, User owner, Short gateNumber) {
        RaceRegistration registration = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(targetRace)
                .horse(horse)
                .submittedBy(owner)
                .status(RaceRegistrationStatus.APPROVED)
                .build());
        return raceEntryRepository.save(RaceEntry.builder()
                .race(targetRace)
                .registration(registration)
                .horse(horse)
                .jockey(jockey)
                .gateNumber(gateNumber)
                .entryStatus("PASSED")
                .build());
    }

    private Race createRace(Staff staff, Referee referee, RaceStatus status, String raceName) {
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
                .minEntries((short) 3)
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
                .status(status)
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
