package com.example.runtime

import android.content.Context
import com.example.data.GameEntity
import com.example.util.ApkGameManager

class GodotRuntime : GameRuntime {
    override val platform = "GODOT"
    override val name = "Godot Android launcher"
    override val coreVersion = "External exported Android build"

    override fun initialize(context: Context) = true
    override fun shutdown() {}

    override fun launchGame(context: Context, game: GameEntity, config: RuntimeConfig): Result<SessionInfo> {
        val packageName = game.androidPackageName
            ?: return Result.failure(IllegalStateException("A Godot project ZIP/PCK is not an Android executable. Export it as an APK first."))
        if (!ApkGameManager.launchInstalledGame(context, packageName))
            return Result.failure(IllegalStateException("The exported Godot Android game is not installed."))
        return Result.success(SessionInfo(game.title, platform, name, "Godot Android export"))
    }
    override fun pauseGame() {}
    override fun resumeGame() {}
    override fun stopGame() {}
    override fun saveState(slot: Int) = false
    override fun loadState(slot: Int) = false
    override fun getPerformanceInformation() = PerformanceInfo(0f, 0f, 0f, 0L, "Reported by Godot game")
    override fun getSupportedFormats() = listOf("APK")
    override fun isDeviceCompatible(context: Context) = CompatibilityResult(true, "Ready to launch an installed Godot Android export.")
}
