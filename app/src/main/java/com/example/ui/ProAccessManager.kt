package com.example.ui

import android.content.Context

object ProAccessManager {
    private const val PREFS = "game_motion_pro"
    private const val KEY_UNLOCKED = "unlocked"
    private const val UNLOCK_CODE = "769933"

    fun isUnlocked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_UNLOCKED, false)

    fun unlock(context: Context, code: String): Boolean {
        val ok = code.trim() == UNLOCK_CODE
        if (ok) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_UNLOCKED, true).apply()
        return ok
    }
}
