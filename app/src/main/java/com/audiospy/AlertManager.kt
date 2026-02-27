package com.audiospy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.audiospy.model.AudioApp

class AlertManager(private val context: Context) {

    companion object {
        const val ALERT_CHANNEL_ID = "audio_alert_channel"
        private var alertNotifId = 1000
    }

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createAlertChannel()
    }

    fun sendMicAlert(app: AudioApp) {
        val tapIntent = buildTapIntent()
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("⚠️ Microphone Alert")
            .setContentText("${app.appName} started using the microphone!")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        nm.notify(alertNotifId++, notification)
    }

    fun sendPlaybackAlert(app: AudioApp) {
        val tapIntent = buildTapIntent()
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("🔊 Audio Alert")
            .setContentText("${app.appName} started playing audio in background!")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        nm.notify(alertNotifId++, notification)
    }

    private fun buildTapIntent(): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun createAlertChannel() {
        NotificationChannel(
            ALERT_CHANNEL_ID,
            "Audio Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when apps secretly use mic or audio"
            enableVibration(true)
        }.also { nm.createNotificationChannel(it) }
    }
}
