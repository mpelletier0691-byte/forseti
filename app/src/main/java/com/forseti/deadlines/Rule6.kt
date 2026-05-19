package com.forseti.deadlines

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod

/**
 * Implementation of FRCP 6(a) day-counting and federal holiday rules.
 *
 * 6(a)(1) periods stated in days:
 *  (A) exclude the day of the event that triggers the period
 *  (B) count every day, including weekends and legal holidays
 *  (C) include the last day, but if it lands on a weekend or holiday,
 *      continue to the next day that is not a weekend or holiday.
 *
 * 6(d) "+3 days when service was by mail or electronic means" is applied
 * by [computeDeadline] when [serviceMode] is one of the +3-day modes.
 */
object Rule6 {
    enum class ServiceMode {
        Personal,           // No +3
        Mail,               // +3
        Electronic,         // +3 (FRCP 5(b)(2)(E) + 6(d))
        FilingByCourt       // No +3
    }

    fun computeDeadline(
        triggerDate: LocalDate,
        days: Int,
        serviceMode: ServiceMode = ServiceMode.Personal
    ): LocalDate {
        val effectiveDays = days + if (serviceMode == ServiceMode.Mail || serviceMode == ServiceMode.Electronic) 3 else 0
        // (A) exclude trigger day
        var d = triggerDate.plus(DatePeriod(days = effectiveDays))
        // (C) roll forward off weekends + federal holidays
        while (isWeekend(d) || isFederalHoliday(d)) {
            d = d.plus(DatePeriod(days = 1))
        }
        return d
    }

    private fun isWeekend(d: LocalDate): Boolean =
        d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY

    private fun isFederalHoliday(d: LocalDate): Boolean = federalHolidaysFor(d.year).contains(d)

    private fun federalHolidaysFor(year: Int): Set<LocalDate> = buildSet {
        // Fixed-date holidays, observed on Friday if Sat or Monday if Sun.
        addObserved(this, LocalDate(year, Month.JANUARY, 1))            // New Year
        addObserved(this, LocalDate(year, Month.JUNE, 19))              // Juneteenth
        addObserved(this, LocalDate(year, Month.JULY, 4))               // Independence Day
        addObserved(this, LocalDate(year, Month.NOVEMBER, 11))          // Veterans Day
        addObserved(this, LocalDate(year, Month.DECEMBER, 25))          // Christmas
        // Floating-date holidays
        add(nthWeekday(year, Month.JANUARY, DayOfWeek.MONDAY, 3))       // MLK Day
        add(nthWeekday(year, Month.FEBRUARY, DayOfWeek.MONDAY, 3))      // Washington's Birthday
        add(lastWeekday(year, Month.MAY, DayOfWeek.MONDAY))             // Memorial Day
        add(nthWeekday(year, Month.SEPTEMBER, DayOfWeek.MONDAY, 1))     // Labor Day
        add(nthWeekday(year, Month.OCTOBER, DayOfWeek.MONDAY, 2))       // Columbus Day
        add(nthWeekday(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4))    // Thanksgiving
    }

    private fun addObserved(set: MutableSet<LocalDate>, d: LocalDate) {
        when (d.dayOfWeek) {
            DayOfWeek.SATURDAY -> set.add(d.plus(DatePeriod(days = -1)))
            DayOfWeek.SUNDAY -> set.add(d.plus(DatePeriod(days = 1)))
            else -> set.add(d)
        }
    }

    private fun nthWeekday(year: Int, month: Month, day: DayOfWeek, n: Int): LocalDate {
        var d = LocalDate(year, month, 1)
        var seen = 0
        while (true) {
            if (d.dayOfWeek == day) {
                seen++
                if (seen == n) return d
            }
            d = d.plus(DatePeriod(days = 1))
        }
    }

    private fun lastWeekday(year: Int, month: Month, day: DayOfWeek): LocalDate {
        var d = LocalDate(year, month, 1).plus(DatePeriod(months = 1)).plus(DatePeriod(days = -1))
        while (d.dayOfWeek != day) d = d.plus(DatePeriod(days = -1))
        return d
    }
}

/**
 * Built-in catalog of common FRCP timing rules. The deadline tracker uses these
 * to suggest add-this-now buttons after the user enters a trigger event.
 */
data class TimingRule(
    val id: String,
    val title: String,
    val rule: String,
    val days: Int,
    val defaultServiceMode: Rule6.ServiceMode = Rule6.ServiceMode.Personal,
    val hint: String
)

object TimingRules {
    val all: List<TimingRule> = listOf(
        TimingRule("rule_4m_service", "Serve all defendants", "FRCP 4(m)", 90,
            Rule6.ServiceMode.FilingByCourt,
            "Counted from the date the complaint was filed."),
        TimingRule("rule_12a_answer", "File answer (after personal service)", "FRCP 12(a)(1)(A)(i)", 21,
            Rule6.ServiceMode.Personal,
            "Counted from the date you were personally served."),
        TimingRule("rule_12a_answer_waiver", "File answer (waiver of service)", "FRCP 12(a)(1)(A)(ii)", 60,
            Rule6.ServiceMode.Mail,
            "Counted from the date the waiver request was sent."),
        TimingRule("rule_12a_us", "U.S. answer", "FRCP 12(a)(2)", 60,
            Rule6.ServiceMode.Mail,
            "When the United States is a defendant."),
        TimingRule("rule_12_motion_response", "Respond to a Rule 12 motion", "Local rule (typical)", 14,
            Rule6.ServiceMode.Electronic,
            "Most districts give 14 or 21 days. Check your local rule."),
        TimingRule("rule_26a_initial_disclosures", "Initial disclosures", "FRCP 26(a)(1)(C)", 14,
            Rule6.ServiceMode.FilingByCourt,
            "After the Rule 26(f) conference."),
        TimingRule("rule_33_response", "Answer interrogatories", "FRCP 33(b)(2)", 30,
            Rule6.ServiceMode.Mail,
            "From service of the interrogatories."),
        TimingRule("rule_34_response", "Respond to RFPs", "FRCP 34(b)(2)(A)", 30,
            Rule6.ServiceMode.Mail,
            "From service of the requests."),
        TimingRule("rule_36_response", "Respond to RFAs", "FRCP 36(a)(3)", 30,
            Rule6.ServiceMode.Mail,
            "Failure to respond = ADMITTED."),
        TimingRule("rule_56_msj", "Move for summary judgment", "FRCP 56(b)", 30,
            Rule6.ServiceMode.FilingByCourt,
            "Within 30 days after the close of all discovery (default)."),
        TimingRule("rule_59_new_trial", "Move for new trial / alter judgment", "FRCP 59(b)/(e)", 28,
            Rule6.ServiceMode.FilingByCourt,
            "Counted from entry of judgment. JURISDICTIONAL - cannot be extended."),
        TimingRule("rule_60_b1_3", "Rule 60(b)(1)-(3) motion", "FRCP 60(c)(1)", 365,
            Rule6.ServiceMode.FilingByCourt,
            "1 year maximum for grounds 1-3.")
    )
}
