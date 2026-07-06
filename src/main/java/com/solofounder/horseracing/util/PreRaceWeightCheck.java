package com.solofounder.horseracing.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PreRaceWeightCheck {

    public static final BigDecimal OVERWEIGHT_TOLERANCE_KG = new BigDecimal("0.91");

    private PreRaceWeightCheck() {
    }

    public static Result evaluate(BigDecimal actualWeight, BigDecimal handicapWeight) {
        if (handicapWeight == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Handicap weight has not been assigned");
        }
        if (actualWeight == null || actualWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Actual weight must be greater than 0");
        }
        if (actualWeight.compareTo(handicapWeight) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Actual carried weight is below handicap weight. Add ballast and check again."
            );
        }

        BigDecimal overweightAmount = actualWeight.subtract(handicapWeight)
                .setScale(2, RoundingMode.HALF_UP);
        boolean passed = overweightAmount.compareTo(OVERWEIGHT_TOLERANCE_KG) <= 0;
        return new Result(
                actualWeight,
                overweightAmount,
                passed ? "passed" : "failed",
                passed ? "ready" : "scratched"
        );
    }

    public record Result(
            BigDecimal carriedWeight,
            BigDecimal overweightAmount,
            String weightCheckStatus,
            String entryStatus
    ) {
        public boolean isOverweight() {
            return overweightAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        public boolean isPassed() {
            return "passed".equals(weightCheckStatus);
        }
    }
}
