package com.audiospy.model

data class AudioApp(
    val packageName: String,
    val appName: String,
    val state: State,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class State { RECORDING, PLAYING }
}
