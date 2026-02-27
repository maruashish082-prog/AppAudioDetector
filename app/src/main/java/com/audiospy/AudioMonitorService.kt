package com.audiospy

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.audiospy.db.AudioLogDatabase
import com.audiospy.db.AudioLogEntity
import com.audiospy.model.AudioApp
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class AudioMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "audio_spy_channel"
        const val NOTIF_ID = 1
        const val ACTION_UPDATE = "com.audiospy.ACTION_UPDATE"
        const val EXTRA_RECORDING = "extra_recording"
        const val EXTRA_PLAYING = "extra_playing"
        private val DEDUP_WINDOW_MS = TimeUnit.SECONDS.toMillis(30)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var detector: AppAudioDetector
    private lateinit var alertManager: AlertManager
    private lateinit var audioManager: AudioManager
    private val dao by lazy { AudioLogDatabase.get(this).dao() }

    private val activeRecording = mutableSetOf<String>()
    private val activePlaying = mutableSetOf<String>()

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            scope.launch { refreshAll() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        detector = AppAudioDetector(this)
        alertManager = AlertManager(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(emptyList(), emptyList()))

        audioManager.registerAudioRecordingCallback(recordingCallback, null)
        startPlaybackPolling()

        // Purge logs older than 7 days
        scope.launch {
            dao.deleteOlderThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))
        }
    }

    private fun startPlaybackPolling() {
        scope.launch {
            while (isActive) {
                refreshAll()
                delay(2_000L)
            }
        }
    }

    private suspend fun refreshAll() {
        val recordingApps = detector.getRecordingApps()
        val playingApps = detector.getPlayingApps()

        handleNewApps(recordingApps, activeRecording, AudioApp.State.RECORDING)
        handleNewApps(playingApps, activePlaying, AudioApp.State.PLAYING)

        broadcast(recordingApps, playingApps)
        updateNotification(recordingApps, playingApps)
    }

    private suspend fun handleNewApps(
        current: List<AudioApp>,
        activeSet: MutableSet<String>,
        state: AudioApp.State
    ) {
        val currentPackages = current.map { it.packageName }.toSet()

        for (app in current) {
            if (app.packageName !in activeSet) {
                logToDb(app, isAlert = true)
                if (state == AudioApp.State.RECORDING) alertManager.sendMicAlert(app)
                else alertManager.sendPlaybackAlert(app)
            } else {
                val since = System.currentTimeMillis() - DEDUP_WINDOW_MS
                val recent = dao.recentCount(app.packageName, state.name, since)
                if (recent == 0) logToDb(app, isAlert = false)
            }
        }

        activeSet.clear()
        activeSet.addAll(currentPackages)
    }

    private suspend fun logToDb(app: AudioApp, isAlert: Boolean) {
        dao.insert(
            AudioLogEntity(
                appName = app.appName,
                packageName = app.packageName,
                state = app.state.name,
                timestamp = System.currentTimeMillis(),
                isAlert = isAlert
            )
        )
    }

    private fun broadcast(recording: List<AudioApp>, playing: List<AudioApp>) {
        sendBroadcast(Intent(ACTION_UPDATE).apply {
            putStringArrayListExtra(EXTRA_RECORDING, ArrayList(recording.map { it.appName }))
            putStringArrayListExtra(EXTRA_PLAYING, ArrayList(playing.map { it.appName }))
        })
    }

    private fun updateNotification(recording: List<AudioApp>, playing: List<AudioApp>) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(recording, playing))
    }

    private fun buildNotification(recording: List<AudioApp>, playing: List<AudioApp>): Notification {
        val micLine = if (recording.isNotEmpty()) "🎙 ${recording.joinToString { it.appName }}" else null
        val audioLine = if (playing.isNotEmpty()) "🔊 ${playing.joinToString { it.appName }}" else null
        val text = listOfNotNull(micLine, audioLine).joinToString(" | ").ifEmpty { "No apps using audio" }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Privacy Monitor Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        NotificationChannel(CHANNEL_ID, "Privacy Monitor", NotificationManager.IMPORTANCE_LOW).also {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        audioManager.unregisterAudioRecordingCallback(recordingCallback)
        scope.cancel()
        super.onDestroy()
    }
}
