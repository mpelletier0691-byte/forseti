package com.forseti.deadlines

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.forseti.data.entities.CaseEntity
import com.forseti.data.entities.DeadlineEntity

/**
 * Opens the device calendar app with fields pre-filled (Google Calendar, Samsung, etc.).
 * Uses [Intent.ACTION_INSERT] — no calendar permissions required; user confirms save.
 */
object CalendarEventHelper {

    fun insertIntent(context: Context, case: CaseEntity, deadline: DeadlineEntity): Intent {
        val description = buildString {
            append(case.title)
            if (case.court.isNotBlank()) append("\nCourt: ").append(case.court)
            if (case.caseNumber.isNotBlank()) append("\nCase #: ").append(case.caseNumber)
            deadline.ruleCitation?.let { append("\nRule: ").append(it) }
            append("\n\nTracked in Forseti (on-device).")
        }
        val endMs = deadline.dueAt + 30L * 60_000L
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, deadline.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, deadline.dueAt)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
            putExtra(CalendarContract.Events.ALL_DAY, false)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, case.court.takeIf { it.isNotBlank() })
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { Intent.createChooser(it, "Add to calendar") }
    }

    fun launch(context: Context, case: CaseEntity, deadline: DeadlineEntity) {
        runCatching {
            context.startActivity(insertIntent(context, case, deadline))
        }
    }
}
