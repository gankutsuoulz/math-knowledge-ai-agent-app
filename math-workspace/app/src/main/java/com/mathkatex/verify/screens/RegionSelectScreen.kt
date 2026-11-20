package com.mathkatex.verify.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathkatex.verify.ui.theme.LocalAppColors
import com.mathkatex.verify.util.RegionMasker
import com.mathkatex.verify.util.RegionShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

enum class RegionMode(val label: String) {
    RECT("方框"),
    LASSO("自由圈选"),
    BRUSH("笔刷")
}

/**
 * 试卷区域选择：方框 / 自由圈选 / 笔刷，支持撤销和重画。
 * 圈选结果会被裁成一张只含圈内内容的图片，再交给识别链路。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelectScreen(
    originalBitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    // 手动转正：EXIF 缺失时（如截图横放）用户可一键旋转
    var workingBitmap by remember { mutableStateOf(originalBitmap) }
    var mode by remember { mutableStateOf(RegionMode.LASSO) }
    val shapes = remember { mutableStateListOf<RegionShape>() }
    var draftRect by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var draftPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var container by remember { mutableStateOf(IntSize.Zero) }
    var busy by remember { mutableStateOf(false) }

    val imgW = workingBitmap.width.toFloat()
    val imgH = workingBitmap.height.toFloat()
    val scale = if (container.width > 0 && container.height > 0)
        min(container.width / imgW, container.height / imgH) else 1f
    val dx = (container.width - imgW * scale) / 2f
    val dy = (container.height - imgH * scale) / 2f

    fun toImage(p: Offset): Offset = Offset((p.x - dx) / scale, (p.y - dy) / scale)
    fun toView(x: Float, y: Float): Offset = Offset(x * scale + dx, y * scale + dy)
    fun clampImage(p: Offset): Offset = Offset(
        p.x.coerceIn(0f, imgW), p.y.coerceIn(0f, imgH)
    )

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("圈选题目区域", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { shapes.clear() }, enabled = shapes.isNotEmpty()) {
                        Text("清空", fontSize = 13.sp)
                    }
                    TextButton(
                        onClick = { if (shapes.isNotEmpty()) shapes.removeAt(shapes.lastIndex) },
                        enabled = shapes.isNotEmpty()
                    ) { Text("撤销", fontSize = 13.sp) }
                    IconButton(onClick = {
                        val matrix = Matrix().apply { postRotate(90f) }
                        workingBitmap = Bitmap.createBitmap(
                            workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, matrix, true
                        )
                        shapes.clear()
                    }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "旋转 90°")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "试卷空白多也没关系：沿着题目边缘圈一圈，圈外内容不会参与识别。",
                fontSize = 12.sp,
                color = colors.text2,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0B1220), RoundedCornerShape(16.dp))
                    .onSizeChanged { container = it },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = workingBitmap.asImageBitmap(),
                    contentDescription = "待圈选试卷",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(mode, imgW, imgH) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    val p = clampImage(toImage(start))
                                    when (mode) {
                                        RegionMode.RECT -> draftRect = p to p
                                        RegionMode.LASSO, RegionMode.BRUSH -> draftPoints = listOf(p)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val p = clampImage(toImage(change.position))
                                    when (mode) {
                                        RegionMode.RECT -> draftRect = draftRect?.copy(second = p) ?: (p to p)
                                        RegionMode.LASSO -> draftPoints = draftPoints + p
                                        RegionMode.BRUSH -> {
                                            val last = draftPoints.lastOrNull()
                                            if (last == null || (last - p).getDistance() > 6f) {
                                                draftPoints = draftPoints + p
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    when (mode) {
                                        RegionMode.RECT -> {
                                            draftRect?.let { (a, b) ->
                                                if ((a - b).getDistance() > 12f) {
                                                    shapes.add(
                                                        RegionShape.Rect(
                                                            minOf(a.x, b.x), minOf(a.y, b.y),
                                                            maxOf(a.x, b.x), maxOf(a.y, b.y)
                                                        )
                                                    )
                                                }
                                            }
                                            draftRect = null
                                        }
                                        RegionMode.LASSO -> {
                                            if (draftPoints.size > 3) {
                                                shapes.add(RegionShape.Polygon(draftPoints.map { it.x to it.y }))
                                            }
                                            draftPoints = emptyList()
                                        }
                                        RegionMode.BRUSH -> {
                                            if (draftPoints.isNotEmpty()) {
                                                shapes.add(
                                                    RegionShape.Brush(
                                                        draftPoints.map { it.x to it.y },
                                                        radius = (imgW / 34f).coerceAtLeast(8f)
                                                    )
                                                )
                                            }
                                            draftPoints = emptyList()
                                        }
                                    }
                                },
                                onDragCancel = {
                                    draftRect = null
                                    draftPoints = emptyList()
                                }
                            )
                        }
                ) {
                    val stroke = Stroke(width = 2.5.dp.toPx())
                    shapes.forEach { shape ->
                        when (shape) {
                            is RegionShape.Rect -> {
                                val topLeft = toView(shape.left, shape.top)
                                val size = androidx.compose.ui.geometry.Size(
                                    (shape.right - shape.left) * scale,
                                    (shape.bottom - shape.top) * scale
                                )
                                drawRect(
                                    color = colors.primary.copy(alpha = 0.14f),
                                    topLeft = topLeft,
                                    size = size
                                )
                                drawRect(
                                    color = colors.primary,
                                    topLeft = topLeft,
                                    size = size,
                                    style = stroke
                                )
                            }
                            is RegionShape.Polygon -> {
                                if (shape.points.size > 2) {
                                    val path = Path().apply {
                                        moveTo(toView(shape.points[0].first, shape.points[0].second).x,
                                            toView(shape.points[0].first, shape.points[0].second).y)
                                        shape.points.drop(1).forEach { (x, y) ->
                                            val v = toView(x, y); lineTo(v.x, v.y)
                                        }
                                        close()
                                    }
                                    drawPath(path, colors.primary.copy(alpha = 0.14f))
                                    drawPath(path, colors.primary, style = stroke)
                                }
                            }
                            is RegionShape.Brush -> {
                                shape.points.forEach { (x, y) ->
                                    val v = toView(x, y)
                                    drawCircle(colors.primary.copy(alpha = 0.22f), shape.radius * scale, v)
                                }
                            }
                        }
                    }
                    draftRect?.let { (a, b) ->
                        val tl = toView(minOf(a.x, b.x), minOf(a.y, b.y))
                        val size = androidx.compose.ui.geometry.Size(
                            kotlin.math.abs(b.x - a.x) * scale,
                            kotlin.math.abs(b.y - a.y) * scale
                        )
                        drawRect(colors.primary, tl, size, style = stroke)
                    }
                    if (draftPoints.size > 1) {
                        val pts = draftPoints.map { toView(it.x, it.y) }
                        if (mode == RegionMode.BRUSH) {
                            pts.forEach { drawCircle(colors.primary.copy(alpha = 0.22f), (imgW / 34f) * scale, it) }
                        } else {
                            val path = Path().apply {
                                moveTo(pts[0].x, pts[0].y)
                                pts.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(
                                path, colors.primary,
                                style = Stroke(width = 2.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegionMode.values().forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label, fontSize = 12.sp) }
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (shapes.isEmpty()) "圈选 1 个区域" else "已选 ${shapes.size} 个区域",
                    fontSize = 11.sp,
                    color = colors.text3,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = when (mode) {
                    RegionMode.RECT -> "拖一个方框罩住题目；排版整齐时最快。"
                    RegionMode.LASSO -> "手指沿题目边缘画一圈，松手自动闭合；不规则排版推荐。"
                    RegionMode.BRUSH -> "在题目上来回涂，涂过的地方都会保留；适合细长题干。"
                },
                fontSize = 11.sp,
                color = colors.text3
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("取消") }
                Button(
                    onClick = {
                        if (shapes.isEmpty() || busy) return@Button
                        busy = true
                        scope.launch {
                            val masked = withContext(Dispatchers.IO) {
                                RegionMasker.mask(workingBitmap, shapes.toList())
                            }
                            busy = false
                            onConfirm(masked)
                        }
                    },
                    enabled = shapes.isNotEmpty() && !busy,
                    modifier = Modifier.weight(1.6f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (busy) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White
                    ) else Text("用这块区域识别")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
