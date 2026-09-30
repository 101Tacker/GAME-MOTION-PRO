package com.example.runtime

import android.content.Context
import com.example.data.GameEntity

class PS3Runtime : GameRuntime {
    override val platform = "PS3"
    override val name = "PS3 runtime integration"
    override val coreVersion = "External runtime required"
    override fun initialize(context: Context) = true
    override fun shutdown() {}
    override fun launchGame(context: Context, game: GameEntity, config: RuntimeConfig): Result<SessionInfo> =
        Result.failure(IllegalStateException("No supported official PS3 Android runtime is bundled. Game Motion will not fake a launch."))
    override fun pauseGame() {}
    override fun resumeGame() {}
    override fun stopGame() {}
    override fun saveState(slot: Int) = false
    override fun loadState(slot: Int) = false
    override fun getPerformanceInformation() = PerformanceInfo(0f, 0f, 0f, 0L, "External runtime")
    override fun getSupportedFormats() = listOf("ISO", "PKG", "Folder")
    override fun isDeviceCompatible(context: Context) = CompatibilityResult(false, "PS3 emulation is not bundled. Game Motion will not claim a PS3 runtime is installed when it is not.")
}
