package com.personal.englishautotalk.talk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.personal.englishautotalk.R
import com.personal.englishautotalk.util.DeviceState
import com.personal.englishautotalk.util.EventLog

enum class TalkMode { NORMAL, DND }

/** 전체화면 인텐트 알림으로 잠금화면 위에 대화 화면을 띄운다. */
object TalkNotifier {
    private const val CHANNEL_ID = "talk_call"
    private const val NOTIFICATION_ID = 1
    private const val TIMEOUT_MS = 10 * 60 * 1000L

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "영어 말걸기", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "정해진 시각에 대화 화면을 띄웁니다"
            // 소리는 앱이 직접 말하고, 진동은 방해금지 모드에서만 화면이 직접 낸다.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, mode: TalkMode, test: Boolean) {
        val prefix = if (test) "[테스트] " else ""
        if (!DeviceState.canPostNotifications(context)) {
            EventLog.add(context, "${prefix}⚠ 알림 권한 없음 — 말걸기 화면을 띄울 수 없음")
            return
        }
        if (!DeviceState.canUseFullScreenIntent(context)) {
            EventLog.add(context, "${prefix}⚠ 전체화면 알림 권한 없음 — 알림만 표시됨(눌러야 시작)")
        }
        val intent = TalkActivity.intent(context, mode, test)
        val pending = PendingIntent.getActivity(
            context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("영어 말걸기 시간이에요")
            .setContentText("화면이 열리면 바로 영어로 말을 겁니다")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        EventLog.add(context, "${prefix}말걸기 화면 요청 (${if (mode == TalkMode.DND) "방해금지: 화면+진동" else "음성"})")
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}
