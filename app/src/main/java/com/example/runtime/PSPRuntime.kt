package com.example.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.data.GameEntity

class PSPRuntime : GameRuntime {
    override val platform = "PSP"
    override val name = "PPSSPP (official Android app)"
    override val coreVersion = "1.20.4"
    private var running = false

    override fun initialize(context: Context): Boolean = true
    override fun shutdown() { running = false }

    override fun launchGame(context: Context, game: GameEntity, config: RuntimeConfig): Result<SessionInfo> {
        if (game.fileExtension.uppercase() !in getSupportedFormats())
            return Result.failure(IllegalArgumentException("Unsupported PSP format"))
        val packageName = "org.ppsspp.ppsspp"
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return Result.failure(IllegalStateException("PPSSPP is not installed. Complete PSP setup first."))
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launch.data = Uri.parse(game.filePath)
        context.startActivity(launch)
        running = true
        return Result.success(SessionInfo(game.title, platform, "$name $coreVersion", "PPSSPP HLE"))
    }
    override fun pauseGame() {}
    override fun resumeGame() {}
    override fun stopGame() { running = false }
    override fun saveState(slot: Int) = false
    override fun loadState(slot: Int) = false
    override fun getPerformanceInformation() = PerformanceInfo(0f, 0f, 0f, 0L, "Reported by PPSSPP")
    override fun getSupportedFormats() = listOf("ISO", "CSO", "PBP", "CHD")
    override fun isDeviceCompatible(context: Context): CompatibilityResult {
        val supported = Build.SUPPORTED_ABIS.any { it.contains("arm64") || it.contains("x86_64") }
        return CompatibilityResult(supported, if (supported) "Ready for the official PPSSPP Android runtime." else "A 64-bit Android ABI is required.")
    }
}
