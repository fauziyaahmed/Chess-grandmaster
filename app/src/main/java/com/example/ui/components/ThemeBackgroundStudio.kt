package com.example.ui.components

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.chess.engine.ChessBoard
import com.example.data.preferences.UserSettings
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.ArenaBackgroundPreset
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.COMMUNITY_THEME_PRESETS
import com.example.ui.theme.CUSTOM_GRADIENT_SWATCHES
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PieceStyle
import com.example.ui.theme.ThemeComparisonSetup
import com.example.ui.theme.colorSchemeForTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val COMPARISON_PREVIEW_FEN = "r1bqk2r/pp2bppp/2np1n2/4p3/2B1P3/2NP1N2/PPP2PPP/R1BQK2R w KQkq - 0 7"

@Composable
fun ArenaBackgroundLayer(
    appTheme: AppThemeOption,
    backgroundPreset: ArenaBackgroundPreset,
    customGradientStartArgb: Long,
    customGradientEndArgb: Long,
    customBackgroundImagePath: String?,
    backgroundDimAlpha: Float,
    modifier: Modifier = Modifier
) {
    val customBitmap by produceState<ImageBitmap?>(initialValue = null, customBackgroundImagePath, backgroundPreset) {
        value = if (backgroundPreset == ArenaBackgroundPreset.CUSTOM_IMAGE && !customBackgroundImagePath.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(customBackgroundImagePath)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        } else null
    }

    val themeColors = colorSchemeForTheme(appTheme)
    val scrimColor = if (appTheme.isDark) Color(0xFF04070D) else Color(0xFFF8FAFC)

    Box(modifier = modifier.background(themeColors.background)) {
        when (backgroundPreset) {
            ArenaBackgroundPreset.SOLID_THEME -> {
                Box(modifier = Modifier.fillMaxSize().background(themeColors.background))
            }

            ArenaBackgroundPreset.EMERALD_AURORA -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF050B14), Color(0xFF081F1A), Color(0xFF060A12))
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x6610B981), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.18f),
                            radius = size.maxDimension * 0.55f
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.18f),
                        radius = size.maxDimension * 0.55f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x443B82F6), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.72f),
                            radius = size.maxDimension * 0.5f
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.72f),
                        radius = size.maxDimension * 0.5f
                    )
                }
            }

            ArenaBackgroundPreset.COSMIC_NEBULA -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF060417), Color(0xFF1E1038), Color(0xFF09142B))
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x668B5CF6), Color.Transparent),
                            center = Offset(size.width * 0.75f, size.height * 0.22f),
                            radius = size.maxDimension * 0.48f
                        ),
                        center = Offset(size.width * 0.75f, size.height * 0.22f),
                        radius = size.maxDimension * 0.48f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x5506B6D4), Color.Transparent),
                            center = Offset(size.width * 0.25f, size.height * 0.78f),
                            radius = size.maxDimension * 0.44f
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.78f),
                        radius = size.maxDimension * 0.44f
                    )
                    val starOffsets = listOf(
                        0.12f to 0.10f, 0.28f to 0.22f, 0.44f to 0.08f, 0.67f to 0.16f,
                        0.88f to 0.12f, 0.18f to 0.45f, 0.52f to 0.38f, 0.82f to 0.52f,
                        0.15f to 0.82f, 0.40f to 0.68f, 0.64f to 0.86f, 0.90f to 0.74f
                    )
                    starOffsets.forEachIndexed { idx, (fx, fy) ->
                        drawCircle(
                            color = Color.White.copy(alpha = if (idx % 3 == 0) 0.55f else 0.28f),
                            radius = if (idx % 2 == 0) 2.2f else 1.4f,
                            center = Offset(size.width * fx, size.height * fy)
                        )
                    }
                }
            }

            ArenaBackgroundPreset.ROYAL_GOLD_DAMASK -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0E0B06), Color(0xFF1C1408), Color(0xFF0B0905))
                        )
                    )
                    val step = (size.minDimension / 8f).coerceAtLeast(36f)
                    var x = 0f
                    while (x <= size.width + step) {
                        drawLine(
                            color = Color(0x1FF59E0B),
                            start = Offset(x, 0f),
                            end = Offset(x - size.height * 0.4f, size.height),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = Color(0x16FBBF24),
                            start = Offset(x, 0f),
                            end = Offset(x + size.height * 0.4f, size.height),
                            strokeWidth = 1.5f
                        )
                        x += step
                    }
                }
            }

            ArenaBackgroundPreset.CYBER_GRID -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF050814), Color(0xFF0D1730), Color(0xFF150924))
                        )
                    )
                    val cols = 10
                    val rows = 14
                    val dx = size.width / cols
                    val dy = size.height / rows
                    for (c in 0..cols) {
                        drawLine(
                            color = Color(0x2222D3EE),
                            start = Offset(c * dx, 0f),
                            end = Offset(c * dx, size.height),
                            strokeWidth = 1.2f
                        )
                    }
                    for (r in 0..rows) {
                        drawLine(
                            color = Color(0x22F43F5E),
                            start = Offset(0f, r * dy),
                            end = Offset(size.width, r * dy),
                            strokeWidth = 1.2f
                        )
                    }
                }
            }

            ArenaBackgroundPreset.GRANDMASTER_ARENA -> {
                Image(
                    painter = painterResource(id = R.drawable.img_chess_hero_banner),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            ArenaBackgroundPreset.MARBLE_HALL -> {
                Image(
                    painter = painterResource(id = R.drawable.img_bg_marble_arena),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            ArenaBackgroundPreset.MAHOGANY_CLUB -> {
                Image(
                    painter = painterResource(id = R.drawable.img_bg_mahogany_club),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            ArenaBackgroundPreset.CUSTOM_GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(customGradientStartArgb),
                                    Color(customGradientEndArgb)
                                )
                            )
                        )
                )
            }

            ArenaBackgroundPreset.CUSTOM_IMAGE -> {
                val bmp = customBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(customGradientStartArgb), Color(customGradientEndArgb))
                                )
                            )
                    )
                }
            }
        }

        if (backgroundPreset != ArenaBackgroundPreset.SOLID_THEME) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = backgroundDimAlpha.coerceIn(0.15f, 0.85f)))
            )
        }
    }
}

@Composable
fun ThemeStudioSheetContent(
    settings: UserSettings,
    ownerName: String,
    ownerRating: Int,
    initialTab: Int = 0,
    onSelectAppTheme: (AppThemeOption) -> Unit,
    onSelectBoardTheme: (BoardTheme) -> Unit,
    onSelectPieceStyle: (PieceStyle) -> Unit,
    onSelectBackgroundPreset: (ArenaBackgroundPreset) -> Unit,
    onUpdateCustomGradient: (Long, Long) -> Unit,
    onUpdateBackgroundDim: (Float) -> Unit,
    onPickCustomImageUri: (Uri) -> Unit,
    onClearCustomImage: () -> Unit,
    onApplyComparisonSetup: (ThemeComparisonSetup) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickCustomImageUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Theme & Arena Studio", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${settings.appTheme.displayName} · ${settings.backgroundPreset.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (activeTab == 0) {
                FilledTonalButton(
                    onClick = { activeTab = 1 },
                    modifier = Modifier.testTag("studio_quick_compare_btn")
                ) {
                    Icon(Icons.Filled.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Compare")
                }
            }
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Customize") },
                modifier = Modifier.testTag("theme_customize_tab")
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Compare With Others") },
                modifier = Modifier.testTag("theme_compare_tab")
            )
        }

        if (activeTab == 0) {
            CustomizeThemeTab(
                settings = settings,
                onSelectAppTheme = onSelectAppTheme,
                onSelectBoardTheme = onSelectBoardTheme,
                onSelectPieceStyle = onSelectPieceStyle,
                onSelectBackgroundPreset = onSelectBackgroundPreset,
                onUpdateCustomGradient = onUpdateCustomGradient,
                onUpdateBackgroundDim = onUpdateBackgroundDim,
                onLaunchPhotoPicker = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearCustomImage = onClearCustomImage
            )
        } else {
            CompareSetupsTab(
                settings = settings,
                ownerName = ownerName,
                ownerRating = ownerRating,
                onApplySetup = onApplyComparisonSetup,
                onShareComparison = { summary ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, summary)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Theme Comparison"))
                }
            )
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CustomizeThemeTab(
    settings: UserSettings,
    onSelectAppTheme: (AppThemeOption) -> Unit,
    onSelectBoardTheme: (BoardTheme) -> Unit,
    onSelectPieceStyle: (PieceStyle) -> Unit,
    onSelectBackgroundPreset: (ArenaBackgroundPreset) -> Unit,
    onUpdateCustomGradient: (Long, Long) -> Unit,
    onUpdateBackgroundDim: (Float) -> Unit,
    onLaunchPhotoPicker: () -> Unit,
    onClearCustomImage: () -> Unit
) {
    // 1. Multiple App Themes
    Text("App Theme (${AppThemeOption.entries.size} Themes)", style = MaterialTheme.typography.titleMedium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(AppThemeOption.entries) { theme ->
            val selected = settings.appTheme == theme
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.surfacePreview),
                border = BorderStroke(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) theme.primaryPreview else MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(142.dp)
                    .clickable { onSelectAppTheme(theme) }
                    .testTag("app_theme_card_${theme.name}")
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(theme.primaryPreview))
                            Box(Modifier.size(14.dp).clip(CircleShape).background(theme.secondaryPreview))
                            Box(Modifier.size(14.dp).clip(CircleShape).background(theme.backgroundPreview).border(1.dp, Color.Gray, CircleShape))
                        }
                        if (selected) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = theme.primaryPreview, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (theme.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (theme.isDark) "Dark Mode" else "Light Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (theme.isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                }
            }
        }
    }

    // 2. Arena Backgrounds
    Text("Arena Background (${ArenaBackgroundPreset.entries.size} Presets)", style = MaterialTheme.typography.titleMedium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ArenaBackgroundPreset.entries) { preset ->
            val selected = settings.backgroundPreset == preset
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(114.dp)
                    .clickable {
                        if (preset == ArenaBackgroundPreset.CUSTOM_IMAGE && settings.customBackgroundImagePath.isNullOrBlank()) {
                            onLaunchPhotoPicker()
                        } else {
                            onSelectBackgroundPreset(preset)
                        }
                    }
                    .testTag("bg_preset_card_${preset.name}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    ArenaBackgroundLayer(
                        appTheme = settings.appTheme,
                        backgroundPreset = preset,
                        customGradientStartArgb = settings.customGradientStartArgb,
                        customGradientEndArgb = settings.customGradientEndArgb,
                        customBackgroundImagePath = settings.customBackgroundImagePath,
                        backgroundDimAlpha = 0.18f,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                        }
                    }
                    Surface(
                        color = Color(0xAA000000),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    ) {
                        Text(
                            text = preset.categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // 3. Custom Background Upload & Custom Gradient Studio
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Custom Background Studio", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onLaunchPhotoPicker,
                    modifier = Modifier.weight(1f).testTag("pick_custom_bg_btn")
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (settings.customBackgroundImagePath != null) "Change Photo" else "Custom Photo")
                }
                if (settings.customBackgroundImagePath != null) {
                    OutlinedButton(
                        onClick = onClearCustomImage,
                        modifier = Modifier.testTag("clear_custom_bg_btn")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove Custom Photo", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text("Custom Gradient Start Color", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CUSTOM_GRADIENT_SWATCHES) { argb ->
                    val active = settings.customGradientStartArgb == argb && settings.backgroundPreset == ArenaBackgroundPreset.CUSTOM_GRADIENT
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (active) 2.5.dp else 1.dp,
                                color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { onUpdateCustomGradient(argb, settings.customGradientEndArgb) }
                            .testTag("grad_start_${argb.toString(16)}")
                    )
                }
            }

            Text("Custom Gradient End Color", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CUSTOM_GRADIENT_SWATCHES) { argb ->
                    val active = settings.customGradientEndArgb == argb && settings.backgroundPreset == ArenaBackgroundPreset.CUSTOM_GRADIENT
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (active) 2.5.dp else 1.dp,
                                color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { onUpdateCustomGradient(settings.customGradientStartArgb, argb) }
                            .testTag("grad_end_${argb.toString(16)}")
                    )
                }
            }

            if (settings.backgroundPreset != ArenaBackgroundPreset.SOLID_THEME) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Backdrop Contrast Scrim", style = MaterialTheme.typography.labelMedium)
                    Text("${(settings.backgroundDimAlpha * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontFamily = JetBrainsMonoFontFamily)
                }
                Slider(
                    value = settings.backgroundDimAlpha,
                    onValueChange = onUpdateBackgroundDim,
                    valueRange = 0.15f..0.85f
                )
            }
        }
    }

    // 4. Board Themes & Piece Styles
    Text("Chessboard Style (${BoardTheme.entries.size} Boards)", style = MaterialTheme.typography.titleMedium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(BoardTheme.entries) { bt ->
            FilterChip(
                selected = settings.boardTheme == bt,
                onClick = { onSelectBoardTheme(bt) },
                label = { Text(bt.displayName) },
                leadingIcon = {
                    Row {
                        Box(Modifier.size(11.dp).background(bt.lightSquare))
                        Box(Modifier.size(11.dp).background(bt.darkSquare))
                    }
                },
                modifier = Modifier.testTag("board_chip_${bt.name}")
            )
        }
    }

    Text("Piece Design (${PieceStyle.entries.size} Sets)", style = MaterialTheme.typography.titleMedium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PieceStyle.entries) { ps ->
            FilterChip(
                selected = settings.pieceStyle == ps,
                onClick = { onSelectPieceStyle(ps) },
                label = { Text(ps.displayName) },
                modifier = Modifier.testTag("piece_chip_${ps.name}")
            )
        }
    }
}

@Composable
private fun CompareSetupsTab(
    settings: UserSettings,
    ownerName: String,
    ownerRating: Int,
    onApplySetup: (ThemeComparisonSetup) -> Unit,
    onShareComparison: (String) -> Unit
) {
    val mySetup = remember(settings, ownerName, ownerRating) {
        settings.toComparisonSetup(ownerName, ownerRating)
    }
    val allPresets = remember(mySetup) { listOf(mySetup) + COMMUNITY_THEME_PRESETS }

    var setupA by remember(mySetup) { mutableStateOf(mySetup) }
    var setupB by remember { mutableStateOf(COMMUNITY_THEME_PRESETS.first()) }
    var editingSlot by remember { mutableIntStateOf(1) } // 0 = Setup A, 1 = Setup B

    val previewBoard = remember { ChessBoard.fromFen(COMPARISON_PREVIEW_FEN) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Compare your current setup side-by-side against other players' styles or mix custom themes and backgrounds.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Slot Selector + Swap + Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = editingSlot == 0,
                onClick = { editingSlot = 0 },
                label = { Text("Edit Slot A") },
                modifier = Modifier.testTag("compare_edit_a_chip")
            )
            FilterChip(
                selected = editingSlot == 1,
                onClick = { editingSlot = 1 },
                label = { Text("Edit Slot B") },
                modifier = Modifier.testTag("compare_edit_b_chip")
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    val tmp = setupA
                    setupA = setupB
                    setupB = tmp
                },
                modifier = Modifier.testTag("compare_swap_btn")
            ) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap Setup A and B")
            }
            IconButton(
                onClick = {
                    val summary = buildString {
                        appendLine("Grandmaster Theme Comparison:")
                        appendLine("A: ${setupA.title} (${setupA.authorLabel}) — ${setupA.appTheme.displayName} + ${setupA.boardTheme.displayName} + ${setupA.backgroundPreset.displayName}")
                        appendLine("B: ${setupB.title} (${setupB.authorLabel}) — ${setupB.appTheme.displayName} + ${setupB.boardTheme.displayName} + ${setupB.backgroundPreset.displayName}")
                    }
                    onShareComparison(summary)
                },
                modifier = Modifier.testTag("compare_share_btn")
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Share Comparison")
            }
        }

        // Community & Saved Setups Row
        Text(
            text = if (editingSlot == 0) "Pick Preset for Slot A:" else "Compare Against Community / Preset (Slot B):",
            style = MaterialTheme.typography.labelLarge
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allPresets) { preset ->
                val isSelected = if (editingSlot == 0) setupA.id == preset.id else setupB.id == preset.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (editingSlot == 0) setupA = preset else setupB = preset
                    },
                    label = { Text("${preset.authorLabel.substringBefore(" ")} · ${preset.title}") },
                    modifier = Modifier.testTag("compare_preset_${preset.id}")
                )
            }
        }

        // Side-by-Side Live Preview Boards
        Row(
            modifier = Modifier.fillMaxWidth().testTag("compare_arena_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ComparisonArenaCard(
                slotLabel = "A",
                setup = setupA,
                board = previewBoard,
                onApply = { onApplySetup(setupA) },
                applyTag = "apply_compare_a_btn",
                modifier = Modifier.weight(1f)
            )
            ComparisonArenaCard(
                slotLabel = "B",
                setup = setupB,
                board = previewBoard,
                onApply = { onApplySetup(setupB) },
                applyTag = "apply_compare_b_btn",
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Mix Custom Overrides for the active slot
        val targetSetup = if (editingSlot == 0) setupA else setupB
        val updateTarget: ((ThemeComparisonSetup) -> ThemeComparisonSetup) -> Unit = { fn ->
            if (editingSlot == 0) setupA = fn(setupA).copy(id = "custom_a", title = "Custom A")
            else setupB = fn(setupB).copy(id = "custom_b", title = "Custom B")
        }

        Text("Customize Slot ${if (editingSlot == 0) "A" else "B"} Directly:", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AppThemeOption.entries) { theme ->
                FilterChip(
                    selected = targetSetup.appTheme == theme,
                    onClick = { updateTarget { it.copy(appTheme = theme) } },
                    label = { Text(theme.displayName) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ArenaBackgroundPreset.entries) { bg ->
                FilterChip(
                    selected = targetSetup.backgroundPreset == bg,
                    onClick = { updateTarget { it.copy(backgroundPreset = bg) } },
                    label = { Text(bg.displayName) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(BoardTheme.entries) { bt ->
                FilterChip(
                    selected = targetSetup.boardTheme == bt,
                    onClick = { updateTarget { it.copy(boardTheme = bt) } },
                    label = { Text(bt.displayName) }
                )
            }
        }

        // Spec Comparison Breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Side-by-Side Breakdown", style = MaterialTheme.typography.titleMedium)
                ComparisonSpecRow("Theme", setupA.appTheme.displayName, setupB.appTheme.displayName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ComparisonSpecRow("Background", setupA.backgroundPreset.displayName, setupB.backgroundPreset.displayName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ComparisonSpecRow("Board", setupA.boardTheme.displayName, setupB.boardTheme.displayName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ComparisonSpecRow("Pieces", setupA.pieceStyle.displayName, setupB.pieceStyle.displayName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ComparisonSpecRow(
                    "Brightness",
                    if (setupA.appTheme.isDark) "Dark" else "Light",
                    if (setupB.appTheme.isDark) "Dark" else "Light"
                )
            }
        }
    }
}

@Composable
private fun ComparisonArenaCard(
    slotLabel: String,
    setup: ThemeComparisonSetup,
    board: ChessBoard,
    onApply: () -> Unit,
    applyTag: String,
    modifier: Modifier = Modifier
) {
    val scheme = colorSchemeForTheme(setup.appTheme)
    Card(
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.5.dp, scheme.primary.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ArenaBackgroundLayer(
                appTheme = setup.appTheme,
                backgroundPreset = setup.backgroundPreset,
                customGradientStartArgb = setup.customGradientStartArgb,
                customGradientEndArgb = setup.customGradientEndArgb,
                customBackgroundImagePath = setup.customBackgroundImagePath,
                backgroundDimAlpha = setup.backgroundDimAlpha,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = scheme.primary, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = slotLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(color = scheme.surfaceContainerHigh.copy(alpha = 0.9f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "04:58",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = scheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = setup.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${setup.authorLabel} · ${setup.ratingLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ChessBoardView(
                    board = board,
                    boardTheme = setup.boardTheme,
                    pieceStyle = setup.pieceStyle,
                    flipped = false,
                    showCoordinates = false,
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    lastMoveFrom = 12,
                    lastMoveTo = 28,
                    interactive = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth().testTag(applyTag)
                ) {
                    Text("Use $slotLabel", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ComparisonSpecRow(label: String, valueA: String, valueB: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(74.dp)
        )
        Text(
            text = valueA,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueB,
            style = MaterialTheme.typography.labelMedium,
            color = ChampionshipGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
