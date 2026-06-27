package com.solofounder.horseracing;

import tools.jackson.databind.ObjectMapper;
import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.entry.RaceEntryResponse;
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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RaceEntryRandomizationIntegrationTests {

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

    @Autowired
    private RefereeReportRepository refereeReportRepository;

    @Autowired
    private RaceResultRepository raceResultRepository;

    private String staffToken;
    private String jockeyToken;
    private String adminToken;
    private Race testRace;
    private Race emptyRace;

    @BeforeEach
    void setupData() {
        refereeReportRepository.deleteAll();
        raceResultRepository.deleteAll();
        raceEntryRepository.deleteAll();
        raceInvitationRepository.deleteAll();
        raceRegistrationRepository.deleteAll();
        raceRepository.deleteAll();

        String suffix = String.valueOf(System.nanoTime());

        // Staff
        User staffUser = userRepository.save(User.builder()
                .fullName("Staff Random")
                .email("staff-" + suffix + "@random-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("099001")
                .role(Role.STAFF)
                .status(UserStatus.ACTIVE)
                .build());
        staffToken = "Bearer " + jwtService.generateToken(staffUser);

        Staff staffProfile = staffRepository.save(Staff.builder()
                .user(staffUser)
                .staffCode("STF-" + suffix)
                .department("Operations")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        // Admin
        User adminUser = userRepository.save(User.builder()
                .fullName("Admin Random")
                .email("admin-" + suffix + "@random-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("099002")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(adminUser);

        // Jockey
        User jockeyUser = userRepository.save(User.builder()
                .fullName("Jockey Random")
                .email("jockey-" + suffix + "@random-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("099003")
                .role(Role.JOCKEY)
                .status(UserStatus.ACTIVE)
                .build());
        jockeyToken = "Bearer " + jwtService.generateToken(jockeyUser);

        // Owner
        User ownerUser = userRepository.save(User.builder()
                .fullName("Owner Random")
                .email("owner-" + suffix + "@random-test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .phone("099004")
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .build());

        // Setup Season, Racecourse, Meeting, Condition, Race
        Season season = seasonRepository.save(Season.builder()
                .seasonName("Test Season")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(6))
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        Racecourse racecourse = racecourseRepository.save(Racecourse.builder()
                .racecourseName("Test Course")
                .location("Hanoi")
                .surfaceType("turf")
                .build());

        RaceMeeting raceMeeting = raceMeetingRepository.save(RaceMeeting.builder()
                .season(season)
                .racecourse(racecourse)
                .meetingName("Test Meeting")
                .meetingDate(LocalDate.now())
                .status("scheduled")
                .createdAt(LocalDateTime.now())
                .build());

        RaceCondition condition = raceConditionRepository.save(RaceCondition.builder()
                .conditionName("Class 5 Turf")
                .distance(1200)
                .trackType("turf")
                .classRequirement("class 5")
                .minEntries((short) 5)
                .maxEntries((short) 10)
                .build());

        testRace = raceRepository.save(Race.builder()
                .raceMeeting(raceMeeting)
                .raceCondition(condition)
                .staff(staffProfile)
                .raceName("Randomization Test Race")
                .raceNo((short) 1)
                .scheduledTime(LocalDateTime.now().plusDays(1))
                .registrationOpenAt(LocalDateTime.now().minusHours(2))
                .registrationCloseAt(LocalDateTime.now().plusHours(2))
                .status(RaceStatus.SCHEDULED)
                .build());

        emptyRace = raceRepository.save(Race.builder()
                .raceMeeting(raceMeeting)
                .raceCondition(condition)
                .staff(staffProfile)
                .raceName("Empty Test Race")
                .raceNo((short) 2)
                .scheduledTime(LocalDateTime.now().plusDays(1))
                .registrationOpenAt(LocalDateTime.now().minusHours(2))
                .registrationCloseAt(LocalDateTime.now().plusHours(2))
                .status(RaceStatus.SCHEDULED)
                .build());

        // Create 3 Horses
        Horse horse1 = createHorse(ownerUser, "Horse One", (short) 5, new BigDecimal("10.00"));
        Horse horse2 = createHorse(ownerUser, "Horse Two", (short) 5, new BigDecimal("5.00"));
        Horse horse3 = createHorse(ownerUser, "Horse Three", (short) 5, new BigDecimal("0.00"));

        // Create 3 Jockeys
        Jockey j1 = createJockey(userRepository.save(User.builder().fullName("J1").email("j1-" + suffix + "@random.com").passwordHash(passwordEncoder.encode("123456")).role(Role.JOCKEY).status(UserStatus.ACTIVE).build()));
        Jockey j2 = createJockey(userRepository.save(User.builder().fullName("J2").email("j2-" + suffix + "@random.com").passwordHash(passwordEncoder.encode("123456")).role(Role.JOCKEY).status(UserStatus.ACTIVE).build()));
        Jockey j3 = createJockey(userRepository.save(User.builder().fullName("J3").email("j3-" + suffix + "@random.com").passwordHash(passwordEncoder.encode("123456")).role(Role.JOCKEY).status(UserStatus.ACTIVE).build()));

        // Create Registrations
        RaceRegistration reg1 = createRegistration(horse1);
        RaceRegistration reg2 = createRegistration(horse2);
        RaceRegistration reg3 = createRegistration(horse3);

        // Create Invitations
        RaceInvitation inv1 = createInvitation(reg1, j1);
        RaceInvitation inv2 = createInvitation(reg2, j2);
        RaceInvitation inv3 = createInvitation(reg3, j3);

        // Create Race Entries with null gate numbers
        createEntry(reg1, inv1, horse1, j1);
        createEntry(reg2, inv2, horse2, j2);
        createEntry(reg3, inv3, horse3, j3);
    }

    private Horse createHorse(User owner, String name, short horseClass, BigDecimal score) {
        return horseRepository.save(Horse.builder()
                .owner(owner)
                .horseName(name)
                .color("brown")
                .age((short) 4)
                .gender("M")
                .currentScore(score)
                .horseClass(horseClass)
                .status("active")
                .build());
    }

    private Jockey createJockey(User user) {
        return jockeyRepository.save(Jockey.builder()
                .user(user)
                .weight(new BigDecimal("52.0"))
                .experienceYears((short) 3)
                .status("available")
                .build());
    }

    private RaceRegistration createRegistration(Horse horse) {
        return raceRegistrationRepository.save(RaceRegistration.builder()
                .race(testRace)
                .horse(horse)
                .submittedBy(horse.getOwner())
                .status(RaceRegistrationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private RaceInvitation createInvitation(RaceRegistration reg, Jockey jockey) {
        return raceInvitationRepository.save(RaceInvitation.builder()
                .raceRegistration(reg)
                .jockey(jockey)
                .invitationStatus(RaceInvitationStatus.USED)
                .sentAt(LocalDateTime.now())
                .build());
    }

    private RaceEntry createEntry(RaceRegistration reg, RaceInvitation inv, Horse horse, Jockey jockey) {
        return raceEntryRepository.save(RaceEntry.builder()
                .race(testRace)
                .registration(reg)
                .invitation(inv)
                .horse(horse)
                .jockey(jockey)
                .gateNumber(null)
                .handicapWeight(new BigDecimal("50.0"))
                .entryStatus("declared")
                .build());
    }

    @Test
    void testRandomizeGatesSuccess() throws Exception {
        // Assert current gate numbers are all null
        List<RaceEntry> before = raceEntryRepository.findByRaceRaceId(testRace.getRaceId());
        assertEquals(3, before.size());
        for (RaceEntry entry : before) {
            assertNull(entry.getGateNumber());
        }

        // Call randomize gates API
        MvcResult result = mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/random-gates")
                .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andReturn();

        RaceEntryResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), RaceEntryResponse[].class);
        assertEquals(3, responses.length);

        Set<Short> gates = new HashSet<>();
        for (RaceEntryResponse r : responses) {
            assertNotNull(r.getGateNumber());
            assertTrue(r.getGateNumber() >= 1 && r.getGateNumber() <= 3);
            gates.add(r.getGateNumber());
        }
        // Unique gates
        assertEquals(3, gates.size());

        // Verify in DB
        List<RaceEntry> after = raceEntryRepository.findByRaceRaceId(testRace.getRaceId());
        assertEquals(3, after.size());
        for (RaceEntry entry : after) {
            assertNotNull(entry.getGateNumber());
            assertTrue(entry.getGateNumber() >= 1 && entry.getGateNumber() <= 3);
        }
    }

    @Test
    void testRandomizeGatesForbidden() throws Exception {
        mockMvc.perform(put("/api/entries/race/" + testRace.getRaceId() + "/random-gates")
                .header("Authorization", jockeyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRandomizeGatesNoEntriesFails() throws Exception {
        mockMvc.perform(put("/api/entries/race/" + emptyRace.getRaceId() + "/random-gates")
                .header("Authorization", staffToken))
                .andExpect(status().isBadRequest());
    }
}
