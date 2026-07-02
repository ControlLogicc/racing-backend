package com.solofounder.horseracing;

import com.solofounder.horseracing.dto.registration.CreateRegistrationRequest;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceRegistration;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.HorseRegistrationType;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceInvitationRepository;
import com.solofounder.horseracing.repository.RaceRegistrationRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.StaffRepository;
import com.solofounder.horseracing.repository.UserRepository;
import com.solofounder.horseracing.service.RaceRegistrationService;
import com.solofounder.horseracing.service.RaceService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceRegistrationOwnerLimitTests {

    @Mock
    private RaceRegistrationRepository raceRegistrationRepository;
    @Mock
    private RaceRepository raceRepository;
    @Mock
    private HorseRepository horseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private RaceInvitationRepository raceInvitationRepository;
    @Mock
    private RaceEntryRepository raceEntryRepository;
    @Mock
    private RaceService raceService;

    @InjectMocks
    private RaceRegistrationService raceRegistrationService;

    private User owner;
    private Race race;

    @BeforeEach
    void setup() {
        owner = User.builder()
                .userId(101L)
                .email("owner-limit@example.com")
                .fullName("Owner Limit")
                .role(Role.OWNER)
                .build();
        race = Race.builder()
                .raceId(201L)
                .raceName("Owner Limit Race")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getEmail(), null)
        );
        lenient().when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        lenient().when(raceRepository.findById(race.getRaceId())).thenReturn(Optional.of(race));
        when(raceService.isOpenForRegistration(any(Race.class), any())).thenReturn(true);
    }

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sameOwnerSameRaceDifferentHorseReturns400() {
        Horse secondHorse = horse(302L, owner, "Second Horse");
        when(horseRepository.findById(secondHorse.getHorseId())).thenReturn(Optional.of(secondHorse));
        when(raceRegistrationRepository.existsByRaceRaceIdAndHorseHorseIdAndStatusIn(
                eq(race.getRaceId()), eq(secondHorse.getHorseId()), any())).thenReturn(false);
        when(raceRegistrationRepository.existsByRaceRaceIdAndSubmittedByUserIdAndStatusIn(
                eq(race.getRaceId()), eq(owner.getUserId()), any())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> raceRegistrationService.createRegistration(request(race, secondHorse))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Owner already registered a horse for this race", exception.getReason());
        verify(raceRegistrationRepository, never()).save(any());
    }

    @Test
    void sameOwnerDifferentRaceIsAllowed() {
        Race otherRace = Race.builder().raceId(202L).raceName("Other Race").build();
        Horse horse = horse(301L, owner, "First Horse");
        allowRegistration(otherRace, horse, owner);

        assertDoesNotThrow(() -> raceRegistrationService.createRegistration(request(otherRace, horse)));
        verify(raceRegistrationRepository).save(any(RaceRegistration.class));
    }

    @Test
    void differentOwnerSameRaceIsAllowed() {
        User otherOwner = User.builder()
                .userId(102L)
                .email("other-owner@example.com")
                .fullName("Other Owner")
                .role(Role.OWNER)
                .build();
        Horse otherHorse = horse(303L, otherOwner, "Other Owner Horse");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(otherOwner.getEmail(), null)
        );
        when(userRepository.findByEmail(otherOwner.getEmail())).thenReturn(Optional.of(otherOwner));
        allowRegistration(race, otherHorse, otherOwner);

        assertDoesNotThrow(() -> raceRegistrationService.createRegistration(request(race, otherHorse)));
        verify(raceRegistrationRepository).save(any(RaceRegistration.class));
    }

    @Test
    void sameHorseSameRaceDuplicateStillReturnsConflict() {
        Horse firstHorse = horse(301L, owner, "First Horse");
        when(horseRepository.findById(firstHorse.getHorseId())).thenReturn(Optional.of(firstHorse));
        when(raceRegistrationRepository.existsByRaceRaceIdAndHorseHorseIdAndStatusIn(
                eq(race.getRaceId()), eq(firstHorse.getHorseId()), any())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> raceRegistrationService.createRegistration(request(race, firstHorse))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Horse is already registered for this race", exception.getReason());
        verify(raceRegistrationRepository, never()).save(any());
    }

    private void allowRegistration(Race targetRace, Horse horse, User targetOwner) {
        when(raceRepository.findById(targetRace.getRaceId())).thenReturn(Optional.of(targetRace));
        when(horseRepository.findById(horse.getHorseId())).thenReturn(Optional.of(horse));
        when(raceRegistrationRepository.existsByRaceRaceIdAndHorseHorseIdAndStatusIn(
                eq(targetRace.getRaceId()), eq(horse.getHorseId()), any())).thenReturn(false);
        when(raceRegistrationRepository.existsByRaceRaceIdAndSubmittedByUserIdAndStatusIn(
                eq(targetRace.getRaceId()), eq(targetOwner.getUserId()), any())).thenReturn(false);
        when(raceRegistrationRepository.save(any(RaceRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Horse horse(Long horseId, User horseOwner, String name) {
        return Horse.builder()
                .horseId(horseId)
                .owner(horseOwner)
                .horseName(name)
                .status("active")
                .registrationType(HorseRegistrationType.NEW)
                .currentScore(BigDecimal.valueOf(50))
                .horseClass((short) 3)
                .ratingVerified(true)
                .build();
    }

    private CreateRegistrationRequest request(Race targetRace, Horse horse) {
        return CreateRegistrationRequest.builder()
                .raceId(targetRace.getRaceId())
                .horseId(horse.getHorseId())
                .build();
    }
}
