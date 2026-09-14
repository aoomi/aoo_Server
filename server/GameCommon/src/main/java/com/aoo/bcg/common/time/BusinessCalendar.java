package com.aoo.bcg.common.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/** Explicit business-zone calendar for activity, accounting and tournament boundaries. */
public final class BusinessCalendar {
    public record DayWindow(Instant startsAt, Instant endsAt) {
        public Duration duration(){return Duration.between(startsAt,endsAt);}
    }
    private final ZoneId zone; private final Clock clock;
    public BusinessCalendar(ZoneId zone,Clock clock){this.zone=Objects.requireNonNull(zone);this.clock=Objects.requireNonNull(clock);}
    public LocalDate today(){return LocalDate.now(clock.withZone(zone));}
    public DayWindow day(LocalDate date){Instant start=date.atStartOfDay(zone).toInstant();Instant end=date.plusDays(1).atStartOfDay(zone).toInstant();return new DayWindow(start,end);}
    public ZoneId zone(){return zone;}
}
