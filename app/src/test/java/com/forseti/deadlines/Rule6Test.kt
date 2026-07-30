package com.forseti.deadlines

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke coverage for the FRCP 6(a)/6(d) deadline engine. Verifies the
 * exclude-trigger-day count, the +3-day mail/electronic bump, and the
 * roll-forward off weekends and federal holidays.
 */
class Rule6Test {

    @Test
    fun personalService_landsOnWeekday_isUnchanged() {
        // Wed 2025-01-01 + 21 days (personal, no bump) = Wed 2025-01-22.
        val result = Rule6.computeDeadline(
            triggerDate = LocalDate(2025, 1, 1),
            days = 21,
            serviceMode = Rule6.ServiceMode.Personal
        )
        assertEquals(LocalDate(2025, 1, 22), result)
    }

    @Test
    fun mailService_addsThreeDays_andRollsOffWeekend() {
        // 2025-01-01 + 21 + 3 = Sat 2025-01-25, rolled forward to Mon 2025-01-27.
        val result = Rule6.computeDeadline(
            triggerDate = LocalDate(2025, 1, 1),
            days = 21,
            serviceMode = Rule6.ServiceMode.Mail
        )
        assertEquals(LocalDate(2025, 1, 27), result)
    }

    @Test
    fun deadlineLandingOnFederalHoliday_rollsToNextBusinessDay() {
        // Fri 2025-06-13 + 21 days = Fri 2025-07-04 (Independence Day).
        // Roll: Sat 07-05, Sun 07-06 -> Mon 2025-07-07.
        val result = Rule6.computeDeadline(
            triggerDate = LocalDate(2025, 6, 13),
            days = 21,
            serviceMode = Rule6.ServiceMode.Personal
        )
        assertEquals(LocalDate(2025, 7, 7), result)
    }

    @Test
    fun result_isNeverWeekend() {
        // Sweep a month of trigger dates; no computed deadline may be a weekend.
        for (day in 1..28) {
            val result = Rule6.computeDeadline(LocalDate(2025, 5, day), 30, Rule6.ServiceMode.Mail)
            assertTrue(
                "deadline $result fell on a weekend",
                result.dayOfWeek != DayOfWeek.SATURDAY && result.dayOfWeek != DayOfWeek.SUNDAY
            )
        }
    }
}
