package com.example.doomscroll_blocker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

/**
 * Main Activity serving as the Flutter entry point.
 * Sets up MethodChannel and EventChannel for Flutter <-> Native communication.
 */
class MainActivity : FlutterActivity() {

    companion object {
        private const val METHOD_CHANNEL = "com.example.doomscroll/enforcer"
        private const val EVENT_CHANNEL = "com.example.doomscroll/timer_events"
    }

    private lateinit var storage: StorageManager
    private var eventSink: EventChannel.EventSink? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        storage = StorageManager(applicationContext)

        // --- Method Channel ---
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getServiceStatus" -> {
                        val isRunning = DoomscrollAccessibilityService.instance != null
                        result.success(isRunning)
                    }

                    "getOverlayPermissionStatus" -> {
                        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(this)
                        } else {
                            true
                        }
                        result.success(hasPermission)
                    }

                    "getStats" -> {
                        storage = StorageManager(applicationContext) // refresh
                        result.success(storage.getStatsMap())
                    }

                    "unlockPenalty" -> {
                        val typedText = call.argument<String>("text") ?: ""
                        val requiredText = "I am opening Reels intentionally and I will close it when my time is up"
                        if (typedText.trim().equals(requiredText, ignoreCase = true)) {
                            storage.completeFrictionUnlock()
                            storage.resetSession()
                            result.success(true)
                        } else {
                            result.success(false)
                        }
                    }

                    "openAccessibilitySettings" -> {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        result.success(true)
                    }

                    "openOverlaySettings" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        result.success(true)
                    }

                    else -> result.notImplemented()
                }
            }

        // --- Event Channel for live timer updates ---
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSink = events
                    DoomscrollAccessibilityService.instance?.onTimerTick = { seconds ->
                        runOnUiThread {
                            eventSink?.success(seconds)
                        }
                    }
                }

                override fun onCancel(arguments: Any?) {
                    eventSink = null
                    DoomscrollAccessibilityService.instance?.onTimerTick = null
                }
            })
    }
}
