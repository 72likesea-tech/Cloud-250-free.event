package com.personal.englishautotalk.util

import android.content.Context
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** 실기기 검수용 이벤트 기록(예약 시각 vs 실제 시각 등). 홈 화면에 최근 항목을 보여준다. */
object EventLog {
    private const val MAX_LINES = 100
    private val stamp = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
    private val clock = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    @Synchronized
    fun add(context: Context, message: String) {
        val prefs = context.getSharedPreferences("event_log", Context.MODE_PRIVATE)
        val lines = listOf("${LocalDateTime.now().format(stamp)}  $message") + read(context)
        prefs.edit().putString("lines", lines.take(MAX_LINES).joinToString("\n")).apply()
    }

    fun read(context: Context): List<String> =
        context.getSharedPreferences("event_log", Context.MODE_PRIVATE)
            .getString("lines", null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun clear(context: Context) {
        context.getSharedPreferences("event_log", Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun fmt(time: ZonedDateTime): String = time.format(clock)
}
