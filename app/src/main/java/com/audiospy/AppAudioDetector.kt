package com.audiospy

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import com.audiospy.model.AudioApp

class AppAudioDetector(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pm = context.packageManager

    /** Apps actively recording audio (microphone) */
    fun getRecordingApps(): List<AudioApp> {
        return audioManager.activeRecordingConfigurations
            .mapNotNull { config -> resolveFromRecordConfig(config) }
            .distinctBy { it.packageName }
    }

    /** Apps actively playing audio (sound output) */
    fun getPlayingApps(): List<AudioApp> {
        return audioManager.activePlaybackConfigurations
            .mapNotNull { config ->
                val uid = config.clientUid
                val packageName = pm.getPackagesForUid(uid)?.firstOrNull() ?: return@mapNotNull null
                AudioApp(
                    packageName = packageName,
                    appName = resolveAppName(packageName),
                    state = AudioApp.State.PLAYING
                )
            }
            .distinctBy { it.packageName }
    }

    private fun resolveFromRecordConfig(config: AudioRecordingConfiguration): AudioApp? {
        val uid = config.clientUid
        val packageName = pm.getPackagesForUid(uid)?.firstOrNull() ?: return null
        return AudioApp(
            packageName = packageName,
            appName = resolveAppName(packageName),
            state = AudioApp.State.RECORDING
        )
    }

    private fun resolveAppName(packageName: String): String =
        runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
}
