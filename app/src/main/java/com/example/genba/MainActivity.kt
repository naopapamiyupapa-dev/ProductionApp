// Version 1.1.1 - Dashboard Interaction Fix & Version Update
package com.example.genba

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// --- カラー定義 ---
val ColorPrimary = Color(0xFF007AFF)
val ColorBg = Color(0xFF000000)
val ColorCard = Color(0xFF1C1C1E)
val ColorText = Color(0xFFFFFFFF)
val ColorAccent = Color(0xFF32D74B)
val ColorWarn = Color(0xFFFF9F0A)
val ColorDanger = Color(0xFFFF453A)
val ColorLabel = Color(0xFFF2F2F7)
val ColorBorder = Color(0xFF48484A)
val ColorInputBg = Color(0xFF2C2C2E)
val ColorInputFocus = Color(0xFF3A3A3C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GenbaToolTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun GenbaToolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = ColorPrimary, onPrimary = ColorText, background = ColorBg, surface = ColorCard, onSurface = ColorText, secondary = ColorAccent),
        content = content
    )
}

fun Modifier.cyberNeonBorder(
    isError: Boolean = false,
    isFocused: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) = this.composed {
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "alpha"
    )

    this.drawBehind {
        val glowColor = when {
            isError -> ColorDanger.copy(alpha = alpha)
            isFocused -> ColorPrimary.copy(alpha = alpha + 0.2f)
            else -> ColorPrimary.copy(alpha = alpha * 0.5f)
        }
        drawRoundRect(
            color = glowColor,
            size = size,
            cornerRadius = CornerRadius(shape.topStart.toPx(size, this)),
            style = Stroke(width = if(isFocused) 8.dp.toPx() else 6.dp.toPx())
        )
    }.border(
        width = if(isFocused) 2.dp else 1.5.dp,
        brush = when {
            isError -> SolidColor(ColorDanger)
            isFocused -> Brush.linearGradient(listOf(ColorPrimary, ColorAccent, ColorPrimary))
            else -> Brush.linearGradient(listOf(ColorPrimary, ColorAccent))
        },
        shape = shape
    )
}

data class RobotPoint(val x: Double, val y: Double, val z: Double, val w: Double, val p: Double, val r: Double)
data class MasterCoord(var name: String = "", var x: String = "0", var y: String = "0", var z: String = "0", var w: String = "0", var p: String = "0", var r: String = "0")
fun MasterCoord.toRobotPoint() = RobotPoint(x = x.toDoubleOrNull() ?: 0.0, y = y.toDoubleOrNull() ?: 0.0, z = z.toDoubleOrNull() ?: 0.0, w = w.toDoubleOrNull() ?: 0.0, p = p.toDoubleOrNull() ?: 0.0, r = r.toDoubleOrNull() ?: 0.0)
data class ActionData(val id: String = UUID.randomUUID().toString(), var name: String = "", var delay: String = "0", var duration: String = "1.0")
data class LapRecord(val timestamp: Long, val lapTime: Double, val isAboveTarget: Boolean)

class GenbaViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val gson = Gson()
    private val PREF_NAME = "GenbaToolPrefs_V110"

    var masterUF by mutableStateOf(List(10) { MasterCoord("UF$it") })
    var masterTF by mutableStateOf(List(10) { MasterCoord("TF$it") })
    var mechPulsePerRot by mutableStateOf("131072")
    var mechGearRatioN by mutableStateOf("10")
    var mechLead by mutableStateOf("10")
    var mechInputPulses by mutableStateOf("131072")
    var mechInputMm by mutableStateOf("1.0")
    var targetTact by mutableStateOf("30.0")
    var cycleHistory by mutableStateOf(listOf<LapRecord>())

    init { loadFromPrefs() }

    private fun loadFromPrefs() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val ufJson = prefs.getString("masterUF", null)
        val tfJson = prefs.getString("masterTF", null)
        val histJson = prefs.getString("cycleHistory", null)
        val type = object : TypeToken<List<MasterCoord>>() {}.type
        val histType = object : TypeToken<List<LapRecord>>() {}.type
        if (ufJson != null) try { masterUF = gson.fromJson(ufJson, type) } catch(e: Exception) {}
        if (tfJson != null) try { masterTF = gson.fromJson(tfJson, type) } catch(e: Exception) {}
        if (histJson != null) try { cycleHistory = gson.fromJson(histJson, histType) } catch(e: Exception) {}
        mechPulsePerRot = prefs.getString("mechPulsePerRot", "131072") ?: "131072"
        mechGearRatioN = prefs.getString("mechGearRatioN", "10") ?: "10"
        mechLead = prefs.getString("mechLead", "10") ?: "10"
        targetTact = prefs.getString("targetTact", "30.0") ?: "30.0"
    }

    fun saveUF(newList: List<MasterCoord>) { masterUF = newList; context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString("masterUF", gson.toJson(masterUF)).apply() }
    fun saveTF(newList: List<MasterCoord>) { masterTF = newList; context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString("masterTF", gson.toJson(masterTF)).apply() }
    fun saveMechSettings() { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString("mechPulsePerRot", mechPulsePerRot).putString("mechGearRatioN", mechGearRatioN).putString("mechLead", mechLead).apply() }
    fun addCycleRecord(lap: Double) { val target = targetTact.toDoubleOrNull() ?: 30.0; val record = LapRecord(System.currentTimeMillis(), lap, lap > target); cycleHistory = (listOf(record) + cycleHistory).take(10); context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString("cycleHistory", gson.toJson(cycleHistory)).apply() }
    fun clearCycleHistory() { cycleHistory = emptyList(); context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().remove("cycleHistory").apply() }
    fun saveTargetTact(v: String) { targetTact = v; context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString("targetTact", v).apply() }
}

@Composable
fun MainScreen(viewModel: GenbaViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf("home") }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = currentTab != "home") {
        currentTab = "home"
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = ColorCard, tonalElevation = 8.dp) {
                TabItem("ホーム", Icons.Default.Home, "home", currentTab) { focusManager.clearFocus(); currentTab = it }
                TabItem("計測", Icons.Default.Timer, "cycle", currentTab) { focusManager.clearFocus(); currentTab = it }
                TabItem("座標", Icons.Default.Place, "coord", currentTab) { focusManager.clearFocus(); currentTab = it }
                TabItem("マスタ", Icons.Default.Storage, "master", currentTab) { focusManager.clearFocus(); currentTab = it }
            }
        },
        containerColor = ColorBg
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState == "home") { (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut()) }
                    else { (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut()) }
                }, label = "pageTransition"
            ) { targetTab ->
                when (targetTab) {
                    "home" -> DashboardPage { focusManager.clearFocus(); currentTab = it }
                    "cycle" -> CycleTimePage(viewModel) { currentTab = "home" }
                    "coord" -> CoordPage(viewModel.masterUF, viewModel.masterTF) { currentTab = "home" }
                    "master" -> MasterPage(viewModel.masterUF, viewModel.masterTF, onUpdateUF = { viewModel.saveUF(it) }, onUpdateTF = { viewModel.saveTF(it) }) { currentTab = "home" }
                    "prod" -> ProductivityPage { currentTab = "home" }
                    "plan" -> PlanPage { currentTab = "home" }
                    "mech" -> MechanicalPage(viewModel) { currentTab = "home" }
                    "time" -> TimeConverterPage { currentTab = "home" }
                    "chart" -> TimeChartPage { currentTab = "home" }
                }
            }
        }
    }
}

@Composable
fun RowScope.TabItem(label: String, icon: ImageVector, id: String, current: String, onClick: (String) -> Unit) {
    NavigationBarItem(
        selected = current == id,
        onClick = { onClick(id) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 10.sp, fontWeight = if(current==id) FontWeight.ExtraBold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = ColorPrimary, selectedTextColor = ColorPrimary, unselectedIconColor = Color(0xFF8E8E93), unselectedTextColor = Color(0xFF8E8E93), indicatorColor = Color.Transparent)
    )
}

@Composable
fun DashboardPage(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("現場管理ツール EX", color = ColorAccent, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 4.dp))
        Text("Ver 1.1.1 Maintenance Update", color = ColorLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        DashboardSection("生産管理", listOf(DashTileData("生産性", "効率・OEE算出", Icons.Default.Assessment, "prod"), DashTileData("計画", "出来高シミュレーション", Icons.Default.EditCalendar, "plan"), DashTileData("計測", "サイクルタイム計測", Icons.Default.Timer, "cycle", isNew = true)), onNavigate)
        DashboardSection("技術計算", listOf(DashTileData("メカ", "パルス/距離変換", Icons.Default.SettingsInputComponent, "mech"), DashTileData("座標", "分割・逃げ計算", Icons.Default.Grid4x4, "coord"), DashTileData("時間", "単位一括変換", Icons.Default.Update, "time")), onNavigate)
        DashboardSection("分析・設定", listOf(DashTileData("チャート", "動作可視化", Icons.Default.Timeline, "chart"), DashTileData("マスタ", "座標保存スロット", Icons.Default.Storage, "master")), onNavigate)
        Spacer(Modifier.height(40.dp))
    }
}

data class DashTileData(val title: String, val sub: String, val icon: ImageVector, val route: String, val isNew: Boolean = false)

@Composable
fun DashboardSection(title: String, tiles: List<DashTileData>, onNavigate: (String) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(title, color = ColorWarn, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        tiles.chunked(2).forEach { rowTiles ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTiles.forEach { tile -> DashboardTile(tile, Modifier.weight(1f)) { onNavigate(tile.route) } }
                if (rowTiles.size < 2) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * ダッシュボードタイルの修正：クリック判定を最前面に配置し、視覚フィードバックを追加
 */
@Composable
fun DashboardTile(data: DashTileData, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .height(100.dp)
            .cyberNeonBorder(isFocused = isPressed)
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCard)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = ColorPrimary),
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(data.icon, null, tint = ColorPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(data.title, color = ColorText, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(4.dp))
            Text(data.sub, color = ColorLabel.copy(alpha = 0.7f), fontSize = 10.sp, lineHeight = 12.sp)
        }
        if (data.isNew) {
            Box(Modifier.align(Alignment.TopEnd).background(ColorDanger, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)) {
                Text("NEW", color = ColorText, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = ColorPrimary.copy(alpha = 0.3f), modifier = Modifier.size(16.dp).align(Alignment.BottomEnd))
    }
}

@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.cyberNeonBorder(shape = RoundedCornerShape(50)).size(40.dp)) { Icon(Icons.Default.Home, "Home", tint = ColorAccent) }
        Text(text = title, color = ColorAccent, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
fun CycleTimePage(viewModel: GenbaViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isRunning by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(0L) }
    var currentTime by remember { mutableStateOf(0L) }
    LaunchedEffect(isRunning) { while (isRunning) { currentTime = System.currentTimeMillis() - startTime; delay(10) } }
    val displayTime = if (isRunning) currentTime / 1000.0 else 0.0
    val target = viewModel.targetTact.toDoubleOrNull() ?: 30.0
    val isOver = displayTime > target
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PageHeader("サイクルタイム計測", onBack)
        InputGrid("目標タクトタイム (s)", null, viewModel.targetTact, labelColor = ColorWarn) { viewModel.saveTargetTact(it) }
        Box(Modifier.fillMaxWidth().height(180.dp).cyberNeonBorder(isError = isOver).background(ColorCard, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = String.format("%.2f", displayTime), fontSize = 64.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = if (isOver) ColorDanger else ColorPrimary)
                Text("SECONDS", color = ColorLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!isRunning) { startTime = System.currentTimeMillis(); isRunning = true } else { isRunning = false; viewModel.addCycleRecord(currentTime / 1000.0) } }, modifier = Modifier.weight(1f).height(80.dp).cyberNeonBorder(shape = RoundedCornerShape(16.dp)), colors = ButtonDefaults.buttonColors(containerColor = if(isRunning) ColorDanger else ColorAccent), shape = RoundedCornerShape(16.dp)) { Text(if(isRunning) "STOP / LAP" else "START", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if(isRunning) ColorText else ColorBg) }
            if (!isRunning && displayTime == 0.0) { Button(onClick = { viewModel.clearCycleHistory() }, modifier = Modifier.width(80.dp).height(80.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.DeleteSweep, "Clear", tint = ColorText) } }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("計測履歴 (最新10件)", color = ColorAccent, fontSize = 14.sp, fontWeight = FontWeight.Black)
            TextButton(onClick = { shareCycleHistoryAsCsv(context, viewModel.cycleHistory) }) { Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("CSV共有", fontSize = 12.sp) }
        }
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(viewModel.cycleHistory) { record -> Row(Modifier.fillMaxWidth().background(ColorCard, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp)); Text(date, color = ColorLabel, fontSize = 10.sp); Text(if(record.isAboveTarget) "NG: 目標超過" else "OK: 目標内", color = if(record.isAboveTarget) ColorDanger else ColorAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Text(String.format("%.2f s", record.lapTime), color = if(record.isAboveTarget) ColorDanger else ColorText, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) } } }
    }
}

fun shareCycleHistoryAsCsv(context: Context, history: List<LapRecord>) {
    if (history.isEmpty()) return
    val csv = "Time,LapTime(s),Status\n" + history.joinToString("\n") { val date = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp)); "$date,${it.lapTime},${if(it.isAboveTarget) "NG" else "OK"}" }
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_SUBJECT, "CycleTime_History"); putExtra(Intent.EXTRA_TEXT, csv) }
    context.startActivity(Intent.createChooser(intent, "履歴を共有"))
}

@Composable
fun MechanicalPage(viewModel: GenbaViewModel, onBack: () -> Unit) {
    val ppr = viewModel.mechPulsePerRot.toDoubleOrNull() ?: 1.0
    val gearN = viewModel.mechGearRatioN.toDoubleOrNull() ?: 1.0
    val lead = viewModel.mechLead.toDoubleOrNull() ?: 1.0
    val pulsesPerMm = (ppr * gearN) / lead
    val inputPulses = viewModel.mechInputPulses.toDoubleOrNull() ?: 0.0
    val convertedMm = if (pulsesPerMm > 0) inputPulses / pulsesPerMm else 0.0
    val inputMm = viewModel.mechInputMm.toDoubleOrNull() ?: 0.0
    val convertedPulses = inputMm * pulsesPerMm
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PageHeader("メカ計算", onBack)
        SectionTitle("基本パラメータ設定")
        Box(Modifier.cyberNeonBorder().padding(12.dp).background(ColorCard, RoundedCornerShape(12.dp))) { Column { InputGrid("エンコーダ解像度", "pulses/rev", viewModel.mechPulsePerRot) { viewModel.mechPulsePerRot = it; viewModel.saveMechSettings() }; InputGrid("減速比 (1/N)", "Nの値を入力", viewModel.mechGearRatioN) { viewModel.mechGearRatioN = it; viewModel.saveMechSettings() }; InputGrid("ボールネジリード", "mm/rev", viewModel.mechLead) { viewModel.mechLead = it; viewModel.saveMechSettings() }; Spacer(Modifier.height(8.dp)); ResItem("1mmあたりのパルス数", String.format("%.2f", pulsesPerMm), "pls/mm", ColorAccent) } }
        SectionTitle("パルス ⇔ 距離(mm) 相互変換")
        Box(Modifier.cyberNeonBorder().padding(12.dp).background(ColorCard, RoundedCornerShape(12.dp))) { Column { InputGrid("入力パルス数", "pls", viewModel.mechInputPulses) { viewModel.mechInputPulses = it }; ResItem("変換後の距離", String.format("%.4f", convertedMm), "mm"); Spacer(Modifier.height(16.dp)); InputGrid("入力距離", "mm", viewModel.mechInputMm) { viewModel.mechInputMm = it }; ResItem("変換後のパルス数", String.format("%.0f", convertedPulses), "pls") } }
        Text("※ 計算式: (Resolution * GearRatio) / Lead", color = ColorLabel.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(top = 16.dp, start = 4.dp))
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProductivityPage(onBack: () -> Unit) {
    var t1 by remember { mutableStateOf("30") }; var t2 by remember { mutableStateOf("85") }; var t3 by remember { mutableStateOf("8") }
    var o1 by remember { mutableStateOf("480") }; var o2 by remember { mutableStateOf("420") }; var o3 by remember { mutableStateOf("30") }
    var o4 by remember { mutableStateOf("700") }; var o5 by remember { mutableStateOf("680") }
    val cycle = t1.toDoubleOrNull() ?: 0.0; val rate = (t2.toDoubleOrNull() ?: 0.0) / 100.0; val hours = t3.toDoubleOrNull() ?: 0.0
    val resTA = if (rate > 0) cycle / rate else 0.0; val targetOutput = if (cycle > 0) (hours * 3600.0 / cycle) * rate else 0.0
    val resTC = if (hours > 0) targetOutput / hours else 0.0
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PageHeader("生産性・効率", onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { t1=""; t2=""; t3="" }) { Text("仕事量クリア ✕", color = ColorDanger, fontWeight = FontWeight.Bold) } }
        SectionTitle("【エリア①】仕事量計算")
        Box(Modifier.cyberNeonBorder().padding(12.dp).background(ColorCard, RoundedCornerShape(12.dp))) { Column { InputGrid("瞬間サイクル (Cycle)", "秒/台", t1) { t1 = it }; InputGrid("目標稼働率 (%)", null, t2) { t2 = it }; InputGrid("実稼働時間 (時間/日)", null, t3, imeAction = ImeAction.Done) { t3 = it }; ResItem("① 目標タクトタイム", String.format("%.1f", resTA), "秒/台"); ResItem("② 1日の目標台数", targetOutput.toInt().toString(), "台/日"); ResItem("③ 時間出来高", String.format("%.1f", resTC), "台/h") } }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PlanPage(onBack: () -> Unit) {
    var p1 by remember { mutableStateOf("18000") }; var p2 by remember { mutableStateOf("20") }; var p3 by remember { mutableStateOf("16") }; var p4 by remember { mutableStateOf("80") }
    val target = p1.toDoubleOrNull() ?: 0.0; val days = p2.toDoubleOrNull() ?: 0.0; val hours = p3.toDoubleOrNull() ?: 0.0; val rate = (p4.toDoubleOrNull() ?: 0.0) / 100.0
    val maxD = if (days > 0) target / days else 0.0; val realH = hours * rate; val resPH = if (maxD > 0) (hours * 3600.0 * rate) / maxD else 0.0
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PageHeader("出来高計画", onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { p1=""; p2=""; p3="" ; p4="" }) { Text("入力クリア ✕", color = ColorDanger, fontWeight = FontWeight.Bold) } }
        Box(Modifier.cyberNeonBorder().padding(12.dp).background(ColorCard, RoundedCornerShape(12.dp))) { Column { InputGrid("月産目標 (台/月)", null, p1) { p1 = it }; InputGrid("月間稼働日数 (日)", null, p2) { p2 = it }; InputGrid("1日の労働時間 (h)", null, p3) { p3 = it }; InputGrid("目標稼働率 (%)", null, p4, imeAction = ImeAction.Done) { p4 = it }; SectionTitle("計算結果詳細"); ResItem("① 1日の最大生産能力", String.format("%.1f", maxD), "台/日"); ResItem("② 1日の実質稼働時間", String.format("%.2f", realH), "h/日"); ResItem("⑤ 1日の実際の出来高", (maxD * rate).toInt().toString(), "台/日"); ResItem("⑦ 必達目標タクト", String.format("%.1f", resPH), "秒/台", ColorPrimary) } }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TimeConverterPage(onBack: () -> Unit) {
    var totalSecs by remember { mutableStateOf(0.0) }
    val units = listOf(Triple("日 (Days)", 86400.0, ColorAccent), Triple("時間 (Hours)", 3600.0, ColorAccent), Triple("分 (Minutes)", 60.0, ColorAccent), Triple("秒 (Seconds)", 1.0, ColorAccent), Triple("ミリ秒 (ms)", 0.001, ColorAccent))
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PageHeader("時間変換", onBack)
        Box(Modifier.cyberNeonBorder().padding(12.dp).background(ColorCard, RoundedCornerShape(12.dp))) { Column { units.forEachIndexed { idx, (label, factor, color) -> val displayValue = if (totalSecs == 0.0) "" else { val v = totalSecs / factor; if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.4f", v).trimEnd('0').trimEnd('.') }; InputGrid(label, null, displayValue, color, if (idx == units.size - 1) ImeAction.Done else ImeAction.Next) { totalSecs = (it.toDoubleOrNull() ?: 0.0) * factor } }; Button(onClick = { totalSecs = 0.0 }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)) { Text("数値をすべてクリア", fontWeight = FontWeight.Black) } } }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TimeChartPage(onBack: () -> Unit) {
    var actions by remember { mutableStateOf(listOf(ActionData(name = "動作1", delay = "0", duration = "2.0"))) }
    val totalCycle = actions.map { (it.delay.toDoubleOrNull() ?: 0.0) + (it.duration.toDoubleOrNull() ?: 0.0) }.maxOrNull() ?: 0.0
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PageHeader("タイムチャート", onBack)
        Box(Modifier.cyberNeonBorder().padding(12.dp).background(ColorCard, RoundedCornerShape(12.dp))) { Column { actions.forEachIndexed { idx, action -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { BasicTextField(value = action.name, onValueChange = { n -> actions = actions.toMutableList().also { it[idx] = it[idx].copy(name = n) } }, modifier = Modifier.weight(1f).background(ColorInputBg, RoundedCornerShape(4.dp)).padding(8.dp), textStyle = TextStyle(color = ColorText, fontSize = 12.sp, fontWeight = FontWeight.Bold)); Spacer(Modifier.width(4.dp)); BasicTextField(value = action.delay, onValueChange = { d -> actions = actions.toMutableList().also { it[idx] = it[idx].copy(delay = d) } }, modifier = Modifier.weight(0.6f).background(ColorInputBg, RoundedCornerShape(4.dp)).padding(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = ColorText, fontSize = 12.sp, textAlign = TextAlign.Center)); Spacer(Modifier.width(4.dp)); BasicTextField(value = action.duration, onValueChange = { dr -> actions = actions.toMutableList().also { it[idx] = it[idx].copy(duration = dr) } }, modifier = Modifier.weight(0.6f).background(ColorInputBg, RoundedCornerShape(4.dp)).padding(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = ColorText, fontSize = 12.sp, textAlign = TextAlign.Center)); IconButton(onClick = { actions = actions.toMutableList().filterIndexed { i, _ -> i != idx } }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Close, null, tint = ColorDanger) } } }; Button(onClick = { actions = actions + ActionData(name = "動作${actions.size+1}") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)) { Text("＋ 動作追加", fontWeight = FontWeight.Black, color = ColorBg) }; ResItem("合計サイクル", String.format("%.2f", totalCycle), "秒", ColorWarn) } }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoordPage(masterUF: List<MasterCoord>, masterTF: List<MasterCoord>, onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var x1 by remember { mutableStateOf("500") }; var y1 by remember { mutableStateOf("0") }; var z1 by remember { mutableStateOf("500") }; var w1 by remember { mutableStateOf("0") }; var p1 by remember { mutableStateOf("-90") }; var r1 by remember { mutableStateOf("0") }
    var x2 by remember { mutableStateOf("") }; var y2 by remember { mutableStateOf("") }; var z2 by remember { mutableStateOf("") }; var w2 by remember { mutableStateOf("") }; var p2 by remember { mutableStateOf("") }; var r2 by remember { mutableStateOf("") }
    var splitMode by remember { mutableStateOf("dist") }; var calcValue by remember { mutableStateOf("20") }; var points by remember { mutableStateOf(listOf<List<String>>()) }; var offDist by remember { mutableStateOf("50") }
    var selectedUFIdx by remember { mutableStateOf(0) }; var selectedTFIdx by remember { mutableStateOf(0) }; var showUFMenu by remember { mutableStateOf(false) }; var showTFMenu by remember { mutableStateOf(false) }
    var selectedCalcUfSlot by remember { mutableStateOf(0) }; var selectedCalcTfSlot by remember { mutableStateOf(0) }; var showCalcUfMenu by remember { mutableStateOf(false) }; var showCalcTfMenu by remember { mutableStateOf(false) }
    fun calc() { haptic.performHapticFeedback(HapticFeedbackType.LongPress); val st = listOf(x1,y1,z1,w1,p1,r1).map { it.toDoubleOrNull() ?: 0.0 }; val en = listOf(x2,y2,z2,w2,p2,r2).map { it.toDoubleOrNull() ?: 0.0 }; val dist = sqrt((en[0]-st[0]).pow(2) + (en[1]-st[1]).pow(2) + (en[2]-st[2]).pow(2)); val v = calcValue.toDoubleOrNull() ?: 20.0; val n = if (splitMode == "dist") ceil(dist / v).toInt().coerceAtLeast(1) else v.toInt().minus(1).coerceAtLeast(1); points = (0..n).map { i -> val ratio = i.toDouble() / n; listOf(i.toString()) + (0..5).map { k -> String.format("%.3f", st[k] + (en[k]-st[k])*ratio) } } }
    fun applyOffset(mode: String, sign: Int) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); val d = (offDist.toDoubleOrNull() ?: 0.0) * sign; val p1_x = x1.toDoubleOrNull() ?: 0.0; val p1_y = y1.toDoubleOrNull() ?: 0.0; val p1_z = z1.toDoubleOrNull() ?: 0.0; val p1_w = w1.toDoubleOrNull() ?: 0.0; val p1_p = p1.toDoubleOrNull() ?: 0.0; val p1_r = r1.toDoubleOrNull() ?: 0.0; val master = if (mode == "user") masterUF[selectedCalcUfSlot].toRobotPoint() else masterTF[selectedCalcTfSlot].toRobotPoint(); if (mode == "user") { x2 = (p1_x + master.x).toString(); y2 = (p1_y + master.y).toString(); z2 = (p1_z + master.z + d).toString() } else { val (dx, dy, dz) = RobotMath.getToolZOffsetVector(p1_w, p1_p, p1_r, d); x2 = (p1_x + master.x + dx).toString(); y2 = (p1_y + master.y + dy).toString(); z2 = (p1_z + master.z + dz).toString() }; w2 = w1; p2 = p1; r2 = r1; calc() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PageHeader("座標分割・逃げ", onBack)
        Box(Modifier.cyberNeonBorder().padding(12.dp).background(ColorCard, RoundedCornerShape(12.dp))) { Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Button(onClick = { showUFMenu = true }, modifier = Modifier.fillMaxWidth().height(45.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg)) { Text("UF$selectedUFIdx 読込 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold) }; DropdownMenu(expanded = showUFMenu, onDismissRequest = { showUFMenu = false }) { masterUF.forEachIndexed { i, d -> DropdownMenuItem(text = { Text("Slot $i: ${d.name}") }, onClick = { selectedUFIdx = i; x1=d.x; y1=d.y; z1=d.z; w1=d.w; p1=d.p; r1=d.r; showUFMenu = false }) } } } ; Box(Modifier.weight(1f)) { Button(onClick = { showTFMenu = true }, modifier = Modifier.fillMaxWidth().height(45.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg)) { Text("TF$selectedTFIdx 読込 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold) }; DropdownMenu(expanded = showTFMenu, onDismissRequest = { showTFMenu = false }) { masterTF.forEachIndexed { i, d -> DropdownMenuItem(text = { Text("Slot $i: ${d.name}") }, onClick = { selectedTFIdx = i; x1=d.x; y1=d.y; z1=d.z; w1=d.w; p1=d.p; r1=d.r; showTFMenu = false }) } } } }; Text("始点 P1", color = ColorText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp)); CoordInputGrid(listOf(x1,y1,z1,w1,p1,r1)) { i,v -> when(i){0->x1=v;1->y1=v;2->z1=v;3->w1=v;4->p1=v;5->r1=v} }; Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).background(Color(0xFF252A30), RoundedCornerShape(12.dp)).border(2.dp, ColorPrimary, RoundedCornerShape(12.dp)).padding(12.dp)) { Column { InputGrid("逃げ距離(mm)", null, offDist) { offDist = it }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { applyOffset("user", 1) }, Modifier.weight(1f).height(48.dp)) { Text("User Z+", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }; Button(onClick = { applyOffset("tool", 1) }, Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)) { Text("Tool Z+ 進", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) } } } }; Text("終点 P2", color = ColorText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp); CoordInputGrid(listOf(x2,y2,z2,w2,p2,r2)) { i,v -> when(i){0->x2=v;1->y2=v;2->z2=v;3->w2=v;4->p2=v;5->r2=v} }; Button(onClick = { calc() }, Modifier.fillMaxWidth().padding(top = 16.dp).height(60.dp)) { Text("計算実行", fontSize = 18.sp, fontWeight = FontWeight.Black) } } }
        if (points.isNotEmpty()) { Box(Modifier.padding(top=16.dp).background(ColorCard, RoundedCornerShape(8.dp)).border(1.dp, ColorBorder, RoundedCornerShape(8.dp))) { LazyColumn(modifier = Modifier.heightIn(max = 400.dp).horizontalScroll(rememberScrollState())) { stickyHeader { Row(Modifier.background(ColorInputBg).padding(8.dp)) { listOf("No.","X","Y","Z","W","P","R").forEach { header -> Text(header, Modifier.width(80.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 12.sp, color = ColorAccent) } } }; itemsIndexed(points) { index, row -> Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) { row.forEach { cell -> Text(text = cell, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ColorText) } } } } } }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun MasterPage(masterUF: List<MasterCoord>, masterTF: List<MasterCoord>, onUpdateUF: (List<MasterCoord>) -> Unit, onUpdateTF: (List<MasterCoord>) -> Unit, onBack: () -> Unit) {
    var selectedSlot by remember { mutableStateOf(0) }; var mName by remember { mutableStateOf("") }; var mx by remember { mutableStateOf("0") }; var my by remember { mutableStateOf("0") }; var mz by remember { mutableStateOf("0") }; var mw by remember { mutableStateOf("0") }; var mp by remember { mutableStateOf("0") }; var mr by remember { mutableStateOf("0") }
    fun fill(slot: Int) { val d = masterUF[slot]; mName = d.name; mx=d.x; my=d.y; mz=d.z; mw=d.w; mp=d.p; mr=d.r }
    LaunchedEffect(Unit) { fill(0) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PageHeader("座標マスタ管理", onBack)
        Box(Modifier.cyberNeonBorder().padding(12.dp).background(ColorCard, RoundedCornerShape(12.dp))) { Column { Row(verticalAlignment = Alignment.CenterVertically) { Text("Slot", Modifier.weight(1f), fontSize = 13.sp, color = ColorLabel, fontWeight = FontWeight.ExtraBold); (0..9).forEach { i -> Box(Modifier.size(32.dp).padding(2.dp).background(if(selectedSlot==i) ColorPrimary else ColorInputBg, RoundedCornerShape(6.dp)).clickable { selectedSlot=i; fill(i) }, contentAlignment = Alignment.Center) { Text(i.toString(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = ColorText) } } }; TextField(value = mName, onValueChange = { mName = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), label = { Text("名称", color = ColorLabel) }, colors = TextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg, focusedTextColor = ColorText, unfocusedTextColor = ColorText)); CoordInputGrid(listOf(mx,my,mz,mw,mp,mr)) { i,v -> when(i){0->mx=v;1->my=v;2->mz=v;3->mw=v;4->mp=v;5->mr=v} }; Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = { onUpdateUF(masterUF.toMutableList().also { it[selectedSlot] = MasterCoord(mName,mx,my,mz,mw,mp,mr) }) }, Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)) { Text("UF保存", fontWeight = FontWeight.Black, color = ColorBg) }; Button(onClick = { onUpdateTF(masterTF.toMutableList().also { it[selectedSlot] = MasterCoord(mName,mx,my,mz,mw,mp,mr) }) }, Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)) { Text("TF保存", fontWeight = FontWeight.Black) } } } }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SectionTitle(title: String) { Text(text = title, color = ColorWarn, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp).drawBehind { drawLine(ColorWarn, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx()) }) }

@Composable
fun InputGrid(label: String, subLabel: String? = null, value: String, labelColor: Color = ColorLabel, imeAction: ImeAction = ImeAction.Next, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val isError = value.isNotEmpty() && value.toDoubleOrNull() == null && value != "-" && value != "."
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1.6f)) { Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Black); if (subLabel != null) Text(subLabel, color = labelColor.copy(alpha = 0.8f), fontSize = 10.sp) }; OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f).height(52.dp).onFocusChanged { isFocused = it.isFocused }, isError = isError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }, onDone = { focusManager.clearFocus() }), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorInputFocus, unfocusedContainerColor = ColorInputBg, focusedTextColor = ColorText, unfocusedTextColor = ColorText, focusedBorderColor = ColorPrimary, unfocusedBorderColor = ColorBorder), textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorText), singleLine = true, shape = RoundedCornerShape(8.dp)) }
}

@Composable
fun ResItem(label: String, value: String, unit: String, borderColor: Color = ColorPrimary) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(ColorInputBg, RoundedCornerShape(8.dp)).drawBehind { drawLine(borderColor, Offset(0f, 0f), Offset(0f, size.height), 5.dp.toPx()) }.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = ColorLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold); Row(verticalAlignment = Alignment.Bottom) { Text(value, color = ColorAccent, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace); Text(unit, color = ColorLabel, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp)) } } }

@Composable
fun CoordInputGrid(values: List<String>, onUpdate: (Int, String) -> Unit) { val labels = listOf("X", "Y", "Z", "W", "P", "R"); Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { (0..2).forEach { i -> CoordSingleInput(labels[i], values[i], ImeAction.Next) { onUpdate(i, it) } } }; Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { (3..5).forEach { i -> CoordSingleInput(labels[i], values[i], if (i == 5) ImeAction.Done else ImeAction.Next) { onUpdate(i, it) } } } } }

@Composable
fun RowScope.CoordSingleInput(label: String, value: String, imeAction: ImeAction = ImeAction.Next, onValueChange: (String) -> Unit) { val focusManager = LocalFocusManager.current; OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f).height(48.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg, focusedTextColor = ColorText, unfocusedTextColor = ColorText), textStyle = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), prefix = { Text(label, fontSize = 9.sp, color = ColorAccent) }, singleLine = true, shape = RoundedCornerShape(4.dp)) }

object RobotMath { fun getToolZOffsetVector(wDeg: Double, pDeg: Double, rDeg: Double, dist: Double): Triple<Double, Double, Double> { val w = Math.toRadians(wDeg); val p = Math.toRadians(pDeg); val r = Math.toRadians(rDeg); val dx = dist * (cos(r) * sin(p) * cos(w) + sin(r) * sin(w)); val dy = dist * (sin(r) * sin(p) * cos(w) - cos(r) * sin(w)); val dz = dist * (cos(p) * cos(w)); return Triple(dx, dy, dz) } }
