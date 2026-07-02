package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.report.CreateRefereeReportRequest;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.RaceResult;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.RefereeReport;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RaceResultRepository;
import com.solofounder.horseracing.repository.RefereeReportRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
import com.solofounder.horseracing.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefereeReportDecisionTests {

    @Mock
    private RefereeReportRepository refereeReportRepository;
    @Mock
    private RaceRepository raceRepository;
    @Mock
    private RefereeRepository refereeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RaceEntryRepository raceEntryRepository;
    @Mock
    private RaceResultRepository raceResultRepository;
    @Mock
    private PrizeCalculationService prizeCalculationService;

    @InjectMocks
    private RefereeReportService refereeReportService;

    private User refereeUser;
    private Referee referee;
    private Race race;
    private RaceEntry entry;

    @BeforeEach
    void setup() {
        refereeUser = User.builder()
                .userId(10L)
                .email("report-referee@example.com")
                .fullName("Report Referee")
                .role(Role.REFEREE)
                .build();
        referee = Referee.builder()
                .refereeId(20L)
                .user(refereeUser)
                .status(RefereeStatus.ACTIVE)
                .build();
        race = Race.builder()
                .raceId(30L)
                .raceName("Report Race")
                .referee(referee)
                .build();
        entry = entry(40L, race, "First Horse", "First Jockey");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(refereeUser.getEmail(), null)
        );
        lenient().when(userRepository.findByEmail(refereeUser.getEmail()))
                .thenReturn(Optional.of(refereeUser));
        lenient().when(refereeRepository.findByUserUserId(refereeUser.getUserId()))
                .thenReturn(Optional.of(referee));
        lenient().when(raceRepository.findById(race.getRaceId())).thenReturn(Optional.of(race));
        lenient().when(raceEntryRepository.findByIdWithDetails(entry.getEntryId()))
                .thenReturn(Optional.of(entry));
        lenient().when(refereeReportRepository.save(any(RefereeReport.class)))
                .thenAnswer(invocation -> {
                    RefereeReport report = invocation.getArgument(0);
                    report.setReportId(50L);
                    return report;
                });
    }

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void warningOnlySavesReportAndDoesNotChangeStandings() {
        CreateRefereeReportRequest request = request("warning", entry.getEntryId(), race.getRaceId());

        assertDoesNotThrow(() -> refereeReportService.createReport(request));

        assertEquals("declared", entry.getEntryStatus());
        verify(refereeReportRepository).save(any(RefereeReport.class));
        verify(raceEntryRepository, never()).save(any(RaceEntry.class));
        verify(raceResultRepository, never()).findByRaceRaceId(any());
    }

    @Test
    void disqualifiedWithResultsUpdatesEntryAndPromotesFollowingResult() {
        RaceEntry secondEntry = entry(41L, race, "Second Horse", "Second Jockey");
        RaceResult first = result(60L, entry, race, (short) 1);
        RaceResult second = result(61L, secondEntry, race, (short) 2);
        when(raceResultRepository.findByRaceRaceId(race.getRaceId()))
                .thenReturn(List.of(first, second));

        refereeReportService.createReport(request("disqualified", entry.getEntryId(), race.getRaceId()));

        assertEquals("disqualified", entry.getEntryStatus());
        assertEquals(RaceResultStatus.DISQUALIFIED, first.getResultStatus());
        assertEquals((short) 2, first.getPosition());
        assertEquals((short) 1, second.getPosition());
        assertEquals(BigDecimal.ZERO, first.getPrizeAmount());
        assertEquals(BigDecimal.ZERO, first.getScoreAwarded());
    }

    @Test
    void disqualifiedWithoutResultDoesNotThrow500() {
        when(raceResultRepository.findByRaceRaceId(race.getRaceId())).thenReturn(List.of());

        assertDoesNotThrow(() ->
                refereeReportService.createReport(request("disqualified", entry.getEntryId(), race.getRaceId())));

        assertEquals("disqualified", entry.getEntryStatus());
        verify(prizeCalculationService, never()).recalculateAfterStandingsChange(any());
    }

    @Test
    void wrongRefereeReturns403() {
        Referee otherReferee = Referee.builder().refereeId(99L).build();
        race.setReferee(otherReferee);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refereeReportService.createReport(request("warning", entry.getEntryId(), race.getRaceId()))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void entryOutsideRaceReturns400() {
        Race otherRace = Race.builder().raceId(31L).build();
        entry.setRace(otherRace);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refereeReportService.createReport(request("warning", entry.getEntryId(), race.getRaceId()))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Race entry does not belong to this race", exception.getReason());
    }

    @Test
    void invalidDecisionReturns400() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refereeReportService.createReport(
                        request("Không hợp lệ", entry.getEntryId(), race.getRaceId()))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Decision must be warning, no_action, disqualified, dnf, or scratched",
                exception.getReason()
        );
        verify(refereeReportRepository, never()).save(any(RefereeReport.class));
    }

    private CreateRefereeReportRequest request(String decision, Long entryId, Long raceId) {
        return CreateRefereeReportRequest.builder()
                .raceId(raceId)
                .entryId(entryId)
                .reportType("VIOLATION")
                .description("Decision test")
                .decision(decision)
                .build();
    }

    private RaceEntry entry(Long id, Race targetRace, String horseName, String jockeyName) {
        User jockeyUser = User.builder().userId(id + 100).fullName(jockeyName).build();
        return RaceEntry.builder()
                .entryId(id)
                .race(targetRace)
                .horse(Horse.builder().horseId(id + 200).horseName(horseName).build())
                .jockey(Jockey.builder().jockeyId(id + 300).user(jockeyUser).build())
                .entryStatus("declared")
                .build();
    }

    private RaceResult result(Long id, RaceEntry targetEntry, Race targetRace, Short position) {
        return RaceResult.builder()
                .resultId(id)
                .entry(targetEntry)
                .race(targetRace)
                .position(position)
                .resultStatus(RaceResultStatus.OFFICIAL)
                .prizeAmount(BigDecimal.TEN)
                .scoreAwarded(BigDecimal.ONE)
                .build();
    }
}
