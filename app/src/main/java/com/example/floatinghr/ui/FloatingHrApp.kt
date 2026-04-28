package com.example.floatinghr.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatinghr.data.SampleRepository
import com.example.floatinghr.model.AlertState
import com.example.floatinghr.model.HeartRateZone
import com.example.floatinghr.model.TrainingPlan
import com.example.floatinghr.model.defaultZones
import com.example.floatinghr.prediction.PacingDecision
import com.example.floatinghr.prediction.PacingPredictionEngine
import com.example.floatinghr.prediction.TrainingTelemetrySample
import com.example.floatinghr.service.FloatingHeartRateService
import com.example.floatinghr.service.HeartRateForegroundService
import kotlinx.coroutines.delay

private val DarkBackground = Color(0xFF050607)
private val CardBackground = Color(0xFF1D1D1F)
private val Cyan = Color(0xFF4FC3F7)
private val MutedText = Color(0xFF9A9A9F)
private val Success = Color(0xFF37C96B)
private val Danger = Color(0xFFFF453A)

@Composable
fun FloatingHrApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Training) }
    var running by remember { mutableStateOf(false) }
    val training = remember { SampleRepository.trainingPlans.first() }
    val context = LocalContext.current

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = DarkBackground,
            surface = CardBackground,
            primary = Cyan,
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
            if (running) {
                WorkoutScreen(training = training, onStop = {
                    running = false
                    context.stopService(Intent(context, FloatingHeartRateService::class.java))
                    context.stopService(Intent(context, HeartRateForegroundService::class.java))
                })
            } else {
                Scaffold(
                    containerColor = DarkBackground,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xEE111113)) {
                            AppTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(innerPadding)
                    ) {
                        when (selectedTab) {
                            AppTab.Training -> TrainingHome(scaffoldPadding = innerPadding, onStart = {
                                startMonitoringServices(context)
                                running = true
                            })
                            AppTab.History -> HistoryScreen(scaffoldPadding = innerPadding)
                            AppTab.Settings -> SettingsScreen(scaffoldPadding = innerPadding)
                        }
                    }
                }
            }
        }
    }
}

private fun startMonitoringServices(context: Context) {
    val foregroundIntent = Intent(context, HeartRateForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(foregroundIntent)
    } else {
        context.startService(foregroundIntent)
    }

    if (Settings.canDrawOverlays(context)) {
        context.startService(Intent(context, FloatingHeartRateService::class.java))
    } else {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private enum class AppTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Training("Training", Icons.Default.Sports),
    History("Verlauf", Icons.Default.History),
    Settings("Einstellungen", Icons.Default.Settings)
}

@Composable
private fun TrainingHome(scaffoldPadding: PaddingValues, onStart: () -> Unit) {
    val devices = remember { SampleRepository.devices }
    val training = remember { SampleRepository.trainingPlans.first() }
    var showPlans by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    val layoutDirection = LocalLayoutDirection.current
    val listPadding = combinedPadding(
        base = scaffoldPadding,
        horizontal = 18.dp,
        top = 18.dp,
        bottom = 120.dp
    )
    val ctaStartPadding = scaffoldPadding.calculateStartPadding(layoutDirection) + 26.dp
    val ctaEndPadding = scaffoldPadding.calculateEndPadding(layoutDirection) + 26.dp
    val ctaBottomPadding = scaffoldPadding.calculateBottomPadding() + 26.dp

    Box(
        Modifier
            .fillMaxSize()
            .consumeWindowInsets(scaffoldPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = listPadding,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(Modifier.height(28.dp))
                Text(
                    "Strap Zone",
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Training wählen und Gurt verbinden",
                    color = MutedText,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            item {
                AppCard {
                    Text("Trainingseinstellung", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱", color = MutedText, fontSize = 24.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Wähle ein Training zum", color = MutedText)
                            Text("Starten", color = MutedText)
                        }
                        Text(
                            "Wählen",
                            color = Cyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showPlans = true }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Success)
                        Spacer(Modifier.width(14.dp))
                        Text("Verbunden mit Polar H10 0FC4C438", color = Success, modifier = Modifier.weight(1f))
                        Text("✓", color = Success, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(62.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nach Geräten suchen", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            items(devices) { device ->
                DeviceCard(device.name, device.rssi)
            }
            item {
                Text(
                    "Tipp: Trage deinen Gurt (Hautkontakt) um ihn zu aktivieren",
                    color = MutedText,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = ctaStartPadding,
                    end = ctaEndPadding,
                    bottom = ctaBottomPadding
                )
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
        ) {
            Text("Training starten", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }

    if (showPlans) {
        TrainingListDialog(training, onDismiss = { showPlans = false }, onEdit = { showEditor = true })
    }
    if (showEditor) {
        TrainingEditorDialog(training, onDismiss = { showEditor = false })
    }
}

@Composable
private fun DeviceCard(name: String, rssi: Int) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF17303C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Cyan)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("▮▮▯  $rssi dBm", color = MutedText)
            }
            Text("›", color = MutedText, fontSize = 34.sp)
        }
    }
}

@Composable
private fun TrainingListDialog(training: TrainingPlan, onDismiss: () -> Unit, onEdit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Trainings", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                TrainingPlanCard(training, onEdit)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Segment")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
private fun TrainingPlanCard(training: TrainingPlan, onEdit: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(training.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("⏱ ${training.totalMinutes}h 0m   ☰ ${training.totalSegments} Segmente", color = MutedText)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    training.segments.map { it.zone }.distinct().forEach { ZoneChip(it) }
                }
            }
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF21495D)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Cyan)
            }
        }
    }
}

@Composable
private fun TrainingEditorDialog(training: TrainingPlan, onDismiss: () -> Unit) {
    var segmentDialog by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Training bearbeiten", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = training.name, onValueChange = {}, label = { Text("Name") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = false, onCheckedChange = {})
                    Text("Als freies Training nach Segmentende fortsetzen", color = MutedText)
                }
                Text("Gesamt: 1h 0m • 12 Segmente", color = MutedText, textAlign = TextAlign.Center)
                AppCard(
                    modifier = Modifier.border(1.dp, Cyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Text("↔  3x wiederholen", color = Cyan, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    training.segments.forEach { segment ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 9.dp)) {
                            Box(Modifier.width(4.dp).height(32.dp).background(segment.zone.color, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(22.dp))
                            Column {
                                Text(segment.zone.label, color = segment.zone.color, fontWeight = FontWeight.Bold)
                                Text("${segment.minutes}m", color = MutedText)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { segmentDialog = true }, modifier = Modifier.weight(1f)) {
                        Text("+ Segment")
                    }
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), enabled = false) {
                        Text("↵ Wiederholen")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Speichern", color = Cyan) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen", color = Cyan) } }
    )
    if (segmentDialog) {
        SegmentDialog(onDismiss = { segmentDialog = false })
    }
}

@Composable
private fun SegmentDialog(onDismiss: () -> Unit) {
    val zones = remember { defaultZones() }
    var selected by remember { mutableStateOf(zones[1]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Segment hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Zielzone", color = MutedText)
                ZoneSelector(zones = zones, selected = selected, onSelect = { selected = it })
                Text("Dauer", color = MutedText)
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Stepper("Min", 5)
                    Stepper("Sek", 0)
                }
                OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Bezeichnung (optional)") })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = Cyan) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen", color = Cyan) } }
    )
}

@Composable
private fun ZoneSelector(
    zones: List<HeartRateZone>,
    selected: HeartRateZone,
    onSelect: (HeartRateZone) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            zones.take(2),
            zones.drop(2).take(2),
            zones.drop(4)
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { zone ->
                    val active = selected == zone
                    Text(
                        zone.label,
                        color = if (active) Color.White else MutedText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) Color(0xFF777A80) else Color.Transparent)
                            .border(1.dp, Color(0xFF5D6066), RoundedCornerShape(8.dp))
                            .clickable { onSelect(zone) }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MutedText)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⊖", color = MutedText, fontSize = 24.sp)
            Text("$value", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Text("⊕", color = Color.White, fontSize = 24.sp)
        }
    }
}

@Composable
private fun combinedPadding(
    base: PaddingValues,
    horizontal: Dp = 0.dp,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = base.calculateStartPadding(layoutDirection) + horizontal,
        top = base.calculateTopPadding() + top,
        end = base.calculateEndPadding(layoutDirection) + horizontal,
        bottom = base.calculateBottomPadding() + bottom
    )
}

@Composable
private fun WorkoutScreen(training: TrainingPlan, onStop: () -> Unit) {
    var bpm by remember { mutableIntStateOf(127) }
    var cadence by remember { mutableIntStateOf(166) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    val zone = training.segments.first().zone
    val state = AlertState.fromBpm(bpm, zone)
    val predictionEngine = remember { PacingPredictionEngine() }
    val samples = remember {
        mutableStateOf(
            listOf(
                TrainingTelemetrySample(0, 113, 156),
                TrainingTelemetrySample(15, 116, 160),
                TrainingTelemetrySample(30, 119, 164),
                TrainingTelemetrySample(45, 122, 168),
                TrainingTelemetrySample(60, 125, 170),
                TrainingTelemetrySample(75, 127, 170)
            )
        )
    }
    val prediction = predictionEngine.predict(
        samples = samples.value,
        targetMinBpm = zone.minBpm,
        targetMaxBpm = zone.maxBpm
    )

    LaunchedEffect(paused) {
        while (!paused) {
            delay(1800)
            elapsedSeconds += 2
            cadence = (cadence + listOf(-1, 0, 1, 2).random()).coerceIn(150, 184)
            bpm = (bpm + listOf(0, 1, 1, 2, -1).random()).coerceIn(110, 134)
            samples.value = (samples.value + TrainingTelemetrySample(elapsedSeconds.toLong(), bpm, cadence)).takeLast(24)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E3A4D), DarkBackground)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹  00:19", color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text("Segment 1/${training.totalSegments}", color = Color.White.copy(alpha = 0.75f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("  🔔  ▸", color = Color.White.copy(alpha = 0.75f), fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            Spacer(Modifier.height(104.dp))
            HeartGauge(bpm = bpm)
            Text("$bpm", color = Color.White, fontSize = 104.sp, fontWeight = FontWeight.Bold)
            Text("BPM", color = MutedText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "${prediction.decision.icon} ${prediction.decision.label}",
                color = prediction.decision.color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(prediction.decision.color.copy(alpha = 0.18f))
                    .border(1.dp, prediction.decision.color, CircleShape)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text("Ziel: ${zone.label} (${zone.minBpm}-${zone.maxBpm} bpm)", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                prediction.message,
                color = if (prediction.decision == PacingDecision.SlowDownNow) Danger else MutedText,
                textAlign = TextAlign.Center
            )
            Text(
                "步頻 ${cadence} spm · 上升 ${"%.1f".format(prediction.heartRateSlopeBpmPerMinute)} bpm/min",
                color = MutedText
            )
            Text("Nächstes: 1m Z3 Tempo", color = MutedText)
            Spacer(Modifier.weight(1f))
            Text("14:41", color = Color.White.copy(alpha = 0.75f), fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth(0.86f).height(6.dp).clip(CircleShape).background(Color(0xFF2D3438))) {
                Box(Modifier.fillMaxWidth(0.02f).height(6.dp).background(Cyan))
            }
            Spacer(Modifier.height(74.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                OutlinedButton(
                    onClick = { paused = !paused },
                    modifier = Modifier.weight(1f).height(70.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (paused) "WEITER" else "PAUSE", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).height(70.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("STOPP", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun HeartGauge(bpm: Int) {
    Canvas(Modifier.size(270.dp, 160.dp)) {
        val stroke = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
        val arcSize = Size(size.width * 0.82f, size.width * 0.82f)
        val topLeft = Offset(size.width * 0.09f, size.height * 0.08f)
        drawArc(Color(0xFF294657), 180f, 180f, false, topLeft, arcSize, style = stroke)
        drawArc(Color(0xFF4385F5), 210f, 36f, false, topLeft, arcSize, style = stroke)
        drawArc(Color(0xFF116B5F), 246f, 54f, false, topLeft, arcSize, style = stroke)
        drawArc(Color(0xFF5E4A59), 300f, 60f, false, topLeft, arcSize, style = stroke)
        val angle = Math.toRadians((180 + ((bpm - 80).coerceIn(0, 100) * 1.8)).toDouble())
        val center = Offset(size.width / 2f, size.height * 1.02f)
        val needleLength = size.width * 0.46f
        val end = Offset(
            center.x + kotlin.math.cos(angle).toFloat() * needleLength,
            center.y + kotlin.math.sin(angle).toFloat() * needleLength
        )
        drawLine(Color.White, center, end, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color(0xFF2B2B2D), 18.dp.toPx(), center)
        drawCircle(Color(0xFF8A8A8E), 20.dp.toPx(), end)
        drawCircle(Color(0xFF4385F5), 13.dp.toPx(), end)
    }
}

private val PacingDecision.label: String
    get() = when (this) {
        PacingDecision.SpeedUp -> "加快步伐"
        PacingDecision.Maintain -> "Im Ziel"
        PacingDecision.SlowDownSoon -> "提前放慢"
        PacingDecision.SlowDownNow -> "立即放慢"
    }

private val PacingDecision.icon: String
    get() = when (this) {
        PacingDecision.SpeedUp -> "↗"
        PacingDecision.Maintain -> "✓"
        PacingDecision.SlowDownSoon -> "↘"
        PacingDecision.SlowDownNow -> "!"
    }

private val PacingDecision.color: Color
    get() = when (this) {
        PacingDecision.SpeedUp -> Cyan
        PacingDecision.Maintain -> Success
        PacingDecision.SlowDownSoon -> Color(0xFFFFB300)
        PacingDecision.SlowDownNow -> Danger
    }

@Composable
private fun HistoryScreen(scaffoldPadding: PaddingValues) {
    LazyColumn(
        contentPadding = combinedPadding(base = scaffoldPadding, top = 58.dp, bottom = 24.dp),
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(scaffoldPadding)
    ) {
        item {
            Text("Verlauf", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(34.dp))
        }
        items(SampleRepository.workouts) { workout ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(workout.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${workout.date} · ${workout.duration}", color = MutedText)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${workout.averageBpm}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("Durchschn. bpm", color = MutedText)
                }
            }
            HorizontalDivider(color = Color(0xFFBBBBBB), thickness = 1.dp)
        }
    }
}

@Composable
private fun SettingsScreen(scaffoldPadding: PaddingValues) {
    var customZones by remember { mutableStateOf(false) }
    var maxHr by remember { mutableIntStateOf(183) }
    LazyColumn(
        contentPadding = combinedPadding(
            base = scaffoldPadding,
            horizontal = 26.dp,
            top = 58.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(scaffoldPadding)
    ) {
        item {
            Text("Einstellungen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        item {
            Text("Training", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Passe die Berechnung deiner Zonen an.", color = MutedText)
        }
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Cyan, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("HF Max", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Wird zur Berechnung\nder Zonen verwendet", color = MutedText)
                    }
                    IconButton(onClick = { maxHr-- }) { Text("−", fontSize = 30.sp) }
                    Text("$maxHr", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { maxHr++ }) { Text("+", fontSize = 30.sp) }
                }
            }
        }
        item {
            Text("Zonen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Standard: % der HF Max. Aktiviere eigene Zonen für BPM-Bereiche.", color = MutedText)
        }
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Eigene Zonen", fontSize = 20.sp)
                        Text("Standard-Zonenbereiche\nüberschreiben", color = Color.White)
                    }
                    Switch(checked = customZones, onCheckedChange = { customZones = it })
                }
            }
        }
        item {
            AppCard {
                defaultZones(maxHr).forEach { zone ->
                    Text(zone.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${zone.minBpm}-${zone.maxBpm} bpm", color = MutedText)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
        item {
            AppCard {
                Text("Floating HR Bar", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("其他 App 上方顯示即時 BPM、Zone 與加快/放慢提示。", color = MutedText)
                Text("提示：視覺變色、聲音、震動同步觸發。", color = MutedText)
            }
        }
    }
}

@Composable
private fun ZoneChip(zone: HeartRateZone) {
    Text(
        zone.shortLabel,
        color = zone.color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(zone.color.copy(alpha = 0.16f))
            .border(1.dp, zone.color.copy(alpha = 0.7f), RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground, contentColor = Color.White)
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}
