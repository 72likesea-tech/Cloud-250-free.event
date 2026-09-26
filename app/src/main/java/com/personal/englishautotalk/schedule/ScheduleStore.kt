package com.personal.englishautotalk.schedule

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** 예약 상태. 2단계는 SharedPreferences로 충분하고, 3단계에서 대화 기록은 Room으로 옮긴다. */
class ScheduleStore(context: Context) {
    private val prefs = context.getSharedPreferences("schedule", Context.MODE_PRIVATE)

    var nextAttemptAt: ZonedDateTime?
        get() = prefs.getLong(KEY_NEXT, 0L).takeIf { it > 0 }
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
        set(value) = prefs.edit().putLong(KEY_NEXT, value?.toInstant()?.toEpochMilli() ?: 0L).apply()

    var completedDate: LocalDate?
        get() = prefs.getString(KEY_COMPLETED, null)?.let(LocalDate::parse)
        set(value) = prefs.edit().putString(KEY_COMPLETED, value?.toString()).apply()

    private companion object {
        const val KEY_NEXT = "next_attempt_at"
        const val KEY_COMPLETED = "completed_date"
    }
}
