package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider

/** Real Android APK installer/launcher used by Game Motion's Game Space library. */
object ApkGameManager {
    data class ApkInfo(
        val packageName: String,
        val label: String,
        val versionName: String?,
        val versionCode: Long,
        val icon: android.graphics.drawable.Drawable?
    )

    fun inspectApk(context: Context, uri: Uri): ApkInfo? {
        return runCatching {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            pfd.use { descriptor ->
                val archivePath = descriptor.fileDescriptor.toString()
                // PackageManager can parse the APK directly from a copied temp file; URI providers
                // are not guaranteed to expose a filesystem path.
            }
            val temp = java.io.File(context.cacheDir, "inspect-${System.nanoTime()}.apk")
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to read APK" }
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
            val packageInfo = context.packageManager.getPackageArchiveInfo(temp.absolutePath, flags)
                ?: throw IllegalArgumentException("The selected file is not a valid Android APK")
            val appInfo = packageInfo.applicationInfo ?: throw IllegalArgumentException("APK has no application info")
            appInfo.sourceDir = temp.absolutePath
            appInfo.publicSourceDir = temp.absolutePath
            ApkInfo(
                packageName = packageInfo.packageName,
                label = context.packageManager.getApplicationLabel(appInfo).toString(),
                versionName = packageInfo.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
                icon = context.packageManager.getApplicationIcon(appInfo)
            ).also { temp.delete() }
        }.getOrNull()
    }

    fun canInstallUnknownSources(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            })
        }
    }

    fun installApk(activity: Activity, uri: Uri, requestCode: Int = 4201) {
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun launchInstalledGame(context: Context, packageName: String): Boolean {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolveInfo = context.packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .firstOrNull() ?: return false
        launcherIntent.component = android.content.ComponentName(packageName, resolveInfo.activityInfo.name)
        context.startActivity(launcherIntent)
        return true
    }

    fun isInstalled(context: Context, packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, PackageManager.MATCH_ALL)
        true
    }.getOrDefault(false)
}
