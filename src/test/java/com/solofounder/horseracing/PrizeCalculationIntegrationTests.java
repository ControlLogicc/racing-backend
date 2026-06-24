package com.solofounder.horseracing;

import tools.jackson.databind.ObjectMapper;
import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.auth.AuthResponse;
import com.solofounder.horseracing.dto.auth.RegisterRequest;
import com.solofounder.horseracing.dto.race.RecalculatePrizesResponse;
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
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PrizeCalculationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JwtService jwtService;

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
    private RefereeReportRepository refereeReportRepository;

    @Autowired
    private RaceEntryRepository raceEntryRepository;

    @Autowired
    private RaceResultRepository raceResultRepository;

    @Autowired
    private PrizeStructureRepository prizeStructureRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String ownerToken;
    private Race testRace;
    private Horse horse1;
    private Horse horse2;
    private Horse horse3;
    private RaceResult result1;
    private RaceResult result2;
    private RaceResult result3;

    @BeforeEach
    void setupData() throws Exception {
        // Clean database tables
        refereeReportRepository.deleteAll();
        raceResultRepository.deleteAll();
        raceEntryRepository.deleteAll();
        raceInvitationRepository.deleteAll();
        raceRegistrationRepository.deleteAll();
        prizeStructureRepository.deleteAll();
        raceRepository.deleteAll();

        String suffix = String.valueOf(System.nanoTime());
        User adminUser = userRepository.save(User.builder()
                .fullName("Admin User")
                .email("admin-" + suffix + "@test-calculation.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("0912345678")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(adminUser);

        User ownerUser = userRepository.save(User.builder()
                .fullName("Owner User")
                .email("owner-" + suffix + "@test-calculation.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("0912345679")
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .build());
        ownerToken = "Bearer " + jwtService.generateToken(ownerUser);

        // Create horses
        horse1 = horseRepository.save(Horse.builder()
                .owner(ownerUser)
                .horseName("Horse One")
                .color("Black")
                .age((short) 4)
                .gender("M")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .status("active")
                .totalWins(0)
                .build());

        horse2 = horseRepository.save(Horse.builder()
                .owner(ownerUser)
                .horseName("Horse Two")
                .color("White")
                .age((short) 5)
                .gender("F")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .status("active")
                .totalWins(0)
                .build());

        horse3 = horseRepository.save(Horse.builder()
                .owner(ownerUser)
                .horseName("Horse Three")
                .color("Brown")
                .age((short) 3)
                .gender("M")
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5)
                .status("active")
                .totalWins(0)
                .build());

        // Create Jockeys
        String jockeySuffix = String.valueOf(System.nanoTime());
        User jockeyUser1 = userRepository.save(User.builder()
                .fullName("Jockey One")
                .email("j1-" + jockeySuffix + "@test-calculation.com")
                .passwordHash("hash")
                .phone("011111")
                .role(Role.JOCKEY)
                .status(UserStatus.ACTIVE)
                .build());
        Jockey jockey1 = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser1)
                .weight(new BigDecimal("50.0"))
                .experienceYears((short) 3)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());

        User jockeyUser2 = userRepository.save(User.builder()
                .fullName("Jockey Two")
                .email("j2-" + jockeySuffix + "@test-calculation.com")
                .passwordHash("hash")
                .phone("011112")
                .role(Role.JOCKEY)
                .status(UserStatus.ACTIVE)
                .build());
        Jockey jockey2 = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser2)
                .weight(new BigDecimal("51.0"))
                .experienceYears((short) 4)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());

        User jockeyUser3 = userRepository.save(User.builder()
                .fullName("Jockey Three")
                .email("j3-" + jockeySuffix + "@test-calculation.com")
                .passwordHash("hash")
                .phone("011113")
                .role(Role.JOCKEY)
                .status(UserStatus.ACTIVE)
                .build());
        Jockey jockey3 = jockeyRepository.save(Jockey.builder()
                .user(jockeyUser3)
                .weight(new BigDecimal("52.0"))
                .experienceYears((short) 5)
                .status("available")
                .createdAt(LocalDateTime.now())
                .build());

        // Setup Season, Racecourse, Meeting, Condition, Race
        Season season = seasonRepository.save(Season.builder()
                .seasonName("Calculation Season")
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(6))
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        Racecourse racecourse = racecourseRepository.save(Racecourse.builder()
                .racecourseName("Hanoi Racecourse")
                .location("Hanoi")
                .surfaceType("turf")
                .capacity(15000)
                .createdAt(LocalDateTime.now())
                .build());

        RaceMeeting meeting = raceMeetingRepository.save(RaceMeeting.builder()
                .season(season)
                .racecourse(racecourse)
                .meetingDate(java.time.LocalDate.now().plusDays(2))
                .status("scheduled")
                .createdAt(LocalDateTime.now())
                .build());

        RaceCondition condition = raceConditionRepository.save(RaceCondition.builder()
                .conditionName("Calculation Condition")
                .distance(1000)
                .trackType("turf")
                .minEntries((short) 3)
                .maxEntries((short) 8)
                .classRequirement("5")
                .createdAt(LocalDateTime.now())
                .build());

        testRace = raceRepository.save(Race.builder()
                .raceMeeting(meeting)
                .raceCondition(condition)
                .raceName("Official Championship")
                .raceNo((short) 1)
                .scheduledTime(LocalDateTime.now().plusDays(2))
                .registrationOpenAt(LocalDateTime.now().minusDays(1))
                .registrationCloseAt(LocalDateTime.now().plusDays(1))
                .status(RaceStatus.OFFICIAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Create Prize Structures
        prizeStructureRepository.save(PrizeStructure.builder()
                .race(testRace)
                .position((short) 1)
                .amount(new BigDecimal("50000000.00"))
                .score(new BigDecimal("50.00"))
                .build());

        prizeStructureRepository.save(PrizeStructure.builder()
                .race(testRace)
                .position((short) 2)
                .amount(new BigDecimal("30000000.00"))
                .score(new BigDecimal("30.00"))
                .build());

        // Create Entries
        RaceRegistration reg1 = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(testRace)
                .horse(horse1)
                .submittedBy(ownerUser)
                .status(RaceRegistrationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build());
        RaceEntry entry1 = raceEntryRepository.save(RaceEntry.builder()
                .race(testRace)
                .registration(reg1)
                .horse(horse1)
                .jockey(jockey1)
                .gateNumber((short) 1)
                .entryStatus("PASSED")
                .build());

        RaceRegistration reg2 = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(testRace)
                .horse(horse2)
                .submittedBy(ownerUser)
                .status(RaceRegistrationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build());
        RaceEntry entry2 = raceEntryRepository.save(RaceEntry.builder()
                .race(testRace)
                .registration(reg2)
                .horse(horse2)
                .jockey(jockey2)
                .gateNumber((short) 2)
                .entryStatus("PASSED")
                .build());

        RaceRegistration reg3 = raceRegistrationRepository.save(RaceRegistration.builder()
                .race(testRace)
                .horse(horse3)
                .submittedBy(ownerUser)
                .status(RaceRegistrationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build());
        RaceEntry entry3 = raceEntryRepository.save(RaceEntry.builder()
                .race(testRace)
                .registration(reg3)
                .horse(horse3)
                .jockey(jockey3)
                .gateNumber((short) 3)
                .entryStatus("PASSED")
                .build());

        // Create provisional Results
        result1 = raceResultRepository.save(RaceResult.builder()
                .race(testRace)
                .entry(entry1)
                .position((short) 1)
                .finishTime(LocalTime.of(1, 15, 30))
                .resultStatus(RaceResultStatus.OFFICIAL)
                .build());

        result2 = raceResultRepository.save(RaceResult.builder()
                .race(testRace)
                .entry(entry2)
                .position((short) 2)
                .finishTime(LocalTime.of(1, 16, 12))
                .resultStatus(RaceResultStatus.OFFICIAL)
                .build());

        result3 = raceResultRepository.save(RaceResult.builder()
                .race(testRace)
                .entry(entry3)
                .position((short) 3) // No prize structure for pos 3
                .finishTime(LocalTime.of(1, 17, 0))
                .resultStatus(RaceResultStatus.OFFICIAL)
                .build());
    }

    @Test
    void testRecalculatePrizesSuccess() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/admin/races/" + testRace.getRaceId() + "/recalculate-prizes")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn();

        RecalculatePrizesResponse response = objectMapper.readValue(res.getResponse().getContentAsString(), RecalculatePrizesResponse.class);
        assertEquals(testRace.getRaceId(), response.getRaceId());
        assertEquals(3, response.getProcessedResults());
        assertEquals(3, response.getUpdatedHorseCount());
        assertEquals(new BigDecimal("80000000.00"), response.getTotalPrizeAmount());
        assertEquals(new BigDecimal("80.00"), response.getTotalScoreAwarded());

        // Verify result 1
        RaceResult dbResult1 = raceResultRepository.findById(result1.getResultId()).orElseThrow();
        assertEquals(new BigDecimal("50000000.00"), dbResult1.getPrizeAmount());
        assertEquals(new BigDecimal("50.00"), dbResult1.getScoreAwarded());

        // Verify result 3 (no prize structure)
        RaceResult dbResult3 = raceResultRepository.findById(result3.getResultId()).orElseThrow();
        assertEquals(BigDecimal.ZERO, dbResult3.getPrizeAmount());
        assertEquals(BigDecimal.ZERO, dbResult3.getScoreAwarded());

        // Verify Horse 1 (Winner)
        Horse dbHorse1 = horseRepository.findById(horse1.getHorseId()).orElseThrow();
        assertEquals(new BigDecimal("50.00"), dbHorse1.getCurrentScore());
        assertEquals((short) 3, dbHorse1.getHorseClass()); // score 50 -> class 3
        assertEquals(1, dbHorse1.getTotalWins());

        // Verify Horse 2 (2nd Place)
        Horse dbHorse2 = horseRepository.findById(horse2.getHorseId()).orElseThrow();
        assertEquals(new BigDecimal("30.00"), dbHorse2.getCurrentScore());
        assertEquals((short) 4, dbHorse2.getHorseClass()); // score 30 -> class 4
        assertEquals(0, dbHorse2.getTotalWins());
    }

    @Test
    void testRecalculatePrizesIsIdempotent() throws Exception {
        // Call first time
        mockMvc.perform(post("/api/admin/races/" + testRace.getRaceId() + "/recalculate-prizes")
                .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Call second time
        MvcResult res = mockMvc.perform(post("/api/admin/races/" + testRace.getRaceId() + "/recalculate-prizes")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn();

        RecalculatePrizesResponse response = objectMapper.readValue(res.getResponse().getContentAsString(), RecalculatePrizesResponse.class);
        assertEquals(new BigDecimal("80000000.00"), response.getTotalPrizeAmount());
        assertEquals(new BigDecimal("80.00"), response.getTotalScoreAwarded());

        // Scores and wins must not double add
        Horse dbHorse1 = horseRepository.findById(horse1.getHorseId()).orElseThrow();
        assertEquals(new BigDecimal("50.00"), dbHorse1.getCurrentScore());
        assertEquals(1, dbHorse1.getTotalWins());
    }

    @Test
    void testRecalculateNonOfficialRaceFails() throws Exception {
        // Change race status to RUNNING
        testRace.setStatus(RaceStatus.RUNNING);
        raceRepository.save(testRace);

        mockMvc.perform(post("/api/admin/races/" + testRace.getRaceId() + "/recalculate-prizes")
                .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRecalculateRaceWithNoResultsFails() throws Exception {
        // Delete results
        raceResultRepository.deleteAll();

        mockMvc.perform(post("/api/admin/races/" + testRace.getRaceId() + "/recalculate-prizes")
                .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDisqualifiedResultsReceiveZero() throws Exception {
        // Set result1 as DISQUALIFIED
        result1.setResultStatus(RaceResultStatus.DISQUALIFIED);
        raceResultRepository.save(result1);

        mockMvc.perform(post("/api/admin/races/" + testRace.getRaceId() + "/recalculate-prizes")
                .header("Authorization", adminToken))
                .andExpect(status().isOk());

        RaceResult dbResult1 = raceResultRepository.findById(result1.getResultId()).orElseThrow();
        assertEquals(BigDecimal.ZERO, dbResult1.getPrizeAmount());
        assertEquals(BigDecimal.ZERO, dbResult1.getScoreAwarded());

        Horse dbHorse1 = horseRepository.findById(horse1.getHorseId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(dbHorse1.getCurrentScore()));
        assertEquals(0, dbHorse1.getTotalWins());
    }

    @Test
    void testOwnerRoleRecalculateFails() throws Exception {
        mockMvc.perform(post("/api/admin/races/" + testRace.getRaceId() + "/recalculate-prizes")
                .header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedRecalculateFails() throws Exception {
        mockMvc.perform(post("/api/admin/races/" + testRace.getRaceId() + "/recalculate-prizes"))
                .andExpect(status().isUnauthorized());
    }
}
