package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.Move
import com.example.chess.engine.Piece
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import com.example.chess.engine.Square
import com.example.ui.theme.BoardPatternStyle
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PieceStyle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CastleDirectionGuide(
    val fromKingSq: Int,
    val toKingSq: Int,
    val fromRookSq: Int,
    val toRookSq: Int,
    val isKingside: Boolean,
    val isCurrentlyLegal: Boolean
)

@Composable
fun ChessBoardView(
    board: ChessBoard,
    boardTheme: BoardTheme,
    pieceStyle: PieceStyle,
    flipped: Boolean,
    showCoordinates: Boolean,
    selectedSquare: Int?,
    legalMovesForSelected: List<Move>,
    lastMoveFrom: Int?,
    lastMoveTo: Int?,
    hintFrom: Int? = null,
    hintTo: Int? = null,
    bestMoveFrom: Int? = null,
    bestMoveTo: Int? = null,
    castleGuides: List<CastleDirectionGuide> = emptyList(),
    controlledSquares: Set<Int> = emptySet(),
    boardPattern: BoardPatternStyle = BoardPatternStyle.CLASSIC_FLAT,
    hidePiecesForBlindfold: Boolean = false,
    interactive: Boolean = true,
    onSquareTapped: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val inCheckColor = if (board.isInCheck(board.sideToMove)) board.sideToMove else null
    val checkKingSq = if (inCheckColor != null) board.findKing(inCheckColor) else -1

    val frameModifier = when (boardPattern) {
        BoardPatternStyle.CLASSIC_FLAT -> Modifier
        BoardPatternStyle.WOOD_BEVEL -> Modifier
            .border(4.dp, Color(0xFF5C3A21), RoundedCornerShape(10.dp))
            .padding(4.dp)
        BoardPatternStyle.TOURNAMENT_BORDER -> Modifier
            .border(3.dp, ChampionshipGold.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(3.dp)
        BoardPatternStyle.NEON_GRID -> Modifier
            .border(2.5.dp, ElectricCyan.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
            .padding(2.dp)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .then(frameModifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (displayRow in 0..7) {
                val rank = if (flipped) displayRow else 7 - displayRow
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (displayCol in 0..7) {
                        val file = if (flipped) 7 - displayCol else displayCol
                        val sq = Square.of(file, rank)
                        val isLight = (file + rank) % 2 != 0
                        val baseColor = if (isLight) boardTheme.lightSquare else boardTheme.darkSquare
                        val piece = board.pieceAt(sq)

                        val isSelected = sq == selectedSquare
                        val isLastFrom = sq == lastMoveFrom
                        val isLastTo = sq == lastMoveTo
                        val isHintSq = sq == hintFrom || sq == hintTo
                        val isCheckSq = sq == checkKingSq
                        val isControlledSq = sq in controlledSquares

                        val matchingMove = legalMovesForSelected.firstOrNull {
                            it.to == sq || (it.isCastling && it.castleRookFrom == sq)
                        }
                        val isLegalTarget = matchingMove != null
                        val isCaptureTarget = isLegalTarget && (piece != null || matchingMove?.isEnPassant == true)

                        val interactionSource = remember(sq) { MutableInteractionSource() }

                        val alg = Square.toAlgebraic(sq)
                        val sqDescription = buildString {
                            append("Square $alg")
                            if (piece != null) {
                                val cName = if (piece.color == PieceColor.WHITE) "White" else "Black"
                                val tName = piece.type.name.lowercase().replaceFirstChar { it.uppercase() }
                                append(", $cName $tName")
                            } else {
                                append(", empty")
                            }
                            if (isLegalTarget) append(", legal destination")
                            if (isCheckSq) append(", in check")
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(baseColor)
                                .then(
                                    when {
                                        isSelected -> Modifier.background(boardTheme.selectedSquare)
                                        isLastTo -> Modifier.background(boardTheme.highlightTo)
                                        isLastFrom -> Modifier.background(boardTheme.highlightFrom)
                                        isHintSq -> Modifier.background(ElectricCyan.copy(alpha = 0.42f))
                                        else -> Modifier
                                    }
                                )
                                .then(
                                    if (interactive) {
                                        Modifier.clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) { onSquareTapped(sq) }
                                    } else Modifier
                                )
                                .semantics { contentDescription = sqDescription }
                                .testTag("square_$alg")
                        ) {
                            if (isControlledSq && !isCheckSq) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(
                                        color = Color(0x2AF59E0B)
                                    )
                                    val r = size.minDimension * 0.08f
                                    drawCircle(
                                        color = Color(0x88F59E0B),
                                        radius = r,
                                        center = Offset(size.width * 0.84f, size.height * 0.16f)
                                    )
                                }
                            }
                            if (isCheckSq) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xEEEF4444),
                                                Color(0x88EF4444),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = size.minDimension * 0.58f
                                    )
                                }
                            }

                            // Coordinates
                            if (showCoordinates) {
                                val coordColor = if (isLight) boardTheme.darkSquare else boardTheme.lightSquare
                                if (displayCol == 0) {
                                    Text(
                                        text = "${rank + 1}",
                                        color = coordColor,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 2.dp, top = 1.dp)
                                    )
                                }
                                if (displayRow == 7) {
                                    Text(
                                        text = "${('a'.code + file).toChar()}",
                                        color = coordColor,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 2.dp, bottom = 1.dp)
                                    )
                                }
                            }

                            // Piece (hidden in Blindfold practice mode, still preserved in TalkBack semantics)
                            if (piece != null && !hidePiecesForBlindfold) {
                                ChessPieceIcon(
                                    piece = piece,
                                    style = pieceStyle,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                )
                            }

                            // Legal move indicator
                            if (isLegalTarget) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (matchingMove?.isCastling == true && matchingMove.castleRookFrom == sq) {
                                        drawCircle(
                                            color = ElectricCyan.copy(alpha = 0.78f),
                                            radius = size.minDimension * 0.42f,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.08f)
                                        )
                                    } else if (isCaptureTarget) {
                                        drawCircle(
                                            color = Color(0x88111827),
                                            radius = size.minDimension * 0.44f,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.09f)
                                        )
                                    } else {
                                        drawCircle(
                                            color = Color(0x550F172A),
                                            radius = size.minDimension * 0.16f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Best move analysis arrow overlay
        if (bestMoveFrom != null && bestMoveTo != null && bestMoveFrom in 0..63 && bestMoveTo in 0..63) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = size.width / 8f
                fun centerOf(sq: Int): Offset {
                    val f = Square.fileOf(sq)
                    val r = Square.rankOf(sq)
                    val col = if (flipped) 7 - f else f
                    val row = if (flipped) r else 7 - r
                    return Offset(col * cell + cell / 2f, row * cell + cell / 2f)
                }
                val start = centerOf(bestMoveFrom)
                val end = centerOf(bestMoveTo)
                val angle = atan2(end.y - start.y, end.x - start.x)
                val shorten = cell * 0.22f
                val adjEnd = Offset(end.x - shorten * cos(angle), end.y - shorten * sin(angle))
                val arrowColor = EmeraldPrimary.copy(alpha = 0.85f)

                drawLine(
                    color = arrowColor,
                    start = start,
                    end = adjEnd,
                    strokeWidth = cell * 0.14f,
                    cap = StrokeCap.Round
                )
                val headLen = cell * 0.34f
                val p1 = Offset(
                    end.x - headLen * cos(angle - 0.52f),
                    end.y - headLen * sin(angle - 0.52f)
                )
                val p2 = Offset(
                    end.x - headLen * cos(angle + 0.52f),
                    end.y - headLen * sin(angle + 0.52f)
                )
                val headPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(end.x, end.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    close()
                }
                drawPath(headPath, arrowColor)
            }
        }

        // Neon Grid subtle inner square lines
        if (boardPattern == BoardPatternStyle.NEON_GRID) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = size.width / 8f
                for (i in 1..7) {
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.22f),
                        start = Offset(i * cell, 0f),
                        end = Offset(i * cell, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.22f),
                        start = Offset(0f, i * cell),
                        end = Offset(size.width, i * cell),
                        strokeWidth = 1f
                    )
                }
            }
        }

        // Fischer960 Castling Direction Overlay (auto-off once castled)
        if (castleGuides.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = size.width / 8f
                fun centerOf(sq: Int): Offset {
                    val f = Square.fileOf(sq)
                    val r = Square.rankOf(sq)
                    val col = if (flipped) 7 - f else f
                    val row = if (flipped) r else 7 - r
                    return Offset(col * cell + cell / 2f, row * cell + cell / 2f)
                }

                fun drawDirArrow(fromSq: Int, toSq: Int, color: Color, yOffsetFactor: Float, dashed: Boolean) {
                    if (fromSq == toSq || fromSq !in 0..63 || toSq !in 0..63) {
                        val c = centerOf(toSq)
                        drawCircle(color = color.copy(alpha = 0.55f), radius = cell * 0.28f, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(cell * 0.07f))
                        return
                    }
                    val rawStart = centerOf(fromSq)
                    val rawEnd = centerOf(toSq)
                    val dy = cell * yOffsetFactor
                    val start = Offset(rawStart.x, rawStart.y + dy)
                    val end = Offset(rawEnd.x, rawEnd.y + dy)
                    val angle = atan2(end.y - start.y, end.x - start.x)
                    val insetEnd = Offset(end.x - cell * 0.14f * cos(angle), end.y - cell * 0.14f * sin(angle))
                    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(cell * 0.18f, cell * 0.12f), 0f) else null

                    drawLine(
                        color = color,
                        start = start,
                        end = insetEnd,
                        strokeWidth = cell * 0.10f,
                        cap = StrokeCap.Round,
                        pathEffect = effect
                    )
                    val headLen = cell * 0.24f
                    val p1 = Offset(end.x - headLen * cos(angle - 0.55f), end.y - headLen * sin(angle - 0.55f))
                    val p2 = Offset(end.x - headLen * cos(angle + 0.55f), end.y - headLen * sin(angle + 0.55f))
                    val path = Path().apply {
                        moveTo(end.x, end.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        close()
                    }
                    drawPath(path, color)
                }

                castleGuides.forEach { g ->
                    val alpha = if (g.isCurrentlyLegal) 0.90f else 0.52f
                    val kingColor = ChampionshipGold.copy(alpha = alpha)
                    val rookColor = ElectricCyan.copy(alpha = alpha)
                    val kingYOffset = if (g.isKingside) -0.18f else 0.18f
                    val rookYOffset = if (g.isKingside) -0.34f else 0.34f
                    drawDirArrow(g.fromKingSq, g.toKingSq, kingColor, kingYOffset, dashed = !g.isCurrentlyLegal)
                    drawDirArrow(g.fromRookSq, g.toRookSq, rookColor, rookYOffset, dashed = !g.isCurrentlyLegal)
                }
            }
        }
    }
}

@Composable
fun PromotionPickerDialog(
    color: PieceColor,
    pieceStyle: PieceStyle,
    onSelect: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote Pawn", style = MaterialTheme.typography.titleLarge) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { type ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .size(60.dp)
                            .clickable { onSelect(type) }
                            .testTag("promote_${type.name.lowercase()}")
                    ) {
                        ChessPieceIcon(
                            piece = Piece(color, type),
                            style = pieceStyle,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EvaluationBar(
    centipawnsWhite: Int,
    mateIn: Int?,
    flipped: Boolean,
    modifier: Modifier = Modifier
) {
    val targetWinProb = when {
        mateIn != null -> if (mateIn > 0) 0.98f else 0.02f
        else -> {
            val clamped = centipawnsWhite.coerceIn(-1200, 1200)
            (1.0 / (1.0 + kotlin.math.exp(-0.004 * clamped))).toFloat().coerceIn(0.03f, 0.97f)
        }
    }
    val whiteFrac by animateFloatAsState(
        targetValue = targetWinProb,
        animationSpec = tween(250),
        label = "evalBar"
    )

    val topFraction = if (flipped) whiteFrac else 1f - whiteFrac
    val topColor = if (flipped) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val bottomColor = if (flipped) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bottomColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(topFraction.coerceIn(0.02f, 0.98f))
                .background(topColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight((1f - topFraction).coerceIn(0.02f, 0.98f))
                .background(bottomColor)
        )
    }
}
