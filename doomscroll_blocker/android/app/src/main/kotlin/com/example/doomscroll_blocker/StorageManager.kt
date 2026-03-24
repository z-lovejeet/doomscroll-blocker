package com.example.doomscroll_blocker

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Manages all persistent state for the doomscroll blocker using SharedPreferences.
 * Handles session timers, penalty tracking, daily limits, and midnight resets.
 */
class StorageManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "doomscroll_prefs"

        // Keys
        private const val KEY_SESSION_SECONDS = "session_seconds"
        private const val KEY_PENALTIES_TODAY = "penalties_today"
        private const val KEY_IS_IN_PENALTY = "is_in_penalty"
        private const val KEY_PENALTY_START_TIME = "penalty_start_time"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_IS_DAILY_LOCKED = "is_daily_locked"
        private const val KEY_PENDING_FRICTION_UNLOCK = "pending_friction_unlock"
        private const val KEY_LAST_SCROLL_TIMESTAMP = "last_scroll_timestamp"
        private const val KEY_RAPID_SCROLL_COUNT = "rapid_scroll_count"

        // Limits
        const val WARNING_SECONDS = 25 * 60       // 25 minutes
        const val MAX_SESSION_SECONDS = 30 * 60    // 30 minutes
        const val PENALTY_DURATION_MS = 3 * 60 * 60 * 1000L  // 3 hours
        const val MAX_PENALTIES_PER_DAY = 3
        const val RAPID_SCROLL_THRESHOLD_MS = 3000L  // 3 seconds between scrolls = mindless
        const val RAPID_SCROLL_WARNING_COUNT = 20     // 20 rapid scrolls triggers warning
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        checkMidnightReset()
    }

    // --- Session Timer ---

    var sessionSeconds: Int
        get() = prefs.getInt(KEY_SESSION_SECONDS, 0)
        set(value) = prefs.edit().putInt(KEY_SESSION_SECONDS, value).apply()

    fun incrementSession(): Int {
        val current = prefs.getInt(KEY_SESSION_SECONDS, 0)
        val newVal = current + 1
        prefs.edit().putInt(KEY_SESSION_SECONDS, newVal).commit()
        return newVal
    }

    fun resetSession() {
        sessionSeconds = 0
        rapidScrollCount = 0
    }

    // --- Penalty Tracking ---

    var penaltiesToday: Int
        get() = prefs.getInt(KEY_PENALTIES_TODAY, 0)
        set(value) = prefs.edit().putInt(KEY_PENALTIES_TODAY, value).apply()

    var isInPenalty: Boolean
        get() = prefs.getBoolean(KEY_IS_IN_PENALTY, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_IN_PENALTY, value).apply()

    var penaltyStartTime: Long
        get() = prefs.getLong(KEY_PENALTY_START_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_PENALTY_START_TIME, value).apply()

    var isDailyLocked: Boolean
        get() = prefs.getBoolean(KEY_IS_DAILY_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DAILY_LOCKED, value).apply()

    var pendingFrictionUnlock: Boolean
        get() = prefs.getBoolean(KEY_PENDING_FRICTION_UNLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_PENDING_FRICTION_UNLOCK, value).apply()

    fun triggerPenalty() {
        val count = penaltiesToday + 1
        penaltiesToday = count
        isInPenalty = true
        penaltyStartTime = System.currentTimeMillis()
        resetSession()

        if (count >= MAX_PENALTIES_PER_DAY) {
            isDailyLocked = true
        }
    }

    /**
     * Check if penalty cooldown period has expired.
     * Returns true if STILL in penalty, false if expired.
     */
    fun checkPenaltyExpired(): Boolean {
        if (!isInPenalty) return false
        val elapsed = System.currentTimeMillis() - penaltyStartTime
        if (elapsed >= PENALTY_DURATION_MS) {
            isInPenalty = false
            pendingFrictionUnlock = true
            return false
        }
        return true
    }

    fun getPenaltyRemainingMs(): Long {
        if (!isInPenalty) return 0L
        val elapsed = System.currentTimeMillis() - penaltyStartTime
        return (PENALTY_DURATION_MS - elapsed).coerceAtLeast(0L)
    }

    fun completeFrictionUnlock() {
        pendingFrictionUnlock = false
    }

    // --- Rapid Scroll Detection ---

    var lastScrollTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SCROLL_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SCROLL_TIMESTAMP, value).apply()

    var rapidScrollCount: Int
        get() = prefs.getInt(KEY_RAPID_SCROLL_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_RAPID_SCROLL_COUNT, value).apply()

    /**
     * Record a scroll event and determine if user is mindlessly scrolling.
     * Returns true if rapid scroll threshold is exceeded.
     */
    fun recordScrollEvent(): Boolean {
        val now = System.currentTimeMillis()
        val lastScroll = lastScrollTimestamp
        lastScrollTimestamp = now

        if (lastScroll == 0L) return false

        val gap = now - lastScroll
        if (gap in 1..RAPID_SCROLL_THRESHOLD_MS) {
            val count = rapidScrollCount + 1
            rapidScrollCount = count
            return count >= RAPID_SCROLL_WARNING_COUNT
        } else {
            // Reset if user is watching normally
            rapidScrollCount = 0
            return false
        }
    }

    fun resetRapidScrollCount() {
        rapidScrollCount = 0
    }

    // --- Midnight Reset ---

    private fun checkMidnightReset() {
        val todayDate = getTodayDateString()
        val lastReset = prefs.getString(KEY_LAST_RESET_DATE, "") ?: ""
        if (lastReset != todayDate) {
            prefs.edit()
                .putInt(KEY_PENALTIES_TODAY, 0)
                .putBoolean(KEY_IS_DAILY_LOCKED, false)
                .putBoolean(KEY_IS_IN_PENALTY, false)
                .putBoolean(KEY_PENDING_FRICTION_UNLOCK, false)
                .putInt(KEY_SESSION_SECONDS, 0)
                .putInt(KEY_RAPID_SCROLL_COUNT, 0)
                .putString(KEY_LAST_RESET_DATE, todayDate)
                .apply()
        }
    }

    private fun getTodayDateString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    // --- Stats for Flutter UI ---

    fun getStatsMap(): Map<String, Any> {
        checkMidnightReset()
        checkPenaltyExpired()
        return mapOf(
            "sessionSeconds" to sessionSeconds,
            "penaltiesToday" to penaltiesToday,
            "isInPenalty" to isInPenalty,
            "isDailyLocked" to isDailyLocked,
            "pendingFrictionUnlock" to pendingFrictionUnlock,
            "penaltyRemainingMs" to getPenaltyRemainingMs().toInt(),
            "warningSeconds" to WARNING_SECONDS,
            "maxSessionSeconds" to MAX_SESSION_SECONDS,
            "maxPenalties" to MAX_PENALTIES_PER_DAY
        )
    }
}
