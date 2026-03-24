package com.example.doomscroll_blocker

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Core monitoring engine. Listens ONLY to Instagram events (configured in XML).
 * Detects the Reels tab, tracks time, enforces limits, and triggers penalties.
 */
class DoomscrollAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DoomscrollService"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"

        // Known content descriptions / text for the Reels tab in Instagram
        private val REELS_INDICATORS = listOf(
            "reels", "reel", "short videos"
        )

        var instance: DoomscrollAccessibilityService? = null
            private set
    }

    private lateinit var storage: StorageManager
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var isOnReelsTab = false
    private var isTimerRunning = false
    private var warningShown = false

    // Listener for streaming timer to Flutter
    var onTimerTick: ((Int) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        storage = StorageManager(applicationContext)
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != INSTAGRAM_PACKAGE) return

        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    checkIfOnReels(event)
                }
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                    if (isOnReelsTab) {
                        handleScrollEvent()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    /**
     * Traverses the UI tree to determine if the Reels tab is active.
     */
    private fun checkIfOnReels(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return

        // Check if daily locked — block immediately
        if (storage.isDailyLocked) {
            if (detectReelsInTree(rootNode)) {
                Log.d(TAG, "Daily locked. Force closing.")
                forceCloseInstagram()
            }
            rootNode.recycle()
            return
        }

        // Check if in penalty and penalty hasn't expired
        if (storage.checkPenaltyExpired()) {
            if (detectReelsInTree(rootNode)) {
                Log.d(TAG, "In penalty box. Force closing.")
                forceCloseInstagram()
            }
            rootNode.recycle()
            return
        }

        // Check if friction unlock is pending
        if (storage.pendingFrictionUnlock) {
            if (detectReelsInTree(rootNode)) {
                Log.d(TAG, "Friction unlock pending. Force closing.")
                forceCloseInstagram()
            }
            rootNode.recycle()
            return
        }

        val wasOnReels = isOnReelsTab
        isOnReelsTab = detectReelsInTree(rootNode)
        rootNode.recycle()

        if (isOnReelsTab && !isTimerRunning) {
            startSessionTimer()
        } else if (!isOnReelsTab && isTimerRunning) {
            pauseSessionTimer()
        }
    }

    /**
     * Deeply searches the accessibility node tree for Reels-related indicators.
     */
    private fun detectReelsInTree(node: AccessibilityNodeInfo): Boolean {
        // Check content description for the active tab
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        for (indicator in REELS_INDICATORS) {
            if (contentDesc.contains(indicator)) {
                // If the tab is selected, we are on the Reels page
                if (node.isSelected || isNodePartOfSelectedTab(node)) {
                    return true
                }
            }
        }

        // Search through the node by looking ONLY for actual active video player IDs
        // Avoid simply matching "reels" because that matches the unselected bottom navbar icon!
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("clips_viewer_pager") || 
            viewId.contains("reels_viewer") ||
            viewId.contains("clips_video_container") || 
            (viewId.contains("reel") && viewId.contains("viewer"))) {
            return true
        }

        // Recurse children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (detectReelsInTree(child)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    private fun isNodePartOfSelectedTab(node: AccessibilityNodeInfo): Boolean {
        var parent = node.parent
        while (parent != null) {
            if (parent.isSelected) {
                parent.recycle()
                return true
            }
            val grandParent = parent.parent
            parent.recycle()
            parent = grandParent
        }
        return false
    }

    // --- Session Timer ---

    private fun startSessionTimer() {
        if (isTimerRunning) return
        isTimerRunning = true
        warningShown = storage.sessionSeconds >= StorageManager.WARNING_SECONDS
        Log.d(TAG, "Timer started. Current session: ${storage.sessionSeconds}s")

        timerRunnable = object : Runnable {
            override fun run() {
                if (!isTimerRunning) return

                val seconds = storage.incrementSession()
                onTimerTick?.invoke(seconds)

                // State: WARNING at 25 minutes
                if (seconds == StorageManager.WARNING_SECONDS && !warningShown) {
                    warningShown = true
                    showWarningOverlay()
                }

                // State: KICK at 30 minutes
                if (seconds >= StorageManager.MAX_SESSION_SECONDS) {
                    Log.d(TAG, "Session limit reached. Triggering penalty.")
                    storage.triggerPenalty()
                    pauseSessionTimer()
                    forceCloseInstagram()
                    return
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun pauseSessionTimer() {
        isTimerRunning = false
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        Log.d(TAG, "Timer paused at ${storage.sessionSeconds}s")
    }

    // --- Scroll Velocity Detection ---

    private fun handleScrollEvent() {
        val isMindless = storage.recordScrollEvent()
        if (isMindless) {
            Log.d(TAG, "Mindless scrolling detected!")
            showRapidScrollWarning()
            storage.resetRapidScrollCount()
        }
    }

    // --- Actions ---

    private fun forceCloseInstagram() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun showWarningOverlay() {
        OverlayService.show(
            applicationContext,
            "⏰ 5 MINUTES REMAINING.\nBREATHE.",
            durationMs = 5000L,
            type = OverlayService.TYPE_WARNING
        )
    }

    private fun showRapidScrollWarning() {
        OverlayService.show(
            applicationContext,
            "⚡ MINDLESS SCROLLING\nDETECTED",
            durationMs = 4000L,
            type = OverlayService.TYPE_RAPID_SCROLL
        )
    }

    override fun onInterrupt() {
        pauseSessionTimer()
        instance = null
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        pauseSessionTimer()
        instance = null
        super.onDestroy()
    }
}
