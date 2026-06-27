package com.solofounder.horseracing;

import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.report.CreateRefereeReportRequest;
import com.solofounder.horseracing.dto.report.RefereeReportResponse;
import com.solofounder.horseracing.dto.report.UpdateRefereeReportRequest;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RefereeReportIntegrationTests {

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
    private RefereeReportRepository refereeReportRepository;

    private String adminToken;
    private String staffToken;
    private String ownerToken;
    private String jockeyToken;
    private String refereeToken;
    private String otherRefereeToken;
    private Referee assignedReferee;
    private Referee otherReferee;
    private Race assignedRace;

    @BeforeEach
    void setupData() {
        refereeReportRepository.deleteAll();

        String suffix = String.valueOf(System.nanoTime());

        User admin = createUser("rr-admin-" + suffix + "@example.com", "Report Admin", Role.ADMIN);
        User staffUser = createUser("rr-staff-" + suffix + "@example.com", "Report Staff", Role.STAFF);
        User owner = createUser("rr-owner-" + suffix + "@example.com", "Report Owner", Role.OWNER);
        User jockey = createUser("rr-jockey-" + suffix + "@example.com", "Report Jockey", Role.JOCKEY);
        User refereeUser = createUser("rr-referee-" + suffix + "@example.com", "Report Referee", Role.REFEREE);
        User otherRefereeUser = createUser("rr-other-referee-" + suffix + "@example.com", "Other Referee", Role.REFEREE);

        Staff staff = staffRepository.save(Staff.builder()
                .user(staffUser)
                .staffCode("RR-STF-" + suffix)
                .department("Operations")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build());

        assignedReferee = refereeRepository.save(Referee.builder()
                .user(refereeUser)
                .licenseNo("RR-LIC-" + suffix)
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        otherReferee = refereeRepository.save(Referee.builder()
                .user(otherRefereeUser)
                .licenseNo("RR-OLIC-" + suffix)
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        assignedRace = createRace(staff, assignedReferee, "Referee Report Race " + suffix);

        adminToken = token(admin);
        staffToken = token(staffUser);
        ownerToken = token(owner);
        jockeyToken = token(jockey);
        refereeToken = token(refereeUser);
        otherRefereeToken = token(otherRefereeUser);
    }

    @Test
    void refereeSubmitReportSuccess() throws Exception {
        RefereeReportResponse response = createReport(refereeToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(content -> objectMapper.readValue(content, RefereeReportResponse.class));

        assertNotNull(response.getReportId());
        assertEquals(assignedRace.getRaceId(), response.getRaceId());
        assertEquals(assignedReferee.getRefereeId(), response.getRefereeId());
        assertEquals("PRE_RACE", response.getReportType());
    }

    @Test
    void noTokenSubmitReturns401() throws Exception {
        CreateRefereeReportRequest request = validCreateRequest(assignedRace.getRaceId(), "PRE_RACE");

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerJockeyStaffSubmitReturns403() throws Exception {
        createReport(ownerToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isForbidden());
        createReport(jockeyToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isForbidden());
        createReport(staffToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isForbidden());
    }

    @Test
    void refereeSubmitRaceNotAssignedReturns403() throws Exception {
        createReport(otherRefereeToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isForbidden());
    }

    @Test
    void raceIdNotFoundReturns404() throws Exception {
        createReport(refereeToken, 999999999L, "PRE_RACE")
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidReportTypeReturns400() throws Exception {
        createReport(refereeToken, assignedRace.getRaceId(), "BAD_TYPE")
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminGetAllReportsReturns200() throws Exception {
        createReport(refereeToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void nonAdminGetAllReportsReturns403() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", refereeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReportsByRaceByAdminAndAssignedRefereeReturns200() throws Exception {
        createReport(refereeToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/" + assignedRace.getRaceId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/" + assignedRace.getRaceId())
                        .header("Authorization", refereeToken))
                .andExpect(status().isOk());
    }

    @Test
    void getReportsByRaceWrongRefereeReturns403() throws Exception {
        createReport(refereeToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/" + assignedRace.getRaceId())
                        .header("Authorization", otherRefereeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void putOwnReportReturns200() throws Exception {
        RefereeReportResponse created = readReport(createReport(refereeToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isOk())
                .andReturn());

        UpdateRefereeReportRequest request = UpdateRefereeReportRequest.builder()
                .reportType("DECISION")
                .content("Final decision recorded")
                .violations("")
                .decisions("result_confirmed")
                .build();

        MvcResult result = mockMvc.perform(put("/api/reports/" + created.getReportId())
                        .header("Authorization", refereeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        RefereeReportResponse response = readReport(result);
        assertEquals(created.getReportId(), response.getReportId());
        assertEquals(assignedRace.getRaceId(), response.getRaceId());
        assertEquals(assignedReferee.getRefereeId(), response.getRefereeId());
        assertEquals("DECISION", response.getReportType());
    }

    @Test
    void putReportNotFoundReturns404() throws Exception {
        UpdateRefereeReportRequest request = UpdateRefereeReportRequest.builder()
                .reportType("DECISION")
                .content("Missing report update")
                .decisions("result_confirmed")
                .build();

        mockMvc.perform(put("/api/reports/999999999")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void putOtherRefereeReportReturns403() throws Exception {
        RefereeReportResponse created = readReport(createReport(refereeToken, assignedRace.getRaceId(), "PRE_RACE")
                .andExpect(status().isOk())
                .andReturn());

        UpdateRefereeReportRequest request = UpdateRefereeReportRequest.builder()
                .reportType("DECISION")
                .content("Other referee update")
                .decisions("result_confirmed")
                .build();

        mockMvc.perform(put("/api/reports/" + created.getReportId())
                        .header("Authorization", otherRefereeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions createReport(String token, Long raceId, String reportType) throws Exception {
        CreateRefereeReportRequest request = validCreateRequest(raceId, reportType);
        return mockMvc.perform(post("/api/reports")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private CreateRefereeReportRequest validCreateRequest(Long raceId, String reportType) {
        return CreateRefereeReportRequest.builder()
                .raceId(raceId)
                .reportType(reportType)
                .content("Pre-race check completed")
                .violations("")
                .decisions("no_issue")
                .build();
    }

    private RefereeReportResponse readReport(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), RefereeReportResponse.class);
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
                .status(RaceStatus.SCHEDULED)
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
