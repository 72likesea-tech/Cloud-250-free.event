package com.personal.englishautotalk.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personal.englishautotalk.util.EventLog
import java.time.ZonedDateTime

/** 재부팅·시간/시간대 변경·앱 업데이트·정확 알람 권한 변경 후 예약을 다시 등록한다. */
class RestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> "재부팅"
            Intent.ACTION_TIME_CHANGED -> "시간 변경"
            Intent.ACTION_TIMEZONE_CHANGED -> "시간대 변경"
            Intent.ACTION_MY_PACKAGE_REPLACED -> "앱 업데이트"
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> "정확 알람 권한 변경"
            else -> return
        }
        EventLog.add(context, "예약 복원 ($reason)")
        restore(context)
    }

    companion object {
        fun restore(context: Context, log: Boolean = true) {
            val store = ScheduleStore(context)
            val target = SchedulePolicy.restore(ZonedDateTime.now(), store.nextAttemptAt, store.completedDate)
            AlarmScheduler.scheduleDaily(context, target, log)
        }
    }
}
