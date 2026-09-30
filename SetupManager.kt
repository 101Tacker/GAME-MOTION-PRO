package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class SetupManager(private val context: Context) {
    data class Step(val id: String, val title: String, val detail: String, val status: Status)
    enum class Status { WAITING, RUNNING, READY, NEEDS_USER, FAILED }

    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("game_motion_setup", Context.MODE_PRIVATE)

    fun hasInternet(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun isComplete() = prefs.getBoolean("complete", false)
    fun markComplete() = prefs.edit().putBoolean("complete", true).apply()

    suspend fun downloadPpssppOfficial(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://www.ppsspp.org/files/1_20_4/PPSSPP.apk"
            val out = File(context.filesDir, "runtimes/PPSSPP-1.20.4.apk")
            out.parentFile?.mkdirs()
            if (out.exists() && out.length() > 1_000_000) return@runCatching out
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Official PPSSPP download failed: HTTP ${response.code}" }
                val body = response.body ?: error("Empty PPSSPP download")
                FileOutputStream(out).use { output -> body.byteStream().use { input -> input.copyTo(output) } }
            }
            check(out.length() > 1_000_000) { "Downloaded PPSSPP package is incomplete" }
            out
        }
    }
}
