package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RatingLineChart(
    values: List<Int>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pad = 12.dp.toPx()

        // Horizontal grid lines
        for (i in 0..3) {
            val y = pad + (h - 2 * pad) * (i / 3f)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }

        if (values.isEmpty()) return@Canvas
        val pts = if (values.size == 1) listOf(values[0], values[0]) else values
        val minVal = (pts.min() - 25).toFloat()
        val maxVal = (pts.max() + 25).coerceAtLeast(pts.min() + 50).toFloat()
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val linePath = Path()
        val fillPath = Path()

        pts.forEachIndexed { idx, v ->
            val x = pad + (w - 2 * pad) * (idx.toFloat() / (pts.size - 1).coerceAtLeast(1))
            val normY = (v - minVal) / range
            val y = (h - pad) - normY * (h - 2 * pad)
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

        pts.forEachIndexed { idx, v ->
            val x = pad + (w - 2 * pad) * (idx.toFloat() / (pts.size - 1).coerceAtLeast(1))
            val normY = (v - minVal) / range
            val y = (h - pad) - normY * (h - 2 * pad)
            drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(x, y))
        }
    }
}
