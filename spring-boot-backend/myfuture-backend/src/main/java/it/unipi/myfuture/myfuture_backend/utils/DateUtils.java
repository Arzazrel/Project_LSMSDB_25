package it.unipi.myfuture.myfuture_backend.utils;

import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class DateUtils {

    // Private constructor to prevent instantiation
    private DateUtils() {}

    /**
     * Method that, given a time window, calculates the date obtained by subtracting the time window from the current date.
     *
     * @param window time window
     * @return start date
     */
    public static Instant calculateStartDate(TimeWindow window) {
        if (window == null) {
            return Instant.now().minus(365, ChronoUnit.DAYS);               // Default one year
        }

        return switch (window) {
            case DAY -> Instant.now().minus(1, ChronoUnit.DAYS);            // one day before
            case WEEK -> Instant.now().minus(7, ChronoUnit.DAYS);           // one week before
            case MONTH -> Instant.now().minus(30, ChronoUnit.DAYS);         // one month before
            case YEAR -> Instant.now().minus(365, ChronoUnit.DAYS);         // one year before
            case TWO_YEAR -> Instant.now().minus(365*2, ChronoUnit.DAYS);   // two years before
            case FIVE_YEAR -> Instant.now().minus(365*5, ChronoUnit.DAYS);  // five years before
            default -> Instant.now().minus(3650, ChronoUnit.DAYS);          // ten years
        };
    }
}