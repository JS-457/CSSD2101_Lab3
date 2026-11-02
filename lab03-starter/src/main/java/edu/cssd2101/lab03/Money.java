package edu.cssd2101.lab03;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Checked integer-cent arithmetic with explicit HALF_EVEN rounding. */
public final class Money {
    private Money() {}

    /**
     * Converts a nonnegative decimal dollar string to cents, rounding once.
     *
     * @param dollars decimal text, never a binary floating-point value
     * @return cents
     * @throws IllegalArgumentException if malformed or negative
     * @throws ArithmeticException if the result does not fit a long
     */
    public static long cents(String dollars) {
        BigDecimal amount = new BigDecimal(dollars);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("negative dollars");
        }
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }

    /**
     * Applies basis points to a price; 100 basis points equal one percent.
     *
     * @param cents nonnegative original price
     * @param basisPoints discount in the inclusive range 0..10000
     * @return rounded final price
     * @throws IllegalArgumentException for a negative price or invalid rate
     */
    public static long discounted(long cents, int basisPoints) {
        if (basisPoints < 0 || basisPoints > 10000) throw new IllegalArgumentException("Basis points must be in between 0 and 10000 (inclusive).");
        if (cents < 0L) throw new IllegalArgumentException("Cents must be nonnegative.");

        return BigDecimal.valueOf(cents).multiply(BigDecimal.valueOf(10000L - basisPoints)).divide(BigDecimal.valueOf(10000L),0, RoundingMode.HALF_EVEN).longValueExact();
    }
}
