package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scanner.GameScanner
import com.example.ui.AppViewModelProvider
import com.example.ui.GameViewModel
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.util.ApkGameManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ImportScreen(
    viewModel: GameViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnalyzing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var importedTitle by remember { mutableStateOf<String?>(null) }
    var detectedConsole by remember { mutableStateOf("PSP") }
    var selectedIsoConsole by remember { mutableStateOf("PS2") }
    var showIsoConsolePicker by remember { mutableStateOf(false) }
    var pendingIsoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingIsoName by remember { mutableStateOf<String?>(null) }
    var pendingIsoSize by remember { mutableStateOf(0L) }
    var pendingApk by remember { mutableStateOf<Triple<Uri, String, Long>?>(null) }
    var pendingApkInfo by remember { mutableStateOf<ApkGameManager.ApkInfo?>(null) }

    val apkInstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val pending = pendingApk
        val info = pendingApkInfo
        if (pending != null && info != null) {
            if (ApkGameManager.isInstalled(context, info.packageName)) {
                viewModel.importGame(
                    title = info.label,
                    packageUri = pending.first.toString(),
                    packageSize = pending.third,
                    unrealVersion = "Android ${info.versionName ?: "unknown"}",
                    architecture = "Android APK",
                    compatibilityStatus = "Installed and ready to launch",
                    consoleType = "ANDROID",
                    fileExtension = "APK",
                    androidPackageName = info.packageName,
                    androidVersionName = info.versionName,
                    androidVersionCode = info.versionCode
                )
                isSuccess = true
                statusMessage = "Installed ${info.label}. It is now in Game Motion Game Space and can be launched as a real Android app."
            } else {
                isSuccess = false
                statusMessage = "APK installation was cancelled or did not complete. Android did not report ${info.label} as installed."
            }
            pendingApk = null
            pendingApkInfo = null
        }
        isAnalyzing = false
    }

    fun processImport(uri: Uri, fileName: String, size: Long, forcedConsole: String? = null) {
        coroutineScope.launch {
            isAnalyzing = true
            statusMessage = null
            isSuccess = false
            importedTitle = fileName.substringBeforeLast('.')

            delay(800)

            val ext = fileName.substringAfterLast('.', "").uppercase()
            val finalConsole: String
            val formatValid: Boolean

            when {
                ext in listOf("CSO", "PBP") -> {
                    finalConsole = "PSP"
                    formatValid = true
                }
                ext in listOf("CHD", "ZSO") -> {
                    finalConsole = "PS2"
                    formatValid = true
                }
                ext in listOf("PKG") -> {
                    finalConsole = "PS3"
                    formatValid = true
                }
                ext == "ISO" -> {
                    finalConsole = forcedConsole ?: selectedIsoConsole
                    formatValid = true
                }
                ext == "PCK" -> {
                    finalConsole = "Godot"
                    formatValid = true
                }
                ext == "APK" -> {
                    finalConsole = "ANDROID"
                    formatValid = true
                }
                else -> {
                    finalConsole = "Unknown"
                    formatValid = false
                }
            }

            if (!formatValid) {
                isSuccess = false
                statusMessage = "❌ Unsupported File Extension: .$ext\n\nOnly the following console extensions are permitted:\n• PSP: .ISO, .CSO, .PBP\n• PS2: .ISO, .CHD, .ZSO\n• PS3: .ISO, .PKG, or Game Folder\n• Android: .APK (installed by Android Package Installer)
• Godot: .PCK"
            } else if (ext == "APK") {
                val info = ApkGameManager.inspectApk(context, uri)
                if (info == null) {
                    isSuccess = false
                    statusMessage = "❌ This APK could not be inspected or is not a valid Android application."
                } else if (!ApkGameManager.canInstallUnknownSources(context)) {
                    pendingApk = Triple(uri, fileName, size)
                    pendingApkInfo = info
                    isSuccess = false
                    statusMessage = "Android requires permission for Game Motion to install APKs. Open the system permission screen, allow installs, then select the APK again."
                    ApkGameManager.openUnknownSourcesSettings(context)
                } else {
                    pendingApk = Triple(uri, fileName, size)
                    pendingApkInfo = info
                    isSuccess = false
                    statusMessage = "Installing ${info.label} (${info.packageName}) using Android's official package installer…"
                    val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                        data = uri
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    }
                    apkInstallLauncher.launch(installIntent)
                }
            } else {
                detectedConsole = finalConsole
                viewModel.importGame(
                    title = importedTitle ?: "Game",
                    packageUri = uri.toString(),
                    packageSize = size,
                    unrealVersion = "Game Station Native",
                    architecture = "ARM64 / Console Emulation",
                    compatibilityStatus = "Ready for $finalConsole",
                    consoleType = finalConsole,
                    fileExtension = ext
                )
                isSuccess = true
                statusMessage = "Imported $fileName as $finalConsole game. It will only launch when a real runtime is available."
            }
            isAnalyzing = false
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "UnknownGame.iso"
            val size = getFileSize(context, it)
            val ext = fileName.substringAfterLast('.', "").uppercase()

            if (ext == "ISO") {
                pendingIsoUri = it
                pendingIsoName = fileName
                pendingIsoSize = size
                showIsoConsolePicker = true
            } else {
                processImport(it, fileName, size)
            }
        }
    }

    val scannerFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { treeUri ->
            coroutineScope.launch {
                isAnalyzing = true
                statusMessage = "Scanning directory tree for PSP, PS2, PS3, and Godot games..."
                isSuccess = true

                var addedCount = 0
                GameScanner.scanFolder(context, treeUri).collect { progress ->
                    if (progress.isComplete) {
                        for (game in progress.discoveredGames) {
                            viewModel.importGame(
                                title = game.title,
                                packageUri = game.uriString,
                                packageSize = game.fileSize,
                                unrealVersion = "Game Station Native",
                                architecture = "ARM64 Core",
                                compatibilityStatus = game.compatibilityStatus,
                                consoleType = game.platform,
                                fileExtension = game.fileFormat
                            )
                            addedCount++
                        }
                        isAnalyzing = false
                        statusMessage = "✅ Auto-Scan Complete: Discovered and registered $addedCount games into your Game Motion library!"
                    }
                }
            }
        }
    }

    if (showIsoConsolePicker) {
        AlertDialog(
            onDismissRequest = { showIsoConsolePicker = false },
            title = {
                Text(
                    text = "Select Console Target for .ISO",
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "The .ISO format is shared across PlayStation systems. Please select the target console for '${pendingIsoName}':",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PSP", "PS2", "PS3").forEach { console ->
                            val isSelected = selectedIsoConsole == console
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedIsoConsole = console },
                                label = { Text(console, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIsoConsolePicker = false
                        pendingIsoUri?.let { uri ->
                            processImport(
                                uri,
                                pendingIsoName ?: "Game.iso",
                                pendingIsoSize,
                                forcedConsole = selectedIsoConsole
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                ) {
                    Text("Confirm & Import", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIsoConsolePicker = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = SurfaceDark
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Game Discovery & Import",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
                Text(
                    text = "Scan game directories or import individual files for PSP, PS2, PS3, and Godot",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Supported Extension Reference Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Supported Console Game Formats",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormatSpecBadge("PSP", ".ISO, .CSO, .PBP", Color(0xFF00E5FF))
                    FormatSpecBadge("PS2", ".ISO, .CHD, .ZSO", Color(0xFF2979FF))
                    FormatSpecBadge("PS3", ".ISO, .PKG, Folder", Color(0xFFFF5252))
                    FormatSpecBadge("Android", ".APK", Color(0xFF43A047))
                    FormatSpecBadge("Godot", ".PCK", Color(0xFF7C4DFF))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row: File Import + Auto Scanner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.SportsEsports, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Game File", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { scannerFolderLauncher.launch(null) },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003852), contentColor = CyanPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Auto-Scan Game Folder", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // Analysis / Status Section
        if (isAnalyzing) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        statusMessage ?: "Scanning game package & cataloging titles...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            statusMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) Color(0xFF003822) else Color(0xFF4A0000)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (isSuccess) Color(0xFF00E676) else Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.FormatSpecBadge(console: String, exts: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(console, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(exts, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

private fun getFileSize(context: Context, uri: Uri): Long {
    var size = 0L
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index != -1) {
                    size = cursor.getLong(index)
                }
            }
        }
    }
    return size
}
