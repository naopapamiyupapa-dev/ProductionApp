// Version 1.1.0 (Integrated Launcher Edition)
package com.example.genba

import android.os.Bundle
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.math.*

// --- [1] カラー定義 ---
val ColorPrimary = Color(0xFF007AFF) // ブルー
val ColorBg = Color(0xFF000000)      // ブラック
val ColorCard = Color(0xFF1C1C1E)    // ダークグレー
val ColorText = Color(0xFFFFFFFF)    // ホワイト
val ColorAccent = Color(0xFF32D74B)  // グリーン
val ColorWarn = Color(0xFFFF9F0A)    // オレンジ
val ColorDanger = Color(0xFFFF453A)  // レッド
val ColorLabel = Color(0xFFF2F2F7)
val ColorBorder = Color(0xFF48484A)
val ColorInputBg = Color(0xFF2C2C2E)

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
        colorScheme = darkColorScheme(
            primary = ColorPrimary,
            onPrimary = ColorText,
            background = ColorBg,
            surface = ColorCard,
            onSurface = ColorText,
            secondary = ColorAccent
        ),
        content = content
    )
}

// ユーティリティ：コピー機能
fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("GenbaTool", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "コピーしました✨", Toast.LENGTH_SHORT).show()
}

// --- [2] データ定義 ---
data class RobotPoint(val x: Double, val y: Double, val z: Double, val w: Double, val p: Double, val r: Double)
data class MasterCoord(var name: String = "", var x: String = "0", var y: String = "0", var z: String = "0", var w: String = "0", var p: String = "0", var r: String = "0")
fun MasterCoord.toRobotPoint() = RobotPoint(x.toDoubleOrNull() ?: 0.0, y.toDoubleOrNull() ?: 0.0, z.toDoubleOrNull() ?: 0.0, w.toDoubleOrNull() ?: 0.0, p.toDoubleOrNull() ?: 0.0, r.toDoubleOrNull() ?: 0.0)
data class ActionData(val id: String = UUID.randomUUID().toString(), var name: String = "", var delay: String = "0", var duration: String = "1.0")

// --- [3] メイン構造（司令塔） ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // 起動時は "dashboard"
    var currentTab by rememberSaveable { mutableStateOf("dashboard") }
    // 座標マスタはページ間で共有するため MainScreen で保持
    var masterUF by remember { mutableStateOf(List(10) { MasterCoord("UF$it") }) }
    var masterTF by remember { mutableStateOf(List(10) { MasterCoord("TF$it") }) }

    Scaffold(
        topBar = {
            // ダッシュボード以外では上部にAppBarを表示
            if (currentTab != "dashboard") {
                CenterAlignedTopAppBar(
                    title = { Text(getPageTitle(currentTab), fontWeight = FontWeight.Black, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { currentTab = "dashboard" }) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = ColorText)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = ColorCard,
                        titleContentColor = ColorText
                    )
                )
            }
        },
        containerColor = ColorBg
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                "dashboard" -> DashboardPage { currentTab = it }
                "prod" -> ProductivityPage()
                "plan" -> PlanPage()
                "time" -> TimeConverterPage()
                "chart" -> TimeChartPage()
                "coord" -> CoordIntegratedPage(masterUF, masterTF, { masterUF = it }, { masterTF = it })
            }
        }
    }
}

fun getPageTitle(id: String) = when(id) {
    "prod" -> "生産性・効率"
    "plan" -> "出来高計画"
    "time" -> "時間変換"
    "chart" -> "タイムチャート"
    "coord" -> "座標総合管理"
    else -> "現場ツール"
}

// --- [4] ダッシュボード（新規追加：タイルUI） ---
@Composable
fun DashboardPage(onSelect: (String) -> Unit) {
    val items = listOf(
        // Triple(ラベル, アイコン, ページID, カラー)
        Triple("生産性・OEE", Icons.Default.Assessment, "prod" to ColorAccent),
        Triple("出来高計画", Icons.Default.DateRange, "plan" to ColorPrimary),
        Triple("チャート表示", Icons.Default.Timeline, "chart" to ColorPrimary),
        Triple("時間変換", Icons.Default.Timer, "time" to ColorWarn),
        Triple("座標総合\n(分割/マスタ)", Icons.Default.Place, "coord" to ColorDanger)
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "現場ダッシュボード",
            color = ColorText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                DashboardTile(
                    label = item.first,
                    icon = item.second,
                    config = item.third,
                    onClick = { onSelect(item.third.first) }
                )
            }
        }
    }
}

@Composable
fun DashboardTile(label: String, icon: ImageVector, config: Pair<String, Color>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ColorCard),
        border = BorderStroke(2.dp, config.second.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = config.second, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                color = ColorText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// --- [5] 各ページの中身（全ロジック完全維持） ---

@Composable
fun ProductivityPage() {
    var t1 by rememberSaveable { mutableStateOf("30") }; var t2 by rememberSaveable { mutableStateOf("85") }; var t3 by rememberSaveable { mutableStateOf("8") }
    var o1 by rememberSaveable { mutableStateOf("480") }; var o2 by rememberSaveable { mutableStateOf("420") }; var o3 by rememberSaveable { mutableStateOf("30") }
    var o4 by rememberSaveable { mutableStateOf("700") }; var o5 by rememberSaveable { mutableStateOf("680") }
    var workerNum by rememberSaveable { mutableStateOf("1") }

    val cycle = t1.toDoubleOrNull() ?: 0.0; val rate = (t2.toDoubleOrNull() ?: 0.0) / 100.0; val hours = t3.toDoubleOrNull() ?: 0.0
    val resTA = if (rate > 0) cycle / rate else 0.0
    val targetOutput = if (cycle > 0) (hours * 3600.0 / cycle) * rate else 0.0
    val resTC = if (hours > 0) targetOutput / hours else 0.0

    val lO1 = o1.toDoubleOrNull() ?: 0.0; val lO2 = o2.toDoubleOrNull() ?: 0.0; val lO3 = o3.toDoubleOrNull() ?: 0.0
    val lO4 = o4.toDoubleOrNull() ?: 0.0; val lO5 = o5.toDoubleOrNull() ?: 0.0
    val timeRate = if (lO1 > 0) (lO2 / lO1) * 100 else 0.0
    val perfRate = if (lO2 > 0 && lO3 > 0) (lO3 * lO4 / (lO2 * 60)) * 100 else 0.0
    val qualRate = if (lO4 > 0) (lO5 / lO4) * 100 else 0.0
    val oee = (timeRate * perfRate * qualRate) / 10000.0
    val realH = if (lO3 > 0) (3600.0 / lO3) * (oee / 100.0) else 0.0

    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { t1=""; t2=""; t3="" }) { Text("仕事量クリア ✕", color = ColorDanger, fontWeight = FontWeight.Bold) }
        }
        SectionTitle("【エリア①】仕事量計算")
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                InputGrid("瞬間サイクル (Cycle)", "秒/台", t1) { t1 = it }
                InputGrid("目標稼働率 (%)", null, t2) { t2 = it }
                InputGrid("実稼働時間 (時間/日)", null, t3) { t3 = it }
                ResItem("① 目標タクトタイム", String.format("%.1f", resTA), "秒/台")
                ResItem("② 1日の目標台数", targetOutput.toInt().toString(), "台/日")
                ResItem("③ 時間出来高", String.format("%.1f", resTC), "台/h")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { o1=""; o2=""; o3=""; o4=""; o5="" }) { Text("OEEクリア ✕", color = ColorDanger, fontWeight = FontWeight.Bold) }
        }
        SectionTitle("【エリア②】設備総合効率 (OEE)")
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                InputGrid("負荷時間 (分)", null, o1) { o1 = it }
                InputGrid("実際稼働時間 (分)", null, o2) { o2 = it }
                InputGrid("基準サイクル (秒/台)", null, o3) { o3 = it }
                InputGrid("総生産数 (台)", null, o4) { o4 = it }
                InputGrid("良品数 (台)", null, o5) { o5 = it }
                ResItem("④ 時間稼働率", String.format("%.1f", timeRate), "%")
                ResItem("⑤ 性能稼働率", String.format("%.1f", perfRate), "%")
                ResItem("⑥ 良品率", String.format("%.1f", qualRate), "%")
                ResItem("⑦ 設備総合効率(OEE)", String.format("%.1f", oee), "%", ColorAccent)
            }
        }
        SectionTitle("シミュレーション")
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                InputGrid("担当人数 (人)", null, workerNum) { workerNum = it }
                ResItem("1hあたり実質出来高", String.format("%.1f", realH), "台/h", ColorWarn)
                ResItem("必要設備台数", if (realH > 0) String.format("%.1f", (targetOutput/hours)/realH) else "0", "台", ColorWarn)
                ResItem("総工数", String.format("%.1f", (workerNum.toDoubleOrNull() ?: 0.0) * hours), "人・h", ColorWarn)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PlanPage() {
    var p1 by rememberSaveable { mutableStateOf("18000") }; var p2 by rememberSaveable { mutableStateOf("20") }
    var p3 by rememberSaveable { mutableStateOf("16") }; var p4 by rememberSaveable { mutableStateOf("80") }
    val target = p1.toDoubleOrNull() ?: 0.0; val days = p2.toDoubleOrNull() ?: 0.0
    val hours = p3.toDoubleOrNull() ?: 0.0; val rate = (p4.toDoubleOrNull() ?: 0.0) / 100.0
    val maxD = if (days > 0) target / days else 0.0; val realH = hours * rate
    val resPH = if (maxD > 0) (hours * 3600.0 * rate) / maxD else 0.0

    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { p1=""; p2=""; p3=""; p4="" }) { Text("入力クリア ✕", color = ColorDanger, fontWeight = FontWeight.Bold) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                InputGrid("月産目標 (台/月)", null, p1) { p1 = it }
                InputGrid("月間稼働日数 (日)", null, p2) { p2 = it }
                InputGrid("1日の労働時間 (h)", null, p3) { p3 = it }
                InputGrid("目標稼働率 (%)", null, p4) { p4 = it }
                SectionTitle("計算結果詳細")
                ResItem("① 1日の最大生産能力", String.format("%.1f", maxD), "台/日")
                ResItem("② 1日の実質稼働時間", String.format("%.2f", realH), "h/日")
                ResItem("③ 1日の総許容秒数", (realH * 3600).toInt().toString(), "秒/日")
                ResItem("④ 1日の総許容分数", (realH * 60).toInt().toString(), "分/日")
                ResItem("⑤ 1日の実際の出来高", (maxD * rate).toInt().toString(), "台/日")
                ResItem("⑥ 月間実際の出来高", (maxD * rate * days).toInt().toString(), "台/月")
                ResItem("⑦ 必達目標タクト", String.format("%.1f", resPH), "秒/台", ColorPrimary)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TimeConverterPage() {
    var totalSecs by rememberSaveable { mutableStateOf(0.0) }
    val units = listOf(
        Triple("日 (Days)", 86400.0, ColorAccent),
        Triple("時間 (Hours)", 3600.0, ColorAccent),
        Triple("分 (Minutes)", 60.0, ColorAccent),
        Triple("秒 (Seconds)", 1.0, ColorAccent),
        Triple("ミリ秒 (ms)", 0.001, ColorAccent),
        Triple("マイクロ秒 (μs)", 0.000001, ColorAccent)
    )
    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                units.forEach { (label, factor, color) ->
                    val displayValue = if (totalSecs == 0.0) "" else {
                        val v = totalSecs / factor
                        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.6f", v).trimEnd('0').trimEnd('.')
                    }
                    InputGrid(label, null, displayValue, labelColor = color) { totalSecs = (it.toDoubleOrNull() ?: 0.0) * factor }
                }
                Button(onClick = { totalSecs = 0.0 }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)) {
                    Text("数値をすべてクリア", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TimeChartPage() {
    var actions by remember { mutableStateOf(listOf(ActionData(name = "動作1", delay = "0", duration = "2.0"))) }
    val totalCycle = actions.map { (it.delay.toDoubleOrNull() ?: 0.0) + (it.duration.toDoubleOrNull() ?: 0.0) }.maxOrNull() ?: 0.0

    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("動作名", Modifier.weight(1f), fontSize = 10.sp, textAlign = TextAlign.Center, color = ColorLabel, fontWeight = FontWeight.Bold)
                    Text("開始秒", Modifier.weight(1f), fontSize = 10.sp, textAlign = TextAlign.Center, color = ColorLabel, fontWeight = FontWeight.Bold)
                    Text("動作秒", Modifier.weight(1f), fontSize = 10.sp, textAlign = TextAlign.Center, color = ColorLabel, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(30.dp))
                }
                actions.forEachIndexed { idx, action ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(value = action.name, onValueChange = { n -> actions = actions.toMutableList().also { it[idx] = it[idx].copy(name = n) } },
                            modifier = Modifier.weight(1f).background(ColorInputBg, RoundedCornerShape(4.dp)).padding(8.dp), textStyle = TextStyle(color = ColorText, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.width(4.dp))
                        BasicTextField(value = action.delay, onValueChange = { d -> actions = actions.toMutableList().also { it[idx] = it[idx].copy(delay = d) } },
                            modifier = Modifier.weight(1f).background(ColorInputBg, RoundedCornerShape(4.dp)).padding(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = ColorText, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.width(4.dp))
                        BasicTextField(value = action.duration, onValueChange = { dr -> actions = actions.toMutableList().also { it[idx] = it[idx].copy(duration = dr) } },
                            modifier = Modifier.weight(1f).background(ColorInputBg, RoundedCornerShape(4.dp)).padding(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = ColorText, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold))
                        IconButton(onClick = { actions = actions.toMutableList().filterIndexed { i, _ -> i != idx } }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Close, null, tint = ColorDanger) }
                    }
                }
                Button(onClick = { actions = actions + ActionData(name = "動作${actions.size+1}") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)) { Text("＋ 動作追加", fontWeight = FontWeight.Black, color = ColorText) }
                ResItem("合計サイクル", String.format("%.2f", totalCycle), "秒", ColorWarn)
                SectionTitle("視覚化グラフ")
                actions.forEach { act ->
                    val d = act.delay.toDoubleOrNull() ?: 0.0; val dr = act.duration.toDoubleOrNull() ?: 0.0
                    val sW = if (totalCycle > 0) (d / totalCycle).toFloat() else 0f; val dW = if (totalCycle > 0) (dr / totalCycle).toFloat() else 0f
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(ColorInputBg, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(act.name, color = ColorAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("${d}s〜${String.format("%.1f", d + dr)}s", color = ColorLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(Modifier.fillMaxWidth().height(20.dp).padding(top = 6.dp).background(Color.Black, RoundedCornerShape(4.dp)).border(1.dp, ColorBorder, RoundedCornerShape(4.dp))) {
                            Row(Modifier.fillMaxSize()) {
                                if (sW > 0) Spacer(Modifier.weight(sW))
                                Box(Modifier.weight(if (dW > 0) dW else 0.001f).fillMaxHeight().background(if(dW>0) ColorPrimary else Color.Transparent, RoundedCornerShape(2.dp)))
                                val eW = (1.0f - sW - dW).coerceAtLeast(0f)
                                if (eW > 0) Spacer(Modifier.weight(eW))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- [6] 座標総合（分割＆マスタ統合：全ロジック維持） ---
@Composable
fun CoordIntegratedPage(
    masterUF: List<MasterCoord>, masterTF: List<MasterCoord>,
    onUpdateUF: (List<MasterCoord>) -> Unit, onUpdateTF: (List<MasterCoord>) -> Unit
) {
    var subTab by rememberSaveable { mutableStateOf("split") }

    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        // サブタブ（分割かマスタか）
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { subTab = "split" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if(subTab=="split") ColorDanger else ColorCard)
            ) { Text("座標分割・逃げ", fontWeight = FontWeight.Bold) }
            Button(
                onClick = { subTab = "master" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if(subTab=="master") ColorDanger else ColorCard)
            ) { Text("マスタ管理", fontWeight = FontWeight.Bold) }
        }

        Divider(color = ColorBorder, thickness = 1.dp)

        if (subTab == "split") {
            CoordSplitSubPage(masterUF, masterTF)
        } else {
            MasterSubPage(masterUF, masterTF, onUpdateUF, onUpdateTF)
        }
    }
}

@Composable
fun CoordSplitSubPage(masterUF: List<MasterCoord>, masterTF: List<MasterCoord>) {
    var x1 by rememberSaveable { mutableStateOf("500") }; var y1 by rememberSaveable { mutableStateOf("0") }; var z1 by rememberSaveable { mutableStateOf("500") }
    var w1 by rememberSaveable { mutableStateOf("0") }; var p1 by rememberSaveable { mutableStateOf("-90") }; var r1 by rememberSaveable { mutableStateOf("0") }
    var x2 by rememberSaveable { mutableStateOf("") }; var y2 by rememberSaveable { mutableStateOf("") }; var z2 by rememberSaveable { mutableStateOf("") }
    var w2 by rememberSaveable { mutableStateOf("") }; var p2 by rememberSaveable { mutableStateOf("") }; var r2 by rememberSaveable { mutableStateOf("") }
    var splitMode by rememberSaveable { mutableStateOf("dist") }
    var calcValue by rememberSaveable { mutableStateOf("20") }; var points by remember { mutableStateOf(listOf<List<String>>()) }
    var offDist by rememberSaveable { mutableStateOf("50") }

    var selectedUFIdx by rememberSaveable { mutableStateOf(0) }
    var selectedTFIdx by rememberSaveable { mutableStateOf(0) }
    var showUFMenu by remember { mutableStateOf(false) }
    var showTFMenu by remember { mutableStateOf(false) }
    var selectedCalcUfSlot by rememberSaveable { mutableStateOf(0) }
    var selectedCalcTfSlot by rememberSaveable { mutableStateOf(0) }
    var showCalcUfMenu by remember { mutableStateOf(false) }
    var showCalcTfMenu by remember { mutableStateOf(false) }
    var highlightedRows by remember { mutableStateOf(setOf<Int>()) }

    fun calc() {
        val st = listOf(x1,y1,z1,w1,p1,r1).map { it.toDoubleOrNull() ?: 0.0 }; val en = listOf(x2,y2,z2,w2,p2,r2).map { it.toDoubleOrNull() ?: 0.0 }
        val dist = sqrt((en[0]-st[0]).pow(2) + (en[1]-st[1]).pow(2) + (en[2]-st[2]).pow(2))
        val v = calcValue.toDoubleOrNull() ?: 20.0
        val n = if (splitMode == "dist") ceil(dist / v).toInt().coerceAtLeast(1) else v.toInt().minus(1).coerceAtLeast(1)
        points = (0..n).map { i -> val ratio = i.toDouble() / n; listOf(i.toString()) + (0..5).map { k -> String.format("%.3f", st[k] + (en[k]-st[k])*ratio) } }
        highlightedRows = emptySet()
    }

    fun applyOffset(mode: String, sign: Int) {
        val d = (offDist.toDoubleOrNull() ?: 0.0) * sign
        val p1_x = x1.toDoubleOrNull() ?: 0.0; val p1_y = y1.toDoubleOrNull() ?: 0.0; val p1_z = z1.toDoubleOrNull() ?: 0.0
        val p1_w = w1.toDoubleOrNull() ?: 0.0; val p1_p = p1.toDoubleOrNull() ?: 0.0; val p1_r = r1.toDoubleOrNull() ?: 0.0
        val master = if (mode == "user") masterUF[selectedCalcUfSlot].toRobotPoint() else masterTF[selectedCalcTfSlot].toRobotPoint()
        if (mode == "user") {
            x2 = (p1_x + master.x).toString(); y2 = (p1_y + master.y).toString(); z2 = (p1_z + master.z + d).toString()
        } else {
            val rw = p1_w * PI / 180.0; val rp = p1_p * PI / 180.0; val totalMove = master.z + d
            x2 = (p1_x + master.x + sin(rp) * totalMove).toString(); y2 = (p1_y + master.y - sin(rw) * cos(rp) * totalMove).toString(); z2 = (p1_z + cos(rw) * cos(rp) * totalMove).toString()
        }
        w2 = w1; p2 = p1; r2 = r1; calc()
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { x1=""; y1=""; z1=""; w1=""; p1=""; r1=""; x2=""; y2=""; z2=""; w2=""; p2=""; r2="" }) { Text("座標クリア ✕", color = ColorDanger, fontWeight = FontWeight.Bold) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        Button(onClick = { showUFMenu = true }, modifier = Modifier.fillMaxWidth().height(45.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg)) {
                            Text("UF$selectedUFIdx 読込 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorText)
                        }
                        DropdownMenu(expanded = showUFMenu, onDismissRequest = { showUFMenu = false }) {
                            masterUF.forEachIndexed { i, d -> DropdownMenuItem(text = { Text("Slot $i: ${d.name}") }, onClick = { selectedUFIdx = i; x1=d.x; y1=d.y; z1=d.z; w1=d.w; p1=d.p; r1=d.r; showUFMenu = false }) }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        Button(onClick = { showTFMenu = true }, modifier = Modifier.fillMaxWidth().height(45.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg)) {
                            Text("TF$selectedTFIdx 読込 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorText)
                        }
                        DropdownMenu(expanded = showTFMenu, onDismissRequest = { showTFMenu = false }) {
                            masterTF.forEachIndexed { i, d -> DropdownMenuItem(text = { Text("Slot $i: ${d.name}") }, onClick = { selectedTFIdx = i; x1=d.x; y1=d.y; z1=d.z; w1=d.w; p1=d.p; r1=d.r; showTFMenu = false }) }
                        }
                    }
                }
                Text("始点 P1", color = ColorText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                CoordInputGrid(listOf(x1,y1,z1,w1,p1,r1)) { i,v -> when(i){0->x1=v;1->y1=v;2->z1=v;3->w1=v;4->p1=v;5->r1=v} }

                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).background(Color(0xFF252A30), RoundedCornerShape(12.dp)).border(2.dp, ColorPrimary, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                OutlinedButton(onClick = { showCalcUfMenu = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, ColorBorder)) {
                                    Text("UF$selectedCalcUfSlot 参照", fontSize = 10.sp, color = ColorLabel)
                                }
                                DropdownMenu(expanded = showCalcUfMenu, onDismissRequest = { showCalcUfMenu = false }) {
                                    masterUF.forEachIndexed { i, d -> DropdownMenuItem(text = { Text("UF$i: ${d.name}") }, onClick = { selectedCalcUfSlot = i; showCalcUfMenu = false }) }
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                OutlinedButton(onClick = { showCalcTfMenu = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, ColorBorder)) {
                                    Text("TF$selectedCalcTfSlot 参照", fontSize = 10.sp, color = ColorLabel)
                                }
                                DropdownMenu(expanded = showCalcTfMenu, onDismissRequest = { showCalcTfMenu = false }) {
                                    masterTF.forEachIndexed { i, d -> DropdownMenuItem(text = { Text("TF$i: ${d.name}") }, onClick = { selectedCalcTfSlot = i; showCalcTfMenu = false }) }
                                }
                            }
                        }
                        InputGrid("逃げ距離(mm)", null, offDist) { offDist = it }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { applyOffset("user", 1) }, Modifier.weight(1f).height(48.dp)) { Text("User Z+", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                            Button(onClick = { applyOffset("user", -1) }, Modifier.weight(1f).height(48.dp)) { Text("User Z-", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { applyOffset("tool", 1) }, Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)) { Text("Tool Z+ 進", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                            Button(onClick = { applyOffset("tool", -1) }, Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)) { Text("Tool Z- 退", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }

                Text("終点 P2", color = ColorText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                CoordInputGrid(listOf(x2,y2,z2,w2,p2,r2)) { i,v -> when(i){0->x2=v;1->y2=v;2->z2=v;3->w2=v;4->p2=v;5->r2=v} }

                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { splitMode = "dist" }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = if(splitMode=="dist") ColorPrimary else ColorInputBg), border = BorderStroke(1.dp, ColorBorder), shape = RoundedCornerShape(8.dp)) { Text("間隔指定", color = ColorText, fontWeight = FontWeight.ExtraBold) }
                    Button(onClick = { splitMode = "num" }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = if(splitMode=="num") ColorPrimary else ColorInputBg), border = BorderStroke(1.dp, ColorBorder), shape = RoundedCornerShape(8.dp)) { Text("点数指定", color = ColorText, fontWeight = FontWeight.ExtraBold) }
                }
                InputGrid(if(splitMode=="dist") "分割間隔(mm)" else "合計点数", null, calcValue) { calcValue = it }
                Button(onClick = { calc() }, Modifier.fillMaxWidth().padding(top = 16.dp).height(60.dp)) { Text("分割計算実行", fontSize = 18.sp, fontWeight = FontWeight.Black) }
            }
        }
        if (points.isNotEmpty()) {
            Box(Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState()).background(ColorCard, RoundedCornerShape(8.dp)).border(1.dp, ColorBorder, RoundedCornerShape(8.dp))) {
                Column {
                    Row(Modifier.background(ColorInputBg).padding(8.dp)) {
                        listOf("No.","X","Y","Z","W","P","R").forEach { header -> Text(header, Modifier.width(80.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 12.sp, color = ColorAccent) }
                    }
                    points.forEachIndexed { i, row ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .background(if (highlightedRows.contains(i)) ColorPrimary.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable { highlightedRows = if (highlightedRows.contains(i)) highlightedRows - i else highlightedRows + i }
                            .padding(8.dp).drawBehind { drawLine(ColorBorder, Offset(0f, size.height), Offset(size.width, size.height), 1f) }
                        ) {
                            row.forEach { cell -> Text(cell, Modifier.width(80.dp), textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ColorText) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun MasterSubPage(
    masterUF: List<MasterCoord>, masterTF: List<MasterCoord>,
    onUpdateUF: (List<MasterCoord>) -> Unit, onUpdateTF: (List<MasterCoord>) -> Unit
) {
    val context = LocalContext.current
    var selectedSlot by rememberSaveable { mutableStateOf(0) }; var mName by rememberSaveable { mutableStateOf("") }
    var mx by rememberSaveable { mutableStateOf("0") }; var my by rememberSaveable { mutableStateOf("0") }; var mz by rememberSaveable { mutableStateOf("0") }
    var mw by rememberSaveable { mutableStateOf("0") }; var mp by rememberSaveable { mutableStateOf("0") }; var mr by rememberSaveable { mutableStateOf("0") }

    fun fill(slot: Int) { val d = masterUF[slot]; mName = d.name; mx=d.x; my=d.y; mz=d.z; mw=d.w; mp=d.p; mr=d.r }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionTitle("スロット選択・編集")
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    (0..9).forEach { i ->
                        Box(Modifier.size(45.dp).padding(2.dp).background(if(selectedSlot==i) ColorDanger else ColorInputBg, RoundedCornerShape(6.dp)).clickable { selectedSlot=i; fill(i) }, contentAlignment = Alignment.Center) {
                            Text(i.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = ColorText)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }
                TextField(value = mName, onValueChange = { mName = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), label = { Text("地点名称を入力", color = ColorLabel, fontWeight = FontWeight.Bold) }, colors = TextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg, focusedTextColor = ColorText, unfocusedTextColor = ColorText))
                CoordInputGrid(listOf(mx,my,mz,mw,mp,mr)) { i,v -> when(i){0->mx=v;1->my=v;2->mz=v;3->mw=v;4->mp=v;5->mr=v} }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        onUpdateUF(masterUF.toMutableList().also { it[selectedSlot] = MasterCoord(mName,mx,my,mz,mw,mp,mr) })
                        Toast.makeText(context, "Slot $selectedSlot をUFに保存", Toast.LENGTH_SHORT).show()
                    }, Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)) { Text("UF保存", fontWeight = FontWeight.Black, color = ColorBg) }

                    Button(onClick = {
                        onUpdateTF(masterTF.toMutableList().also { it[selectedSlot] = MasterCoord(mName,mx,my,mz,mw,mp,mr) })
                        Toast.makeText(context, "Slot $selectedSlot をTFに保存", Toast.LENGTH_SHORT).show()
                    }, Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)) { Text("TF保存", fontWeight = FontWeight.Black, color = ColorText) }
                }
            }
        }
        SectionTitle("登録済みサマリ")
        masterUF.forEachIndexed { i, d ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(ColorCard, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Text("Slot $i:", Modifier.width(55.dp), fontSize = 11.sp, color = ColorAccent, fontWeight = FontWeight.Black)
                Text("UF [${d.name}] / TF [${masterTF[i].name}]", fontSize = 11.sp, color = ColorText, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// --- [7] 共通UI部品：セクションタイトル、入力グリッド、結果アイテム ---

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title, color = ColorWarn, fontSize = 14.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp).drawBehind {
            drawLine(ColorWarn, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
        }
    )
}

@Composable
fun InputGrid(label: String, subLabel: String? = null, value: String, labelColor: Color = ColorLabel, onValueChange: (String) -> Unit) {
    var textFieldValue by remember(value) { mutableStateOf(TextFieldValue(value)) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1.6f)) {
            Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
            if (subLabel != null) Text(subLabel, color = labelColor.copy(alpha = 0.8f), fontSize = 10.sp)
        }
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it; onValueChange(it.text) },
            modifier = Modifier.weight(1f).height(52.dp).onFocusChanged { if (it.isFocused) textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg, focusedTextColor = ColorText, unfocusedTextColor = ColorText, focusedBorderColor = ColorPrimary, unfocusedBorderColor = ColorBorder),
            textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorText),
            singleLine = true
        )
    }
}

@Composable
fun ResItem(label: String, value: String, unit: String, borderColor: Color = ColorPrimary) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(ColorInputBg, RoundedCornerShape(8.dp))
            .clickable { copyToClipboard(context, value) }
            .drawBehind { drawLine(borderColor, Offset(0f, 0f), Offset(0f, size.height), 5.dp.toPx()) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = ColorLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = ColorText, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Text(unit, color = ColorLabel, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ColorLabel.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun CoordInputGrid(values: List<String>, onUpdate: (Int, String) -> Unit) {
    val labels = listOf("X", "Y", "Z", "W", "P", "R")
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..2).forEach { i -> CoordSingleInput(labels[i], values[i]) { onUpdate(i, it) } }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (3..5).forEach { i -> CoordSingleInput(labels[i], values[i]) { onUpdate(i, it) } }
        }
    }
}

@Composable
fun RowScope.CoordSingleInput(label: String, value: String, onValueChange: (String) -> Unit) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = value, selection = TextRange(value.length))
        }
    }
    Column(Modifier.weight(1f)) {
        Text(label, fontSize = 10.sp, color = ColorLabel, fontWeight = FontWeight.Black)
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it; onValueChange(it.text) },
            modifier = Modifier.height(50.dp).onFocusChanged {
                if (it.isFocused) textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg, focusedTextColor = ColorText, unfocusedTextColor = ColorText, focusedBorderColor = ColorPrimary, unfocusedBorderColor = ColorBorder),
            textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = ColorText),
            singleLine = true
        )
    }
}