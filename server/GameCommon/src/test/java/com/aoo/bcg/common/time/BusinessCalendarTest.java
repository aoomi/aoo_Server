package com.aoo.bcg.common.time;
import java.time.*;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class BusinessCalendarTest{
 @Test void shanghaiBusinessDayUsesConfiguredZone(){var calendar=new BusinessCalendar(BusinessZoneRegistry.require("CN"),Clock.fixed(Instant.parse("2026-08-23T16:30:00Z"),ZoneOffset.UTC));assertEquals(LocalDate.of(2026,8,24),calendar.today());assertEquals(Duration.ofHours(24),calendar.day(calendar.today()).duration());}
 @Test void dstDaysAreNotAssumedToBeTwentyFourHours(){var london=new BusinessCalendar(ZoneId.of("Europe/London"),Clock.systemUTC());assertEquals(Duration.ofHours(23),london.day(LocalDate.of(2026,3,29)).duration());assertEquals(Duration.ofHours(25),london.day(LocalDate.of(2026,10,25)).duration());}
}
