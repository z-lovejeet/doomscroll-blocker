package com.example.doomscroll_blocker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Draws a full-screen dark overlay warning over the user's screen.
 * Used to deliver intrusive visual warnings that break the doomscrolling trance.
 */
class OverlayService : Service() {

    companion object {
        const val EXTRA_MESSAGE = "overlay_message"
        const val EXTRA_DURATION_MS = "overlay_duration_ms"
        const val EXTRA_TYPE = "overlay_type"

        const val TYPE_WARNING = "warning"
        const val TYPE_RAPID_SCROLL = "rapid_scroll"

        fun show(context: Context, message: String, durationMs: Long = 5000L, type: String = TYPE_WARNING) {
            // Check overlay permission before attempting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Log.w("OverlayService", "Overlay permission not granted, skipping overlay")
                return
            }
            try {
                val intent = Intent(context, OverlayService::class.java).apply {
                    putExtra(EXTRA_MESSAGE, message)
                    putExtra(EXTRA_DURATION_MS, durationMs)
                    putExtra(EXTRA_TYPE, type)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("OverlayService", "Failed to start overlay service", e)
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Time's up!"
        val durationMs = intent?.getLongExtra(EXTRA_DURATION_MS, 5000L) ?: 5000L
        val type = intent?.getStringExtra(EXTRA_TYPE) ?: TYPE_WARNING

        showOverlay(message, durationMs, type)
        return START_NOT_STICKY
    }

    private fun showOverlay(message: String, durationMs: Long, type: String) {
        removeOverlay()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)

            val bgColor = when (type) {
                TYPE_RAPID_SCROLL -> Color.argb(230, 180, 60, 0)   // Orange-red
                else -> Color.argb(230, 20, 20, 20)                // Dark
            }
            setBackgroundColor(bgColor)
        }

        val iconText = TextView(this).apply {
            text = when (type) {
                TYPE_RAPID_SCROLL -> "⚡"
                else -> "⏰"
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 64f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val msgText = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            gravity = Gravity.CENTER
            setLineSpacing(8f, 1.2f)
        }

        val subText = TextView(this).apply {
            text = when (type) {
                TYPE_RAPID_SCROLL -> "Slow down. Watch intentionally."
                else -> "Put the phone down."
            }
            setTextColor(Color.argb(180, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        layout.addView(iconText)
        layout.addView(msgText)
        layout.addView(subText)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(layout, params)
        overlayView = layout

        handler.postDelayed({ removeOverlay(); stopSelf() }, durationMs)
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}
