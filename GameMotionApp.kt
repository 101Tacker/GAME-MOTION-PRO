package com.example.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.ApkGameManager
import kotlinx.coroutines.launch

private val Gold = Color(0xFFFFC857)
private val GoldBright = Color(0xFFFFE29A)
private val Black = Color(0xFF080808)
private val Panel = Color(0xFF15120C)

sealed class Screen(val route: String, val title: String) {
    data object Library : Screen("library", "Game Space")
    data object Import : Screen("import", "Import Games")
    data object Recent : Screen("recent", "Recent")
    data object Settings : Screen("settings", "Settings")
    data object About : Screen("about", "About Tucci Cyber Nation")
    data object Runtime : Screen("runtime/{id}", "Runtime")
}

@Composable
fun GameMotionApp(modifier: Modifier = Modifier, onToggleFullscreen: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(ProAccessManager.isUnlocked(context)) }
    var setupComplete by remember { mutableStateOf(SetupManager(context).isComplete()) }

    if (!unlocked) {
        ProLockScreen(onUnlocked = { unlocked = true })
        return
    }
    if (!setupComplete) {
        SetupWizard(onComplete = { setupComplete = true })
        return
    }
    MainAppContent(modifier)
}

@Composable
private fun ProLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val whatsapp = "https://wa.me/256778371779?text=Game%20Motion%20Pro%20Code%20Unlock"

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF201707), Black)))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(44.dp))
            Icon(Icons.Default.SportsEsports, null, tint = Gold, modifier = Modifier.size(78.dp))
            Text("GAME MOTION", color = GoldBright, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("PRO EDITION", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.height(26.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PRO ACCESS REQUIRED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Enter your six-digit Game Motion Pro Code to unlock the real setup and Game Space.", color = Color.LightGray, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(value = code, onValueChange = { code = it.filter(Char::isDigit).take(6); error = false }, label = { Text("Pro Code") }, singleLine = true, isError = error)
                    if (error) Text("Invalid code.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { if (ProAccessManager.unlock(context, code)) onUnlocked() else error = true }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Black), modifier = Modifier.fillMaxWidth()) { Text("UNLOCK GAME MOTION PRO", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsapp))) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Message, null); Spacer(Modifier.width(8.dp)); Text("GET CODE ON WHATSAPP")
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("WhatsApp quick message: Game Motion Pro Code Unlock", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("TUCCI CYBER NATION", color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("A Game Motion project focused on transparent, real runtime integration and a clean Android game library.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun SetupWizard(onComplete: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val manager = remember { SetupManager(context) }
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Internet connection is required for first-launch setup.") }
    var pspReady by remember { mutableStateOf(ApkGameManager.isInstalled(context, "org.ppsspp.ppsspp")) }
    var selectedTab by remember { mutableStateOf(0) }

    fun installPpsspp(file: java.io.File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        ApkGameManager.installApk(activity ?: return, uri)
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C1608), Black)))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text("GAME MOTION PRO", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("First-launch setup center", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("This setup prepares the Game Space and connects only to official or user-approved runtime sources. Nothing is reported as installed until Android confirms it.", color = Color.LightGray)
            Spacer(Modifier.height(18.dp))

            SetupCard("1", "Internet & device check", if (manager.hasInternet()) "READY — validated Internet connection" else "WAITING — connect to the Internet")
            SetupCard("2", "PSP / PPSSPP", if (pspReady) "READY — PPSSPP is installed" else "DOWNLOAD — official PPSSPP Android package")
            SetupCard("3", "PS2", "READY FOR INTEGRATION — no fake runtime; external supported emulator required")
            SetupCard("4", "PS3", "READY FOR INTEGRATION — no fake runtime; Android runtime is not bundled")
            SetupCard("5", "Android Game Space", "READY — APK installer and launcher enabled")
            SetupCard("6", "Godot project launcher", "READY — imports project ZIP metadata and launches exported Android builds")
            Spacer(Modifier.height(12.dp))

            if (manager.hasInternet() && !pspReady) {
                Button(enabled = !running, onClick = {
                    running = true
                    message = "Downloading PPSSPP from the official PPSSPP host…"
                    scope.launch {
                        val result = manager.downloadPpssppOfficial()
                        result.onSuccess { file ->
                            message = "Download verified. Android will now ask you to approve PPSSPP installation."
                            installPpsspp(file)
                        }.onFailure { message = it.message ?: "PPSSPP download failed" }
                        running = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Black), modifier = Modifier.fillMaxWidth()) {
                    Text(if (running) "PREPARING…" else "INSTALL OFFICIAL PPSSPP")
                }
            }
            if (!manager.hasInternet()) {
                OutlinedButton(onClick = { message = if (manager.hasInternet()) "Internet is ready." else "Still offline. Connect to Wi-Fi or mobile data and try again." }, modifier = Modifier.fillMaxWidth()) { Text("CHECK INTERNET AGAIN") }
            }
            Spacer(Modifier.height(12.dp))
            Text(message, color = GoldBright, modifier = Modifier.padding(4.dp))
            Spacer(Modifier.height(18.dp))

            Text("SETUP MODULES", color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Gold) {
                listOf("PSP", "PS2", "PS3", "ANDROID", "GODOT").forEachIndexed { i, title -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) }) }
            }
            when (selectedTab) {
                0 -> DetailText("PSP", "Game Motion uses the official PPSSPP Android application. Your games remain your files; Game Motion only manages setup and launching.")
                1 -> DetailText("PS2", "PS2 support is deliberately transparent. Game Motion does not download a questionable or discontinued runtime and does not simulate a successful launch. A compatible Android emulator must be installed separately.")
                2 -> DetailText("PS3", "PS3 support is integration-only in this release. There is no bundled Android PS3 runtime. The app will never claim that an emulator is installed when it is not.")
                3 -> DetailText("Android Game Space", "Import APKs through Android's package installer, confirm installation, then Game Space launches the installed app's real launcher activity.")
                4 -> DetailText("Godot", "Game Motion can catalog Godot project ZIPs containing project.godot. For Android execution, use a legitimately exported APK; a source project ZIP is not itself an Android executable.")
            }
            Spacer(Modifier.height(20.dp))
            Button(enabled = manager.hasInternet() && (pspReady || ApkGameManager.isInstalled(context, "org.ppsspp.ppsspp")), onClick = { manager.markComplete(); onComplete() }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Black), modifier = Modifier.fillMaxWidth()) {
                Text("FINISH SETUP & OPEN GAME SPACE", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text("Tucci Cyber Nation • Game Motion Pro • transparent runtime status", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SetupCard(number: String, title: String, status: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Gold, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Text(number, color = Black, fontWeight = FontWeight.Black) } }
            Spacer(Modifier.width(14.dp)); Column { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(status, color = Color.LightGray, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun DetailText(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(title, color = GoldBright, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(text, color = Color.LightGray) }
}

@Composable
private fun MainAppContent(modifier: Modifier = Modifier) {
    val navController = androidx.navigation.compose.rememberNavController()
    val navEntry by navController.currentBackStackEntryAsState()
    val route = navEntry?.destination?.route
    val items = listOf(Screen.Library, Screen.Import, Screen.Recent, Screen.Settings, Screen.About)
    Row(modifier.fillMaxSize().background(Black)) {
        NavigationRail(containerColor = Color(0xFF0D0B08)) {
            items.forEach { item ->
                NavigationRailItem(selected = route == item.route, onClick = { navController.navigate(item.route) { launchSingleTop = true } }, icon = { Icon(when(item) { Screen.Library -> Icons.Default.SportsEsports; Screen.Import -> Icons.Default.AddCircle; Screen.Recent -> Icons.Default.History; Screen.Settings -> Icons.Default.Settings; else -> Icons.Default.Info }, null) }, label = { Text(item.title.take(10)) }, colors = NavigationRailItemDefaults.colors(selectedIconColor = Gold, selectedTextColor = Gold, indicatorColor = Gold.copy(alpha = .14f), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
            }
        }
        NavHost(navController, startDestination = Screen.Library.route, modifier = Modifier.fillMaxSize()) {
            androidx.navigation.compose.composable(Screen.Library.route) { LibraryScreen(onPlayGame = { id -> navController.navigate("runtime/$id") }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) }, onNavigateToImport = { navController.navigate(Screen.Import.route) }) }
            androidx.navigation.compose.composable(Screen.Import.route) { ImportScreen() }
            androidx.navigation.compose.composable(Screen.Recent.route) { RecentScreen(onPlayGame = { id -> navController.navigate("runtime/$id") }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) }) }
            androidx.navigation.compose.composable(Screen.Settings.route) { SettingsScreen() }
            androidx.navigation.compose.composable(Screen.About.route) { ProAboutScreen() }
            androidx.navigation.compose.composable(Screen.Runtime.route) { backStack ->
                RuntimeScreen(gameId = backStack.arguments?.getString("id")?.toLongOrNull() ?: -1L, onExit = { navController.popBackStack() }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) })
            }
        }
    }
}

@Composable
private fun ProAboutScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("ABOUT GAME MOTION", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Tucci Cyber Nation", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        listOf(
            "What Game Motion is" to "A launcher and game-space layer for Android games and legitimate runtime integrations.",
            "How setup works" to "First launch verifies Internet access, prepares supported runtime components, and records setup state locally.",
            "Android Game Space" to "APK games are installed through Android's package installer and launched using their real launcher activity.",
            "Runtime honesty" to "Game Motion never reports an emulator as installed or a game as running when Android has not actually started it.",
            "PSP" to "Uses the official PPSSPP Android application rather than a fabricated PSP renderer.",
            "PS2 / PS3" to "Integration is exposed without bundling unsupported or questionable Android binaries.",
            "Godot" to "Godot source projects can be catalogued; Android execution requires an exported Android build.",
            "Creator" to "Tucci Cyber Nation — building practical gaming utilities with transparent setup and runtime status."
        ).forEach { (h, b) -> Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Column(Modifier.padding(16.dp)) { Text(h, color = GoldBright, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(b, color = Color.LightGray) } } }
        Spacer(Modifier.height(20.dp)); Text("Game Motion Pro", color = Gold, fontWeight = FontWeight.Bold); Text("Version 1.0 • Android 15 ready • Designed for Game Space workflows", color = Color.Gray)
    }
}
