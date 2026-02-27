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

    /** Apps actively recording audio (microphone) */
    fun getRecordingApps(): List<AudioApp> {
        return audioManager.activeRecordingConfigurations
            .mapNotNull { config -> resolveUidViaReflection(config, AudioApp.State.RECORDING) }
            .distinctBy { it.packageName }
    }

    /** Apps actively playing audio (sound output) */
    fun getPlayingApps(): List<AudioApp> {
        return audioManager.activePlaybackConfigurations
            .mapNotNull { config -> resolveUidViaReflection(config, AudioApp.State.PLAYING) }
            .distinctBy { it.packageName }
    }

    /**
     * Both AudioRecordingConfiguration and AudioPlaybackConfiguration have getClientUid()
     * marked as @hide in the Android SDK — not accessible via the public API surface.
     * Reflection is the only compile-safe approach without using system stubs.
     */
    private fun resolveUidViaReflection(config: Any, state: AudioApp.State): AudioApp? {
        val uid = runCatching {
            config.javaClass
                .getDeclaredMethod("getClientUid")
                .also { it.isAccessible = true }
                .invoke(config) as? Int
        }.getOrNull() ?: return null

        val packageName = pm.getPackagesForUid(uid)?.firstOrNull() ?: return null
        return AudioApp(
            packageName = packageName,
            appName = resolveAppName(packageName),
            state = state
        )
    }

    private fun resolveAppName(packageName: String): String =
        runCatching {
            @Suppress("DEPRECATION")
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
}
