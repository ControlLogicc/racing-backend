package com.solofounder.horseracing.service;

import com.solofounder.horseracing.util.PreRaceWeightCheck;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PreRaceWeightCheckTests {

    private static final BigDecimal HANDICAP_WEIGHT = new BigDecimal("55.00");

    @Test
    void belowHandicapRequiresBallastAndDoesNotPass() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> PreRaceWeightCheck.evaluate(new BigDecimal("54.99"), HANDICAP_WEIGHT)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Actual carried weight is below handicap weight. Add ballast and check again.",
                exception.getReason()
        );
    }

    @Test
    void exactHandicapPasses() {
        PreRaceWeightCheck.Result result =
                PreRaceWeightCheck.evaluate(new BigDecimal("55.00"), HANDICAP_WEIGHT);

        assertEquals("passed", result.weightCheckStatus());
        assertEquals("ready", result.entryStatus());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(result.overweightAmount()));
    }

    @Test
    void overweightAtTolerancePasses() {
        PreRaceWeightCheck.Result result =
                PreRaceWeightCheck.evaluate(new BigDecimal("55.91"), HANDICAP_WEIGHT);

        assertEquals("passed", result.weightCheckStatus());
        assertEquals("ready", result.entryStatus());
        assertEquals(0, new BigDecimal("0.91").compareTo(result.overweightAmount()));
    }

    @Test
    void overweightAboveToleranceFailsAndScratches() {
        PreRaceWeightCheck.Result result =
                PreRaceWeightCheck.evaluate(new BigDecimal("55.92"), HANDICAP_WEIGHT);

        assertEquals("failed", result.weightCheckStatus());
        assertEquals("scratched", result.entryStatus());
        assertEquals(0, new BigDecimal("0.92").compareTo(result.overweightAmount()));
    }

    @Test
    void missingHandicapIsRejected() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> PreRaceWeightCheck.evaluate(new BigDecimal("55.00"), null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Handicap weight has not been assigned", exception.getReason());
    }

    @Test
    void negativeActualWeightIsRejected() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> PreRaceWeightCheck.evaluate(new BigDecimal("-1.00"), HANDICAP_WEIGHT)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
