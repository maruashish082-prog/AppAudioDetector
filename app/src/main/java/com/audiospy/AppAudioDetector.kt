package com.audiospy

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecordingConfiguration
import com.audiospy.model.AudioApp

class AppAudioDetector(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pm = context.packageManager

    /** Apps actively recording audio (microphone)
     *  AudioRecordingConfiguration.getClientUid() is public API since API 24 */
    fun getRecordingApps(): List<AudioApp> {
        return audioManager.activeRecordingConfigurations
            .mapNotNull { config -> resolveFromRecordConfig(config) }
            .distinctBy { it.packageName }
    }

    /** Apps actively playing audio (sound output)
     *  AudioPlaybackConfiguration has no public getClientUid() — use reflection */
    fun getPlayingApps(): List<AudioApp> {
        return audioManager.activePlaybackConfigurations
            .mapNotNull { config -> resolveFromPlaybackConfig(config) }
            .distinctBy { it.packageName }
    }

    private fun resolveFromRecordConfig(config: AudioRecordingConfiguration): AudioApp? {
        // getClientUid() is a public method on AudioRecordingConfiguration (API 24+)
        val uid = config.clientUid
        val packageName = pm.getPackagesForUid(uid)?.firstOrNull() ?: return null
        return AudioApp(
            packageName = packageName,
            appName = resolveAppName(packageName),
            state = AudioApp.State.RECORDING
        )
    }

    private fun resolveFromPlaybackConfig(config: AudioPlaybackConfiguration): AudioApp? {
        // AudioPlaybackConfiguration.getClientUid() is @hide — must use reflection
        val uid = runCatching {
            config.javaClass
                .getDeclaredMethod("getClientUid")
                .also { it.isAccessible = true }
                .invoke(config) as Int
        }.getOrNull() ?: return null

        val packageName = pm.getPackagesForUid(uid)?.firstOrNull() ?: return null
        return AudioApp(
            packageName = packageName,
            appName = resolveAppName(packageName),
            state = AudioApp.State.PLAYING
        )
    }

    private fun resolveAppName(packageName: String): String =
        runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
}
