package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chess.engine.Piece
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.ui.theme.PieceStyle

@Composable
fun ChessPieceIcon(
    piece: Piece,
    style: PieceStyle,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val (fillPrimary, fillSecondary, strokeColor, detailColor) = piecePalette(piece.color, style)
            val w = size.width
            val h = size.height
            val strokeWidth = (w * 0.045f).coerceAtLeast(1.2f)

            val fillBrush = Brush.linearGradient(
                colors = listOf(fillPrimary, fillSecondary),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            when (piece.type) {
                PieceType.PAWN -> drawPawn(w, h, fillBrush, strokeColor, detailColor, strokeWidth, style)
                PieceType.ROOK -> drawRook(w, h, fillBrush, strokeColor, detailColor, strokeWidth, style)
                PieceType.KNIGHT -> drawKnight(w, h, fillBrush, strokeColor, detailColor, strokeWidth, style)
                PieceType.BISHOP -> drawBishop(w, h, fillBrush, strokeColor, detailColor, strokeWidth, style)
                PieceType.QUEEN -> drawQueen(w, h, fillBrush, strokeColor, detailColor, strokeWidth, style)
                PieceType.KING -> drawKing(w, h, fillBrush, strokeColor, detailColor, strokeWidth, style)
            }
        }
    }
}

private data class PieceColors(
    val primary: Color,
    val secondary: Color,
    val stroke: Color,
    val detail: Color
)

private fun piecePalette(color: PieceColor, style: PieceStyle): PieceColors = when (style) {
    PieceStyle.CLASSIC_STAUNTON -> if (color == PieceColor.WHITE) {
        PieceColors(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF1E293B), Color(0xFF334155))
    } else {
        PieceColors(Color(0xFF334155), Color(0xFF0F172A), Color(0xFFCBD5E1), Color(0xFFE2E8F0))
    }
    PieceStyle.NEO_GEOMETRIC -> if (color == PieceColor.WHITE) {
        PieceColors(Color(0xFFF0FDF4), Color(0xFFA7F3D0), Color(0xFF064E3B), Color(0xFF047857))
    } else {
        PieceColors(Color(0xFF1E3A8A), Color(0xFF0F172A), Color(0xFF93C5FD), Color(0xFF60A5FA))
    }
    PieceStyle.ROYAL_GOLD -> if (color == PieceColor.WHITE) {
        PieceColors(Color(0xFFFEF3C7), Color(0xFFF59E0B), Color(0xFF451A03), Color(0xFF78350F))
    } else {
        PieceColors(Color(0xFF27272A), Color(0xFF09090B), Color(0xFFFBBF24), Color(0xFFF59E0B))
    }
    PieceStyle.TOURNAMENT_CRYSTAL -> if (color == PieceColor.WHITE) {
        PieceColors(Color(0xFFECFEFF), Color(0xFFA5F3FC), Color(0xFF0E7490), Color(0xFF06B6D4))
    } else {
        PieceColors(Color(0xFF4C1D95), Color(0xFF1E1B4B), Color(0xFFE9D5FF), Color(0xFFC084FC))
    }
}

private fun DrawScope.drawPawn(
    w: Float, h: Float, brush: Brush, stroke: Color, detail: Color, sw: Float, style: PieceStyle
) {
    // Base
    drawRoundRect(
        brush = brush,
        topLeft = Offset(w * 0.24f, h * 0.78f),
        size = Size(w * 0.52f, h * 0.12f),
        cornerRadius = CornerRadius(w * 0.05f)
    )
    drawRoundRect(
        color = stroke,
        topLeft = Offset(w * 0.24f, h * 0.78f),
        size = Size(w * 0.52f, h * 0.12f),
        cornerRadius = CornerRadius(w * 0.05f),
        style = Stroke(sw)
    )
    // Neck & Stem
    val body = Path().apply {
        moveTo(w * 0.30f, h * 0.78f)
        cubicTo(w * 0.36f, h * 0.62f, w * 0.41f, h * 0.52f, w * 0.40f, h * 0.44f)
        lineTo(w * 0.60f, h * 0.44f)
        cubicTo(w * 0.59f, h * 0.52f, w * 0.64f, h * 0.62f, w * 0.70f, h * 0.78f)
        close()
    }
    drawPath(body, brush)
    drawPath(body, stroke, style = Stroke(sw, join = StrokeJoin.Round))

    // Collar
    drawRoundRect(
        brush = brush,
        topLeft = Offset(w * 0.34f, h * 0.40f),
        size = Size(w * 0.32f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.03f)
    )
    drawRoundRect(
        color = stroke,
        topLeft = Offset(w * 0.34f, h * 0.40f),
        size = Size(w * 0.32f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.03f),
        style = Stroke(sw * 0.85f)
    )

    // Head
    val headRadius = if (style == PieceStyle.NEO_GEOMETRIC) w * 0.14f else w * 0.155f
    drawCircle(brush = brush, radius = headRadius, center = Offset(w * 0.5f, h * 0.27f))
    drawCircle(color = stroke, radius = headRadius, center = Offset(w * 0.5f, h * 0.27f), style = Stroke(sw))
}

private fun DrawScope.drawRook(
    w: Float, h: Float, brush: Brush, stroke: Color, detail: Color, sw: Float, style: PieceStyle
) {
    val path = Path().apply {
        moveTo(w * 0.22f, h * 0.88f)
        lineTo(w * 0.78f, h * 0.88f)
        lineTo(w * 0.75f, h * 0.76f)
        lineTo(w * 0.67f, h * 0.73f)
        lineTo(w * 0.65f, h * 0.37f)
        lineTo(w * 0.74f, h * 0.31f)
        lineTo(w * 0.74f, h * 0.16f)
        lineTo(w * 0.62f, h * 0.16f)
        lineTo(w * 0.62f, h * 0.23f)
        lineTo(w * 0.55f, h * 0.23f)
        lineTo(w * 0.55f, h * 0.16f)
        lineTo(w * 0.45f, h * 0.16f)
        lineTo(w * 0.45f, h * 0.23f)
        lineTo(w * 0.38f, h * 0.23f)
        lineTo(w * 0.38f, h * 0.16f)
        lineTo(w * 0.26f, h * 0.16f)
        lineTo(w * 0.26f, h * 0.31f)
        lineTo(w * 0.35f, h * 0.37f)
        lineTo(w * 0.33f, h * 0.73f)
        lineTo(w * 0.25f, h * 0.76f)
        close()
    }
    drawPath(path, brush)
    drawPath(path, stroke, style = Stroke(sw, join = StrokeJoin.Round))
    drawLine(detail, Offset(w * 0.33f, h * 0.74f), Offset(w * 0.67f, h * 0.74f), strokeWidth = sw * 0.85f)
    drawLine(detail, Offset(w * 0.35f, h * 0.37f), Offset(w * 0.65f, h * 0.37f), strokeWidth = sw * 0.85f)
}

private fun DrawScope.drawKnight(
    w: Float, h: Float, brush: Brush, stroke: Color, detail: Color, sw: Float, style: PieceStyle
) {
    val path = Path().apply {
        moveTo(w * 0.22f, h * 0.88f)
        lineTo(w * 0.78f, h * 0.88f)
        lineTo(w * 0.75f, h * 0.76f)
        cubicTo(w * 0.74f, h * 0.50f, w * 0.72f, h * 0.28f, w * 0.54f, h * 0.18f)
        lineTo(w * 0.50f, h * 0.11f)
        lineTo(w * 0.43f, h * 0.17f)
        lineTo(w * 0.37f, h * 0.12f)
        lineTo(w * 0.35f, h * 0.22f)
        cubicTo(w * 0.25f, h * 0.31f, w * 0.19f, h * 0.42f, w * 0.22f, h * 0.50f)
        cubicTo(w * 0.25f, h * 0.55f, w * 0.33f, h * 0.52f, w * 0.39f, h * 0.48f)
        cubicTo(w * 0.46f, h * 0.46f, w * 0.49f, h * 0.54f, w * 0.38f, h * 0.64f)
        lineTo(w * 0.28f, h * 0.76f)
        close()
    }
    drawPath(path, brush)
    drawPath(path, stroke, style = Stroke(sw, join = StrokeJoin.Round))
    drawCircle(detail, radius = w * 0.032f, center = Offset(w * 0.37f, h * 0.31f))
    drawLine(detail, Offset(w * 0.27f, h * 0.78f), Offset(w * 0.73f, h * 0.78f), strokeWidth = sw * 0.8f)
}

private fun DrawScope.drawBishop(
    w: Float, h: Float, brush: Brush, stroke: Color, detail: Color, sw: Float, style: PieceStyle
) {
    // Base
    val base = Path().apply {
        moveTo(w * 0.22f, h * 0.88f)
        lineTo(w * 0.78f, h * 0.88f)
        lineTo(w * 0.72f, h * 0.76f)
        lineTo(w * 0.28f, h * 0.76f)
        close()
    }
    drawPath(base, brush)
    drawPath(base, stroke, style = Stroke(sw, join = StrokeJoin.Round))

    // Mitre body
    val body = Path().apply {
        moveTo(w * 0.31f, h * 0.76f)
        cubicTo(w * 0.34f, h * 0.62f, w * 0.29f, h * 0.42f, w * 0.50f, h * 0.22f)
        cubicTo(w * 0.71f, h * 0.42f, w * 0.66f, h * 0.62f, w * 0.69f, h * 0.76f)
        close()
    }
    drawPath(body, brush)
    drawPath(body, stroke, style = Stroke(sw, join = StrokeJoin.Round))

    // Top finial orb
    drawCircle(brush = brush, radius = w * 0.065f, center = Offset(w * 0.50f, h * 0.16f))
    drawCircle(color = stroke, radius = w * 0.065f, center = Offset(w * 0.50f, h * 0.16f), style = Stroke(sw * 0.85f))

    // Cross / Mitre slash
    drawLine(detail, Offset(w * 0.44f, h * 0.48f), Offset(w * 0.56f, h * 0.36f), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(detail, Offset(w * 0.44f, h * 0.42f), Offset(w * 0.56f, h * 0.42f), strokeWidth = sw * 0.85f, cap = StrokeCap.Round)
}

private fun DrawScope.drawQueen(
    w: Float, h: Float, brush: Brush, stroke: Color, detail: Color, sw: Float, style: PieceStyle
) {
    val body = Path().apply {
        moveTo(w * 0.22f, h * 0.88f)
        lineTo(w * 0.78f, h * 0.88f)
        lineTo(w * 0.74f, h * 0.75f)
        lineTo(w * 0.84f, h * 0.30f)
        lineTo(w * 0.66f, h * 0.48f)
        lineTo(w * 0.62f, h * 0.22f)
        lineTo(w * 0.50f, h * 0.44f)
        lineTo(w * 0.38f, h * 0.22f)
        lineTo(w * 0.34f, h * 0.48f)
        lineTo(w * 0.16f, h * 0.30f)
        lineTo(w * 0.26f, h * 0.75f)
        close()
    }
    drawPath(body, brush)
    drawPath(body, stroke, style = Stroke(sw, join = StrokeJoin.Round))

    val tips = listOf(
        Offset(w * 0.16f, h * 0.26f),
        Offset(w * 0.38f, h * 0.18f),
        Offset(w * 0.62f, h * 0.18f),
        Offset(w * 0.84f, h * 0.26f),
        Offset(w * 0.50f, h * 0.14f)
    )
    tips.dropLast(1).forEach { pt ->
        drawCircle(brush = brush, radius = w * 0.045f, center = pt)
        drawCircle(color = stroke, radius = w * 0.045f, center = pt, style = Stroke(sw * 0.8f))
    }
    drawLine(detail, Offset(w * 0.27f, h * 0.76f), Offset(w * 0.73f, h * 0.76f), strokeWidth = sw * 0.85f)
}

private fun DrawScope.drawKing(
    w: Float, h: Float, brush: Brush, stroke: Color, detail: Color, sw: Float, style: PieceStyle
) {
    // Crown cross
    drawLine(stroke, Offset(w * 0.50f, h * 0.10f), Offset(w * 0.50f, h * 0.27f), strokeWidth = sw * 1.4f, cap = StrokeCap.Round)
    drawLine(stroke, Offset(w * 0.41f, h * 0.17f), Offset(w * 0.59f, h * 0.17f), strokeWidth = sw * 1.3f, cap = StrokeCap.Round)
    drawLine(detail, Offset(w * 0.50f, h * 0.11f), Offset(w * 0.50f, h * 0.26f), strokeWidth = sw * 0.7f, cap = StrokeCap.Round)
    drawLine(detail, Offset(w * 0.42f, h * 0.17f), Offset(w * 0.58f, h * 0.17f), strokeWidth = sw * 0.7f, cap = StrokeCap.Round)

    val path = Path().apply {
        moveTo(w * 0.22f, h * 0.88f)
        lineTo(w * 0.78f, h * 0.88f)
        lineTo(w * 0.74f, h * 0.75f)
        cubicTo(w * 0.85f, h * 0.52f, w * 0.82f, h * 0.34f, w * 0.67f, h * 0.32f)
        cubicTo(w * 0.59f, h * 0.31f, w * 0.54f, h * 0.38f, w * 0.50f, h * 0.42f)
        cubicTo(w * 0.46f, h * 0.38f, w * 0.41f, h * 0.31f, w * 0.33f, h * 0.32f)
        cubicTo(w * 0.18f, h * 0.34f, w * 0.15f, h * 0.52f, w * 0.26f, h * 0.75f)
        close()
    }
    drawPath(path, brush)
    drawPath(path, stroke, style = Stroke(sw, join = StrokeJoin.Round))

    // Center royal crest line
    drawLine(detail, Offset(w * 0.50f, h * 0.43f), Offset(w * 0.50f, h * 0.72f), strokeWidth = sw * 0.8f, cap = StrokeCap.Round)
    drawLine(detail, Offset(w * 0.27f, h * 0.76f), Offset(w * 0.73f, h * 0.76f), strokeWidth = sw * 0.85f)
}
