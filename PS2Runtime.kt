package com.example.runtime

import android.content.Context
import com.example.data.GameEntity

class PS2Runtime : GameRuntime {
    override val platform = "PS2"
    override val name = "PS2 runtime integration"
    override val coreVersion = "External runtime required"
    override fun initialize(context: Context) = true
    override fun shutdown() {}
    override fun launchGame(context: Context, game: GameEntity, config: RuntimeConfig): Result<SessionInfo> =
        Result.failure(IllegalStateException("No supported official PS2 Android runtime is bundled. Game Motion will not fake a launch."))
    override fun pauseGame() {}
    override fun resumeGame() {}
    override fun stopGame() {}
    override fun saveState(slot: Int) = false
    override fun loadState(slot: Int) = false
    override fun getPerformanceInformation() = PerformanceInfo(0f, 0f, 0f, 0L, "External runtime")
    override fun getSupportedFormats() = listOf("ISO", "CHD", "ZSO")
    override fun isDeviceCompatible(context: Context) = CompatibilityResult(false, "PS2 requires a separately supported Android emulator/runtime; none is bundled by Game Motion.")
}
