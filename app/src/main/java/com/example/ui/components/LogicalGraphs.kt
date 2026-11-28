package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.engine.AnalyzedMove
import com.example.chess.engine.ColorOutcomeStats
import com.example.chess.engine.LogicalPlayerAnalytics
import com.example.chess.engine.MoveClassification
import com.example.chess.engine.SkillRadarMetric
import com.example.ui.theme.BlunderRed
import com.example.ui.theme.BrilliantCyan
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MistakeOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogicalSkillRadarCard(
    analytics: LogicalPlayerAnalytics,
    modifier: Modifier = Modifier
) {
    val metrics = analytics.radarMetrics
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val spokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("logical_radar_graph")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Hub, contentDescription = "Skill Logic Graph", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Chess Intelligence Radar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = analytics.summaryHeadline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (metrics.size >= 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.minDimension * 0.44f
                        val n = metrics.size
                        val angleStep = (2.0 * PI / n).toFloat()
                        val startAngle = (-PI / 2.0).toFloat()

                        // Concentric grid rings (25%, 50%, 75%, 100%)
                        listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { frac ->
                            val ringPath = Path()
                            for (i in 0 until n) {
                                val angle = startAngle + i * angleStep
                                val x = cx + cos(angle) * radius * frac
                                val y = cy + sin(angle) * radius * frac
                                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                            }
                            ringPath.close()
                            drawPath(ringPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
                        }

                        // Spokes
                        for (i in 0 until n) {
                            val angle = startAngle + i * angleStep
                            val ex = cx + cos(angle) * radius
                            val ey = cy + sin(angle) * radius
                            drawLine(spokeColor, Offset(cx, cy), Offset(ex, ey), strokeWidth = 1.dp.toPx())
                        }

                        // Player data polygon
                        val polyPath = Path()
                        val vertices = mutableListOf<Offset>()
                        metrics.forEachIndexed { idx, m ->
                            val frac = (m.score.coerceIn(10, 100) / 100f)
                            val angle = startAngle + idx * angleStep
                            val x = cx + cos(angle) * radius * frac
                            val y = cy + sin(angle) * radius * frac
                            val pt = Offset(x, y)
                            vertices.add(pt)
                            if (idx == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
                        }
                        polyPath.close()

                        drawPath(
                            path = polyPath,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    EmeraldPrimary.copy(alpha = 0.48f),
                                    ChampionshipGold.copy(alpha = 0.22f)
                                ),
                                center = Offset(cx, cy),
                                radius = radius
                            )
                        )
                        drawPath(
                            path = polyPath,
                            color = EmeraldPrimary,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        vertices.forEach { pt ->
                            drawCircle(color = ChampionshipGold, radius = 4.dp.toPx(), center = pt)
                            drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = pt)
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    metrics.forEach { m ->
                        SkillMetricBadge(metric = m)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillMetricBadge(metric: SkillRadarMetric) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${metric.score}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (metric.score >= 70) EmeraldPrimary else ChampionshipGold
            )
            Text(
                text = metric.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LogicalColorOutcomeCard(
    analytics: LogicalPlayerAnalytics,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("logical_color_outcome_graph")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("White vs Black Outcome Logic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (analytics.avgMovesPerGame > 0) {
                    Text(
                        text = "Avg ${analytics.avgMovesPerGame} moves",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ColorOutcomeBarRow(label = "White Pieces", stats = analytics.whiteStats)
            ColorOutcomeBarRow(label = "Black Pieces", stats = analytics.blackStats)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendDotLabel(color = EmeraldPrimary, label = "Win")
                LegendDotLabel(color = Color(0xFF64748B), label = "Draw")
                LegendDotLabel(color = BlunderRed.copy(alpha = 0.85f), label = "Loss")
            }
        }
    }
}

@Composable
private fun ColorOutcomeBarRow(label: String, stats: ColorOutcomeStats) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = if (stats.games == 0) "No finished games"
                else "${stats.games}g · ${stats.wins}W / ${stats.draws}D / ${stats.losses}L (${stats.winPct.roundToInt()}% W)",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (stats.games == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        } else {
            val wWeight = stats.winPct.coerceAtLeast(1f)
            val dWeight = stats.drawPct.coerceAtLeast(1f)
            val lWeight = stats.lossPct.coerceAtLeast(1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier.weight(wWeight).fillMaxHeight().background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (stats.winPct >= 16f) {
                        Text("${stats.winPct.roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
                Box(
                    modifier = Modifier.weight(dWeight).fillMaxHeight().background(Color(0xFF64748B)),
                    contentAlignment = Alignment.Center
                ) {
                    if (stats.drawPct >= 16f) {
                        Text("${stats.drawPct.roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
                Box(
                    modifier = Modifier.weight(lWeight).fillMaxHeight().background(BlunderRed.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (stats.lossPct >= 16f) {
                        Text("${stats.lossPct.roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDotLabel(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LogicalRatingProgressionCard(
    formatLabel: String,
    values: List<Int>,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember(values) { mutableStateOf<Int?>(null) }
    val lineColor = EmeraldPrimary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    val currentVal = values.lastOrNull() ?: 1200
    val startVal = values.firstOrNull() ?: currentVal
    val delta = currentVal - startVal
    val peakVal = values.maxOrNull() ?: currentVal

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("logical_progression_graph")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShowChart, contentDescription = null, tint = lineColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$formatLabel Curve (${values.size} pts)",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                val inspectedText = selectedIdx?.let { idx ->
                    values.getOrNull(idx)?.let { "Pt #${idx + 1}: $it" }
                } ?: "Peak $peakVal · ${if (delta >= 0) "+$delta" else "$delta"}"
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = inspectedText,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (delta >= 0) EmeraldPrimary else BlunderRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .pointerInput(values) {
                        detectTapGestures { offset ->
                            if (values.isNotEmpty() && size.width > 0) {
                                val norm = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val idx = (norm * (values.size - 1)).roundToInt().coerceIn(0, values.lastIndex)
                                selectedIdx = idx
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val pad = 12.dp.toPx()

                    for (i in 0..3) {
                        val y = pad + (h - 2 * pad) * (i / 3f)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                    }

                    if (values.isEmpty()) return@Canvas
                    val pts = if (values.size == 1) listOf(values[0], values[0]) else values
                    val minVal = (pts.min() - 20).toFloat()
                    val maxVal = (pts.max() + 20).coerceAtLeast(pts.min() + 40).toFloat()
                    val range = (maxVal - minVal).coerceAtLeast(1f)

                    val linePath = Path()
                    val fillPath = Path()
                    val offsets = mutableListOf<Offset>()

                    pts.forEachIndexed { idx, v ->
                        val x = pad + (w - 2 * pad) * (idx.toFloat() / (pts.size - 1).coerceAtLeast(1))
                        val normY = (v - minVal) / range
                        val y = (h - pad) - normY * (h - 2 * pad)
                        offsets.add(Offset(x, y))
                        if (idx == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, h - pad)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                        if (idx == pts.lastIndex) {
                            fillPath.lineTo(x, h - pad)
                            fillPath.close()
                        }
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    offsets.forEachIndexed { idx, pt ->
                        val isSel = selectedIdx == idx
                        drawCircle(
                            color = if (isSel) ChampionshipGold else lineColor,
                            radius = if (isSel) 5.5.dp.toPx() else 3.2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

private val MatchWinningMagenta = Color(0xFFEC4899)

fun isMatchWinningMove(
    move: AnalyzedMove,
    perspectiveSide: com.example.chess.engine.PieceColor = com.example.chess.engine.PieceColor.WHITE
): Boolean {
    val sign = if (perspectiveSide == com.example.chess.engine.PieceColor.WHITE) 1 else -1
    val beforePersp = move.evalBeforeCp * sign
    val afterPersp = move.evalAfterCp * sign
    val swing = afterPersp - beforePersp
    val isPerspectivePlayersMove = move.isWhite == (perspectiveSide == com.example.chess.engine.PieceColor.WHITE)
    val forcingMateForPlayer = move.mateAfter != null && (
        (perspectiveSide == com.example.chess.engine.PieceColor.WHITE && move.mateAfter > 0) ||
            (perspectiveSide == com.example.chess.engine.PieceColor.BLACK && move.mateAfter < 0)
        )
    if (!isPerspectivePlayersMove) return false
    return move.classification == MoveClassification.BRILLIANT ||
        forcingMateForPlayer ||
        (swing >= 140 && afterPersp >= 120) ||
        (move.classification == MoveClassification.BEST && swing >= 100 && afterPersp >= 140)
}

@Composable
fun LogicalEvalFlowGraph(
    analyzedMoves: List<AnalyzedMove>,
    currentPly: Int,
    onSelectPly: (Int) -> Unit,
    perspectiveSide: com.example.chess.engine.PieceColor = com.example.chess.engine.PieceColor.WHITE,
    playerLabel: String = if (perspectiveSide == com.example.chess.engine.PieceColor.WHITE) "White" else "Black",
    opponentLabel: String = if (perspectiveSide == com.example.chess.engine.PieceColor.WHITE) "Black" else "White",
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val sign = if (perspectiveSide == com.example.chess.engine.PieceColor.WHITE) 1 else -1
    val winningCount = remember(analyzedMoves, perspectiveSide) {
        analyzedMoves.count { isMatchWinningMove(it, perspectiveSide) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("logical_eval_flow_graph"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Move Comparison & Match-Winning Flow",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Move 1 → Endgame · Timer-Independent Position Power",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (winningCount > 0) {
                Surface(
                    color = MatchWinningMagenta.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$winningCount Match-Win",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampionshipGold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        val curMove = if (currentPly > 0) analyzedMoves.getOrNull(currentPly - 1) else analyzedMoves.lastOrNull()
        if (curMove != null) {
            val perspAfter = (curMove.evalAfterCp * sign) / 100.0
            val perspBefore = (curMove.evalBeforeCp * sign) / 100.0
            val delta = perspAfter - perspBefore
            val isWinMv = isMatchWinningMove(curMove, perspectiveSide)
            val isPlayerPly = curMove.isWhite == (perspectiveSide == com.example.chess.engine.PieceColor.WHITE)
            val moverTag = if (isPlayerPly) playerLabel else opponentLabel
            Surface(
                color = if (isWinMv) MatchWinningMagenta.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ply ${curMove.plyIndex + 1} ($moverTag): ${curMove.playedSan} vs Best ${curMove.bestSan}" +
                            if (isWinMv) " · MATCH-WINNING!" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isWinMv) ChampionshipGold else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${if (perspAfter >= 0) "+" else ""}${"%.1f".format(perspAfter)} (${if (delta >= 0) "+" else ""}${"%.1f".format(delta)})",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (isWinMv) MatchWinningMagenta else if (delta >= -0.2) EmeraldPrimary else BlunderRed
                    )
                }
            }
        }

        // Main Advantage Curve (Up = Powerful / Match-Winning Move, Down = Mistake / Opposition Advantage)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .pointerInput(analyzedMoves) {
                    detectTapGestures { offset ->
                        if (analyzedMoves.isNotEmpty() && size.width > 0) {
                            val norm = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val ply = (norm * (analyzedMoves.size - 1)).roundToInt() + 1
                            onSelectPly(ply.coerceIn(1, analyzedMoves.size))
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val pad = 10.dp.toPx()
                val midY = h / 2f

                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                if (analyzedMoves.isEmpty()) return@Canvas
                val values = analyzedMoves.map { (it.evalAfterCp * sign).coerceIn(-700, 700) }
                val pts = if (values.size == 1) listOf(values[0], values[0]) else values

                val linePath = Path()
                val aboveFill = Path()
                val belowFill = Path()
                aboveFill.moveTo(pad, midY)
                belowFill.moveTo(pad, midY)

                val nodeOffsets = mutableListOf<Offset>()
                pts.forEachIndexed { idx, cp ->
                    val x = pad + (w - 2 * pad) * (idx.toFloat() / (pts.size - 1).coerceAtLeast(1))
                    val norm = cp / 700f
                    val y = midY - norm * (midY - pad)
                    nodeOffsets.add(Offset(x, y))
                    if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                    aboveFill.lineTo(x, y.coerceAtMost(midY))
                    belowFill.lineTo(x, y.coerceAtLeast(midY))
                }
                val lastX = nodeOffsets.last().x
                aboveFill.lineTo(lastX, midY); aboveFill.close()
                belowFill.lineTo(lastX, midY); belowFill.close()

                drawPath(aboveFill, color = EmeraldPrimary.copy(alpha = 0.28f))
                drawPath(belowFill, color = BlunderRed.copy(alpha = 0.24f))
                drawPath(linePath, color = Color.White.copy(alpha = 0.92f), style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

                if (currentPly in 1..analyzedMoves.size) {
                    val curOffset = nodeOffsets[(currentPly - 1).coerceIn(0, nodeOffsets.lastIndex)]
                    drawLine(ChampionshipGold, Offset(curOffset.x, 0f), Offset(curOffset.x, h), strokeWidth = 1.5.dp.toPx())
                    drawCircle(ChampionshipGold, radius = 4.5.dp.toPx(), center = curOffset)
                }

                // Highlight Match-Winning moves (Gold/Magenta Diamond) + Blunders/Mistakes/Best
                analyzedMoves.forEachIndexed { idx, mv ->
                    val pt = nodeOffsets.getOrNull(idx) ?: return@forEachIndexed
                    if (isMatchWinningMove(mv, perspectiveSide)) {
                        drawCircle(MatchWinningMagenta.copy(alpha = 0.35f), radius = 8.5.dp.toPx(), center = pt)
                        val r = 5.5.dp.toPx()
                        val diamond = Path().apply {
                            moveTo(pt.x, pt.y - r)
                            lineTo(pt.x + r, pt.y)
                            lineTo(pt.x, pt.y + r)
                            lineTo(pt.x - r, pt.y)
                            close()
                        }
                        drawPath(diamond, color = ChampionshipGold)
                        drawPath(diamond, color = MatchWinningMagenta, style = Stroke(width = 1.6.dp.toPx()))
                    } else {
                        when (mv.classification) {
                            MoveClassification.BRILLIANT -> drawCircle(BrilliantCyan, radius = 4.dp.toPx(), center = pt)
                            MoveClassification.MISTAKE -> drawCircle(MistakeOrange, radius = 3.5.dp.toPx(), center = pt)
                            MoveClassification.BLUNDER -> drawCircle(BlunderRed, radius = 4.3.dp.toPx(), center = pt)
                            else -> Unit
                        }
                    }
                }
            }
        }

        // Per-Move Comparison Strip: Player (Up) vs Opposition (Down) from Move 1 to Endgame
        if (analyzedMoves.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .pointerInput(analyzedMoves) {
                        detectTapGestures { offset ->
                            if (analyzedMoves.isNotEmpty() && size.width > 0) {
                                val norm = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val ply = (norm * (analyzedMoves.size - 1)).roundToInt() + 1
                                onSelectPly(ply.coerceIn(1, analyzedMoves.size))
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f
                    val count = analyzedMoves.size.coerceAtLeast(1)
                    val slotW = w / count.toFloat()
                    val barW = (slotW * 0.72f).coerceIn(2.dp.toPx(), 12.dp.toPx())

                    drawLine(gridColor, Offset(0f, midY), Offset(w, midY), strokeWidth = 1.dp.toPx())

                    analyzedMoves.forEachIndexed { idx, mv ->
                        val isPlayerPly = mv.isWhite == (perspectiveSide == com.example.chess.engine.PieceColor.WHITE)
                        val isWin = isMatchWinningMove(mv, perspectiveSide)
                        val qualityFrac = (1f - (mv.cpLoss.coerceIn(0, 350) / 350f)).coerceIn(0.18f, 1f)
                        val barH = (midY - 3.dp.toPx()) * qualityFrac
                        val xLeft = idx * slotW + (slotW - barW) / 2f

                        val barColor = when {
                            isWin -> MatchWinningMagenta
                            mv.classification == MoveClassification.BLUNDER -> BlunderRed
                            mv.classification == MoveClassification.MISTAKE -> MistakeOrange
                            isPlayerPly -> EmeraldPrimary
                            else -> ElectricBlue
                        }
                        if (isPlayerPly) {
                            drawRect(
                                color = barColor,
                                topLeft = Offset(xLeft, midY - barH),
                                size = androidx.compose.ui.geometry.Size(barW, barH)
                            )
                        } else {
                            drawRect(
                                color = barColor,
                                topLeft = Offset(xLeft, midY),
                                size = androidx.compose.ui.geometry.Size(barW, barH)
                            )
                        }
                    }
                }
            }
        }

        // Color Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LegendDotLabel(color = ChampionshipGold, label = "Match-Win")
            LegendDotLabel(color = EmeraldPrimary, label = playerLabel)
            LegendDotLabel(color = ElectricBlue, label = opponentLabel)
            LegendDotLabel(color = MistakeOrange, label = "Mistake")
            LegendDotLabel(color = BlunderRed, label = "Blunder")
        }
    }
}
