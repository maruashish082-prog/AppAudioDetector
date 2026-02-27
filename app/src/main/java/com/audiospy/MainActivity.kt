package com.audiospy

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.audiospy.databinding.ActivityMainBinding
import com.audiospy.db.AudioLogDatabase
import com.audiospy.ui.HistoryAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val historyAdapter = HistoryAdapter()

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startMonitorService()
            else Toast.makeText(this, "Notification permission required", Toast.LENGTH_LONG).show()
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val recording = intent?.getStringArrayListExtra(AudioMonitorService.EXTRA_RECORDING) ?: return
            val playing = intent.getStringArrayListExtra(AudioMonitorService.EXTRA_PLAYING) ?: return
            binding.tvRecording.text = "🎙 Mic: ${recording.joinToString().ifEmpty { "None" }}"
            binding.tvPlaying.text = "🔊 Audio: ${playing.joinToString().ifEmpty { "None" }}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // Observe Room DB live
        val dao = AudioLogDatabase.get(this).dao()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.observeAll().collect { logs ->
                    historyAdapter.submitList(logs)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) startMonitorService()
        else requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startMonitorService() {
        startForegroundService(Intent(this, AudioMonitorService::class.java))
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            receiver,
            IntentFilter(AudioMonitorService.ACTION_UPDATE),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}
