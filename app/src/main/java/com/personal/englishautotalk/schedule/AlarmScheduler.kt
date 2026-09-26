package com.personal.englishautotalk.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.personal.englishautotalk.ui.MainActivity
import com.personal.englishautotalk.util.EventLog
import java.time.ZonedDateTime

object AlarmScheduler {
    const val ACTION_FIRE = "com.personal.englishautotalk.FIRE"
    const val EXTRA_TEST = "test"
    const val EXTRA_SCHEDULED_AT = "scheduled_at"

    // 요청 코드가 같으면 기존 예약을 덮어쓰므로 매일 예약은 항상 하나만 존재한다.
    private const val RC_DAILY = 100
    private const val RC_TEST = 200

    /** 매일 말걸기(또는 재시도) 예약. 이전 예약을 대체한다. */
    fun scheduleDaily(context: Context, at: ZonedDateTime, log: Boolean = true) {
        val store = ScheduleStore(context)
        val changed = store.nextAttemptAt?.toInstant() != at.toInstant()
        store.nextAttemptAt = at
        setAlarm(context, at.toInstant().toEpochMilli(), test = false)
        if (log && changed) EventLog.add(context, "다음 말걸기 예약: ${EventLog.fmt(at)}")
    }

    /** 검수용 1회성 예약. 매일 일정에는 영향을 주지 않는다. */
    fun scheduleTest(context: Context, at: ZonedDateTime) {
        setAlarm(context, at.toInstant().toEpochMilli(), test = true)
        EventLog.add(context, "[테스트] 예약: ${EventLog.fmt(at)} — 화면을 끄고 기다리세요")
    }

    private fun setAlarm(context: Context, atMillis: Long, test: Boolean) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = PendingIntent.getBroadcast(
            context,
            if (test) RC_TEST else RC_DAILY,
            Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_FIRE)
                .putExtra(EXTRA_TEST, test)
                .putExtra(EXTRA_SCHEDULED_AT, atMillis),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (alarmManager.canScheduleExactAlarms()) {
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, show), operation)
        } else {
            // 권한이 없으면 정시를 보장할 수 없다. 성공으로 표시하지 않고 기록에 남긴다.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
            EventLog.add(context, "⚠ 정확 알람 권한 없음 — 말걸기가 늦어질 수 있음")
        }
    }
}
