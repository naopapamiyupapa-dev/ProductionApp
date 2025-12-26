package com.example.genba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dpS
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.math.*

// --- カラー定義 (Version 2.0: 現場視認性重視 + ロボット10台対応) ---
val ColorPrimary = Color(0xFF007AFF) // 青
val ColorBg = Color(0xFF000000)      // 黒
val ColorCard = Color(0xFF1C1C1E)    // グレー
val ColorText = Color(0xFFFFFFFF)    // 白
val ColorAccent = Color(0xFF32D74B)  // ライムグリーン (視認性最強)
val ColorWarn = Color(0xFFFF9F0A)    // オレンジ
val ColorDanger = Color(0xFFFF453A)  // 赤
val ColorLabel = Color(0xFFF2F2F7)   // 明るいグレー
val ColorBorder = Color(0xFF48484A)  // 枠線
val ColorInputBg = Color(0xFF2C2C2E) // 入力背景

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

// --- ロジック用データ型 ---
data class RobotPoint(
    val x: Double, val y: Double, val z: Double,
    val w: Double, val p: Double, val r: Double
)

data class MasterCoord(
    var name: String = "",
    var x: String = "0", var y: String = "0", var z: String = "0",
    var w: String = "0", var p: String = "0", var r: String = "0"
)

fun MasterCoord.toRobotPoint() = RobotPoint(
    x = x.toDoubleOrNull() ?: 0.0,
    y = y.toDoubleOrNull() ?: 0.0,
    z = z.toDoubleOrNull() ?: 0.0,
    w = w.toDoubleOrNull() ?: 0.0,
    p = p.toDoubleOrNull() ?: 0.0,
    r = r.toDoubleOrNull() ?: 0.0
)

// ロボット1台分のデータ構造
data class RobotMaster(
    var id: String = UUID.randomUUID().toString(),
    var robotName: String = "Robot",
    var ufSlots: List<MasterCoord> = List(10) { MasterCoord("UF$it") },
    var tfSlots: List<MasterCoord> = List(10) { MasterCoord("TF$it") }
)

data class ActionData(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var delay: String = "0",
    var duration: String = "1.0"
)

@Composable
fun MainScreen() {
    var currentTab by remember { mutableStateOf("prod") }
    // ロボット10台分のデータを初期化
    var robots by remember { 
        mutableStateOf(List(10) { i -> RobotMaster(robotName = "${i + 1}号機") }) 
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = ColorCard, tonalElevation = 8.dp) {
                TabItem("生産性", Icons.Default.Assessment, "prod", currentTab) { currentTab = it }
                TabItem("計画", Icons.Default.DateRange, "plan", currentTab) { currentTab = it }
                TabItem("時間", Icons.Default.Timer, "time", currentTab) { currentTab = it }
                TabItem("チャート", Icons.Default.Timeline, "chart", currentTab) { currentTab = it }
                TabItem("座標", Icons.Default.Place, "coord", currentTab) { currentTab = it }
                TabItem("マスタ", Icons.Default.List, "master", currentTab) { currentTab = it }
            }
        },
        containerColor = ColorBg
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                "prod" -> ProductivityPage()
                "plan" -> PlanPage()
                "time" -> TimeConverterPage()
                "chart" -> TimeChartPage()
                "coord" -> CoordPage(robots)
                "master" -> MasterPage(robots) { updatedRobots -> robots = updatedRobots }
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
        label = { Text(label, fontSize = 9.sp, fontWeight = if(current==id) FontWeight.ExtraBold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ColorPrimary,
            selectedTextColor = ColorPrimary,
            unselectedIconColor = Color(0xFF8E8E93),
            unselectedTextColor = Color(0xFF8E8E93),
            indicatorColor = Color.Transparent
        )
    )
}

@Composable
fun PageTitle(title: String) {
    Text(
        text = title, color = ColorAccent, fontSize = 20.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center
    )
}

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
            onValueChange = { 
                textFieldValue = it
                onValueChange(it.text)
            },
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .onFocusChanged { if (it.isFocused) textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg,
                focusedTextColor = ColorText, unfocusedTextColor = ColorText,
                focusedBorderColor = ColorPrimary, unfocusedBorderColor = ColorBorder
            ),
            textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorText),
            singleLine = true
        )
    }
}

@Composable
fun ResItem(label: String, value: String, unit: String, borderColor: Color = ColorPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(ColorInputBg, RoundedCornerShape(8.dp))
            .drawBehind {
                drawLine(borderColor, Offset(0f, 0f), Offset(0f, size.height), 5.dp.toPx())
            }.padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = ColorLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = ColorText, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text(unit, color = ColorLabel, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

// --- 1. 生産性ページ ---
@Composable
fun ProductivityPage() {
    var t1 by remember { mutableStateOf("30") }; var t2 by remember { mutableStateOf("85") }; var t3 by remember { mutableStateOf("8") }
    var o1 by remember { mutableStateOf("480") }; var o2 by remember { mutableStateOf("420") }; var o3 by remember { mutableStateOf("30") }
    var o4 by remember { mutableStateOf("700") }; var o5 by remember { mutableStateOf("680") }
    var workerNum by remember { mutableStateOf("1") }

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
        PageTitle("生産性・効率 (Productivity/OEE)")
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
    var p1 by remember { mutableStateOf("18000") }; var p2 by remember { mutableStateOf("20") }
    var p3 by remember { mutableStateOf("16") }; var p4 by remember { mutableStateOf("80") }
    val target = p1.toDoubleOrNull() ?: 0.0; val days = p2.toDoubleOrNull() ?: 0.0
    val hours = p3.toDoubleOrNull() ?: 0.0; val rate = (p4.toDoubleOrNull() ?: 0.0) / 100.0
    val maxD = if (days > 0) target / days else 0.0; val realH = hours * rate
    val resPH = if (maxD > 0) (hours * 3600.0 * rate) / maxD else 0.0

    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
        PageTitle("出来高計画 (Production Plan)")
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
    var totalSecs by remember { mutableStateOf(0.0) }
    val units = listOf(
        Triple("日 (Days)", 86400.0, ColorAccent),
        Triple("時間 (Hours)", 3600.0, ColorAccent),
        Triple("分 (Minutes)", 60.0, ColorAccent),
        Triple("秒 (Seconds)", 1.0, ColorAccent),
        Triple("ミリ秒 (ms)", 0.001, ColorAccent),
        Triple("マイクロ秒 (μs)", 0.000001, ColorAccent)
    )
    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
        PageTitle("時間変換 (Time Converter Ultra)")
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
        PageTitle("タイムチャート (Time Chart)")
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
                Button(onClick = { actions = actions + ActionData(name = "動作${actions.size+1}") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)) { Text("＋ 動作追加", fontWeight = FontWeight.Black, color = ColorBg) }
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

@Composable
fun CoordPage(robots: List<RobotMaster>) {
    var x1 by remember { mutableStateOf("500") }; var y1 by remember { mutableStateOf("0") }; var z1 by remember { mutableStateOf("500") }
    var w1 by remember { mutableStateOf("0") }; var p1 by remember { mutableStateOf("-90") }; var r1 by remember { mutableStateOf("0") }
    var x2 by remember { mutableStateOf("") }; var y2 by remember { mutableStateOf("") }; var z2 by remember { mutableStateOf("") }
    var w2 by remember { mutableStateOf("") }; var p2 by remember { mutableStateOf("") }; var r2 by remember { mutableStateOf("") }
    var splitMode by remember { mutableStateOf("dist") }
    var calcValue by remember { mutableStateOf("20") }; var points by remember { mutableStateOf(listOf<List<String>>()) }
    var offDist by remember { mutableStateOf("50") }

    // どのロボットのマスタデータを参照するか
    var selectedRobotIdx by remember { mutableStateOf(0) }
    var showRobotMenu by remember { mutableStateOf(false) }

    // UF/TFスロット選択
    var selectedUfIdx by remember { mutableStateOf(0) }
    var selectedTfIdx by remember { mutableStateOf(0) }
    var showUfMenu by remember { mutableStateOf(false) }
    var showTfMenu by remember { mutableStateOf(false) }
    
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
        val p1_w = w1.toDoubleOrNull() ?: 0.0; val p1_p = p1.toDoubleOrNull() ?: 0.0
        
        val robot = robots[selectedRobotIdx]
        val master = if (mode == "user") robot.ufSlots[selectedUfIdx].toRobotPoint() else robot.tfSlots[selectedTfIdx].toRobotPoint()

        if (mode == "user") {
            x2 = (p1_x + master.x).toString(); y2 = (p1_y + master.y).toString(); z2 = (p1_z + master.z + d).toString()
        } else {
            val rw = p1_w * PI / 180.0; val rp = p1_p * PI / 180.0; val totalMove = master.z + d
            x2 = (p1_x + master.x + sin(rp) * totalMove).toString(); y2 = (p1_y + master.y - sin(rw) * cos(rp) * totalMove).toString(); z2 = (p1_z + cos(rw) * cos(rp) * totalMove).toString()
        }
        w2 = w1; p2 = p1; r2 = r1; calc()
    }

    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
        PageTitle("座標分割・逃げ (Split/Offset)")
        
        // ロボット選択ドロップダウン (トップに配置)
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Button(onClick = { showRobotMenu = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg)) {
                Text("参照ロボット: ${robots[selectedRobotIdx].robotName} ▼", color = ColorAccent, fontWeight = FontWeight.Black)
            }
            DropdownMenu(expanded = showRobotMenu, onDismissRequest = { showRobotMenu = false }) {
                robots.forEachIndexed { i, robot ->
                    DropdownMenuItem(text = { Text("${i + 1}: ${robot.robotName}") }, onClick = { selectedRobotIdx = i; showRobotMenu = false })
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { x1=""; y1=""; z1=""; w1=""; p1=""; r1=""; x2=""; y2=""; z2=""; w2=""; p2=""; r2="" }) { Text("座標クリア ✕", color = ColorDanger, fontWeight = FontWeight.Bold) }
        }
        
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                // スロット読込
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        Button(onClick = { showUfMenu = true }, modifier = Modifier.fillMaxWidth().height(45.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg)) {
                            Text("UF$selectedUfIdx 読込 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorText)
                        }
                        DropdownMenu(expanded = showUfMenu, onDismissRequest = { showUfMenu = false }) {
                            robots[selectedRobotIdx].ufSlots.forEachIndexed { i, d ->
                                DropdownMenuItem(text = { Text("Slot $i: ${d.name}") }, onClick = { x1=d.x; y1=d.y; z1=d.z; w1=d.w; p1=d.p; r1=d.r; selectedUfIdx=i; showUfMenu = false })
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        Button(onClick = { showTfMenu = true }, modifier = Modifier.fillMaxWidth().height(45.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg)) {
                            Text("TF$selectedTfIdx 読込 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorText)
                        }
                        DropdownMenu(expanded = showTfMenu, onDismissRequest = { showTfMenu = false }) {
                            robots[selectedRobotIdx].tfSlots.forEachIndexed { i, d ->
                                DropdownMenuItem(text = { Text("Slot $i: ${d.name}") }, onClick = { x1=d.x; y1=d.y; z1=d.z; w1=d.w; p1=d.p; r1=d.r; selectedTfIdx=i; showTfMenu = false })
                            }
                        }
                    }
                }
                
                Text("始点 P1", color = ColorText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                CoordInputGrid(listOf(x1,y1,z1,w1,p1,r1)) { i,v -> when(i){0->x1=v;1->y1=v;2->z1=v;3->w1=v;4->p1=v;5->r1=v} }
                
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).background(Color(0xFF252A30), RoundedCornerShape(12.dp)).border(2.dp, ColorPrimary, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Column {
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
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun CoordInputGrid(values: List<String>, onUpdate: (Int, String) -> Unit) {
    val labels = listOf("X","Y","Z","W","P","R")
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..2).forEach { i ->
                Column(Modifier.weight(1f)) {
                    Text(labels[i], fontSize = 10.sp, color = ColorLabel, fontWeight = FontWeight.Black)
                    var textFieldValue by remember(values[i]) { mutableStateOf(TextFieldValue(values[i])) }
                    OutlinedTextField(value = textFieldValue, onValueChange = { textFieldValue = it; onUpdate(i, it.text) }, modifier = Modifier.height(50.dp).onFocusChanged { if (it.isFocused) textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg, focusedTextColor = ColorText, unfocusedTextColor = ColorText), textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), singleLine = true)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (3..5).forEach { i ->
                Column(Modifier.weight(1f)) {
                    Text(labels[i], fontSize = 10.sp, color = ColorLabel, fontWeight = FontWeight.Black)
                    var textFieldValue by remember(values[i]) { mutableStateOf(TextFieldValue(values[i])) }
                    OutlinedTextField(value = textFieldValue, onValueChange = { textFieldValue = it; onUpdate(i, it.text) }, modifier = Modifier.height(50.dp).onFocusChanged { if (it.isFocused) textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg, focusedTextColor = ColorText, unfocusedTextColor = ColorText), textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), singleLine = true)
                }
            }
        }
    }
}

@Composable
fun MasterPage(robots: List<RobotMaster>, onUpdate: (List<RobotMaster>) -> Unit) {
    var selectedRobotIdx by remember { mutableStateOf(0) }
    var selectedSlotIdx by remember { mutableStateOf(0) }
    var showRobotMenu by remember { mutableStateOf(false) }

    val robot = robots[selectedRobotIdx]
    var mRobotName by remember(selectedRobotIdx) { mutableStateOf(robot.robotName) }
    
    // スロット編集用
    var mSlotName by remember(selectedRobotIdx, selectedSlotIdx) { 
        mutableStateOf(robot.ufSlots[selectedSlotIdx].name) 
    }
    var mx by remember(selectedRobotIdx, selectedSlotIdx) { mutableStateOf(robot.ufSlots[selectedSlotIdx].x) }
    var my by remember(selectedRobotIdx, selectedSlotIdx) { mutableStateOf(robot.ufSlots[selectedSlotIdx].y) }
    var mz by remember(selectedRobotIdx, selectedSlotIdx) { mutableStateOf(robot.ufSlots[selectedSlotIdx].z) }
    var mw by remember(selectedRobotIdx, selectedSlotIdx) { mutableStateOf(robot.ufSlots[selectedSlotIdx].w) }
    var mp by remember(selectedRobotIdx, selectedSlotIdx) { mutableStateOf(robot.ufSlots[selectedSlotIdx].p) }
    var mr by remember(selectedRobotIdx, selectedSlotIdx) { mutableStateOf(robot.ufSlots[selectedSlotIdx].r) }

    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
        PageTitle("座標マスタ (ロボット10台対応)")
        
        // ロボット選択 & 名称設定
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard), modifier = Modifier.padding(bottom = 12.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { showRobotMenu = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorInputBg)) {
                        Text("編集ロボット: ${robot.robotName} ▼", color = ColorAccent, fontWeight = FontWeight.Black)
                    }
                    DropdownMenu(expanded = showRobotMenu, onDismissRequest = { showRobotMenu = false }) {
                        robots.forEachIndexed { i, r ->
                            DropdownMenuItem(text = { Text("${i + 1}: ${r.robotName}") }, onClick = { selectedRobotIdx = i; showRobotMenu = false })
                        }
                    }
                }
                
                TextField(
                    value = mRobotName, 
                    onValueChange = { 
                        mRobotName = it
                        val newRobots = robots.toMutableList()
                        newRobots[selectedRobotIdx] = newRobots[selectedRobotIdx].copy(robotName = it)
                        onUpdate(newRobots)
                    }, 
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("ロボット名称 (例: 1号機)", color = ColorLabel) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg)
                )
            }
        }

        // スロット編集
        Card(colors = CardDefaults.cardColors(containerColor = ColorCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Slot番号", Modifier.weight(1f), fontSize = 13.sp, color = ColorLabel, fontWeight = FontWeight.ExtraBold)
                    (0..9).forEach { i ->
                        Box(Modifier.size(34.dp).padding(2.dp).background(if(selectedSlotIdx==i) ColorPrimary else ColorInputBg, RoundedCornerShape(6.dp)).clickable { selectedSlotIdx=i }, contentAlignment = Alignment.Center) {
                            Text(i.toString(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = ColorText)
                        }
                    }
                }
                
                TextField(value = mSlotName, onValueChange = { mSlotName = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), label = { Text("地点名称をここに入力", color = ColorLabel, fontWeight = FontWeight.Bold) }, colors = TextFieldDefaults.colors(focusedContainerColor = ColorInputBg, unfocusedContainerColor = ColorInputBg))
                
                CoordInputGrid(listOf(mx,my,mz,mw,mp,mr)) { i,v -> when(i){0->mx=v;1->my=v;2->mz=v;3->mw=v;4->mp=v;5->mr=v} }
                
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { 
                            val newRobots = robots.toMutableList()
                            val newUf = newRobots[selectedRobotIdx].ufSlots.toMutableList()
                            newUf[selectedSlotIdx] = MasterCoord(mSlotName, mx, my, mz, mw, mp, mr)
                            newRobots[selectedRobotIdx] = newRobots[selectedRobotIdx].copy(ufSlots = newUf)
                            onUpdate(newRobots)
                        }, 
                        Modifier.weight(1f).height(52.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)
                    ) { Text("UFに保存", fontWeight = FontWeight.Black, color = ColorBg) }
                    
                    Button(
                        onClick = { 
                            val newRobots = robots.toMutableList()
                            val newTf = newRobots[selectedRobotIdx].tfSlots.toMutableList()
                            newTf[selectedSlotIdx] = MasterCoord(mSlotName, mx, my, mz, mw, mp, mr)
                            newRobots[selectedRobotIdx] = newRobots[selectedRobotIdx].copy(tfSlots = newTf)
                            onUpdate(newRobots)
                        }, 
                        Modifier.weight(1f).height(52.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)
                    ) { Text("TFに保存", fontWeight = FontWeight.Black, color = ColorText) }
                }
            }
        }
        
        SectionTitle("登録済みサマリ (${robot.robotName})")
        robot.ufSlots.forEachIndexed { i, d ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(ColorCard, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Text("Slot $i:", Modifier.width(55.dp), fontSize = 11.sp, color = ColorAccent, fontWeight = FontWeight.Black)
                Text("UF [${d.name}] / TF [${robot.tfSlots[i].name}]", fontSize = 11.sp, color = ColorText, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(text = "Version 2.0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = ColorLabel, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(60.dp))
    }
}
