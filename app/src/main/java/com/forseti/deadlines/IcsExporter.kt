package com.forseti.deadlines

import com.forseti.data.entities.CaseEntity
import com.forseti.data.entities.DeadlineEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Minimal RFC 5545 ICS writer. Each deadline becomes a 30-minute VEVENT.
 * Output is plain UTF-8 text suitable for sharing or saving to Drive.
 *
 * RFC 5545 section 3.1 requires CRLF line endings; some calendar clients (notably
 * Apple Calendar via Mail.app) reject LF-only files, so we emit "\r\n" explicitly
 * instead of using the platform `appendLine`.
 */
object IcsExporter {
    private const val CRLF = "\r\n"

    fun export(case: CaseEntity, deadlines: List<DeadlineEntity>): String = buildString {
        appendCrlf("BEGIN:VCALENDAR")
        appendCrlf("VERSION:2.0")
        appendCrlf("PRODID:-//Forseti//Deadlines 1.0//EN")
        appendCrlf("CALSCALE:GREGORIAN")
        deadlines.forEach { d ->
            val start = utcStamp(d.dueAt)
            val end = utcStamp(d.dueAt + 30L * 60_000L)
            appendCrlf("BEGIN:VEVENT")
            appendCrlf("UID:forseti-${case.id}-${d.id}@forseti")
            appendCrlf("DTSTAMP:${utcStamp(System.currentTimeMillis())}")
            appendCrlf("DTSTART:$start")
            appendCrlf("DTEND:$end")
            appendCrlf("SUMMARY:${escape(d.title)}")
            val desc = buildString {
                append(case.title)
                if (case.caseNumber.isNotBlank()) append(" - ").append(case.caseNumber)
                d.ruleCitation?.let { append(" (").append(it).append(')') }
            }
            appendCrlf("DESCRIPTION:${escape(desc)}")
            appendCrlf("END:VEVENT")
        }
        appendCrlf("END:VCALENDAR")
    }

    private fun StringBuilder.appendCrlf(line: String) { append(line).append(CRLF) }

    private fun utcStamp(epochMillis: Long): String {
        val ldt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC)
        return "%04d%02d%02dT%02d%02d00Z".format(
            ldt.year, ldt.monthNumber, ldt.dayOfMonth,
            ldt.hour, ldt.minute
        )
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")
}
