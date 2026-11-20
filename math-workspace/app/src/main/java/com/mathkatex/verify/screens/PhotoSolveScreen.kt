package com.mathkatex.verify.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mathkatex.verify.screens.MarkdownWebView
import com.mathkatex.verify.data.SettingsManager
import com.mathkatex.verify.data.local.KnowledgeEntity
import com.mathkatex.verify.data.local.KnowledgeRepository
import com.mathkatex.verify.data.service.LLMService
import com.google.gson.Gson
import com.mathkatex.verify.ui.theme.LocalAppColors
import com.mathkatex.verify.util.LogFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

// 最大图片尺寸限制：1600px（防止 OOM）
private const val MAX_IMAGE_DIMENSION = 1600
// JPEG 压缩质量
private const val JPEG_QUALITY = 85

/** 结构化输出提示词：App 直接渲染成卡片，解析失败再退回 Markdown。 */
private const val STRUCTURED_SOLVE_PROMPT = """你是一位耐心的初中数学老师。请先识别图片中的数学题目，再检查学生的解答。
只输出一个 JSON 对象，不要用代码块包裹，字段如下：
{"verdict":"对/错/部分对/未完成","question":"题干（LaTeX）","knowledge":["知识点1","知识点2"],"stuck":"卡在哪一步（一句话）","hints":["引导问题1","引导问题2","引导问题3"],"steps":["完整解析步骤1","步骤2"],"answer":"最终答案（LaTeX）"}
要求：hints 只给启发式追问，不直接给答案；steps/answer 可以晚点再看。"""

sealed class PhotoSolveStep {
    object Idle : PhotoSolveStep()
    object ImageSelected : PhotoSolveStep()
    object Analyzing : PhotoSolveStep()
    object Completed : PhotoSolveStep()
    object Error : PhotoSolveStep()
}

data class StructuredSolution(
    val verdict: String? = null,
    val question: String? = null,
    val knowledge: List<String> = emptyList(),
    val stuck: String? = null,
    val hints: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val answer: String? = null
)

data class SolveResult(
    val rawResponse: String,
    val structured: StructuredSolution? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSolveScreen(
    onNavigateToImageEdit: ((Bitmap) -> Unit)? = null,
    onStartPractice: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val llmService = remember { LLMService() }
    val scrollState = rememberScrollState()

    var currentStep by remember { mutableStateOf<PhotoSolveStep>(PhotoSolveStep.Idle) }
    // 圈选页内嵌显示，避免离开页面导致已选图片状态丢失
    var showRegionSelect by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SolveResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFullSolution by remember { mutableStateOf(false) }
    var knowledgeDialog by remember { mutableStateOf<KnowledgeEntity?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 相机高清拍照用的临时文件 URI
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Initialize LLM service with saved settings
    LaunchedEffect(Unit) {
        llmService.refreshConfigFromSettings()
    }

    // 高清相机启动器 - 使用 FileProvider 创建临时文件
    val photoFile = remember {
        val dir = File(context.cacheDir, "photos")
        dir.mkdirs()
        File(dir, "math_photo_${System.currentTimeMillis()}.jpg")
    }
    val cameraPhotoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = cameraPhotoUri
            // 在 IO 线程解码并缩放到安全尺寸
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmapFromUri(context, cameraPhotoUri, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                }
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    currentStep = PhotoSolveStep.ImageSelected
                } else {
                    errorMessage = "图片解码失败"
                    currentStep = PhotoSolveStep.Error
                }
            }
        }
    }

    // 相册启动器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmapFromUri(context, uri, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                }
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    currentStep = PhotoSolveStep.ImageSelected
                } else {
                    errorMessage = "图片解码失败"
                    currentStep = PhotoSolveStep.Error
                }
            }
        }
    }

    // 权限启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            cameraLauncher.launch(cameraPhotoUri)
        }
    }

    // 不在离开页面时 recycle：位图可能被圈选页继续使用，交给 GC 回收
    val colors = LocalAppColors.current

    // 内嵌圈选页：确认/取消都直接回到当前页面，图片状态不丢
    if (showRegionSelect && selectedBitmap != null) {
        RegionSelectScreen(
            originalBitmap = selectedBitmap!!,
            onConfirm = { masked ->
                selectedBitmap = masked
                currentStep = PhotoSolveStep.ImageSelected
                showRegionSelect = false
            },
            onCancel = { showRegionSelect = false }
        )
        return
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "解题辅导",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // 顶部操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (hasCameraPermission) {
                            cameraLauncher.launch(cameraPhotoUri)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("拍照")
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("相册选择")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 图片预览区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    is PhotoSolveStep.Idle -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "点击上方按钮获取图片",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is PhotoSolveStep.Error -> {
                        val bitmap = selectedBitmap
                        if (bitmap == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("识别失败，请重新选择图片", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    else -> {
                        selectedBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 编辑按钮 - 仅在图片选中时显示
            if (currentStep == PhotoSolveStep.ImageSelected && selectedBitmap != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showRegionSelect = true
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("圈选区域", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onNavigateToImageEdit?.invoke(selectedBitmap!!)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("裁剪/旋转", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentStep == PhotoSolveStep.ImageSelected)
                            "提示：可点「圈选区域」只保留题目部分，空白多或不规则排版都能圈"
                        else
                            "提示：请确保图片中题目清晰可见，避免反光和阴影",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 分析按钮
            Button(
                onClick = {
                    if ((currentStep == PhotoSolveStep.ImageSelected || currentStep is PhotoSolveStep.Error) && selectedBitmap != null) {
                        currentStep = PhotoSolveStep.Analyzing
                        errorMessage = null

                        scope.launch {
                            // Base64 编码在 IO 线程执行，不阻塞 UI
                            val base64 = withContext(Dispatchers.IO) {
                                bitmapToBase64(selectedBitmap!!)
                            }
                            // 读取自定义提示词（如果启用）
                            val usePrompt = if (SettingsManager.photoPromptEnabled) {
                                SettingsManager.photoPrompt
                            } else {
                                STRUCTURED_SOLVE_PROMPT
                            }

                            val apiResult = llmService.analyzeMathImage(
                                imageBase64 = base64,
                                prompt = usePrompt
                            )

                            apiResult.fold(
                                onSuccess = { response ->
                                    LogFileManager.logResponse("PhotoSolve", response)
                                    result = SolveResult(
                                        rawResponse = response,
                                        structured = parseStructuredSolution(response)
                                    )
                                    currentStep = PhotoSolveStep.Completed
                                },
                                onFailure = { error ->
                                    LogFileManager.logError("PhotoSolve", error.message ?: "Unknown error", error)
                                    errorMessage = error.message ?: "未知错误"
                                    currentStep = PhotoSolveStep.Error
                                }
                            )
                        }
                    }
                },
                enabled = selectedBitmap != null && (currentStep == PhotoSolveStep.ImageSelected || currentStep is PhotoSolveStep.Error),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始识别分析")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error state
            if (currentStep is PhotoSolveStep.Error && errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "识别失败",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading / Completed state
            when (currentStep) {
                is PhotoSolveStep.Analyzing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AI正在分析中...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is PhotoSolveStep.Completed -> {
                    result?.let { solveResult ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, colors.line),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = "已识别题目",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.success
                                    )
                                }
                                Spacer(Modifier.height(10.dp))

                                val structured = solveResult.structured
                                if (structured == null) {
                                    Text(
                                        text = "先别急着看答案",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.text
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "顺着下面的提示想，卡住了再往下看",
                                        fontSize = 11.5.sp,
                                        color = colors.text3
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = colors.line, thickness = 1.dp)
                                    Spacer(Modifier.height(6.dp))
                                    MarkdownWebView(
                                        markdown = solveResult.rawResponse,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .heightIn(min = 100.dp)
                                    )
                                } else {
                                    Text(
                                        text = structured.verdict ?: "已识别",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if ((structured.verdict ?: "").contains("错")) colors.danger else colors.success
                                    )
                                    structured.question?.takeIf { it.isNotBlank() }?.let { q ->
                                        Spacer(Modifier.height(8.dp))
                                        MarkdownWebView(
                                            markdown = q.replace("\\n", "\n"),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                                        )
                                    }
                                    if (structured.knowledge.isNotEmpty()) {
                                        Spacer(Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            structured.knowledge.take(6).forEach { k ->
                                                Surface(
                                                    color = colors.primarySoft,
                                                    shape = RoundedCornerShape(999.dp),
                                                    modifier = Modifier.clickable {
                                                        scope.launch {
                                                            val id = KnowledgeRepository.resolveKnowledgeId(k)
                                                            knowledgeDialog = id?.let { KnowledgeRepository.getById(it) }
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        text = k,
                                                        fontSize = 11.sp,
                                                        color = colors.primary,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    // 卡点与引导问题里常有公式，交给本地 KaTeX 渲染，避免露出 $ 或反斜杠
                                    if (structured.stuck?.isNotBlank() == true || structured.hints.isNotEmpty()) {
                                        val guidanceMarkdown = buildString {
                                            structured.stuck?.takeIf { it.isNotBlank() }?.let {
                                                append("**卡点**：").append(it.replace("\\n", "\n")).append("\n\n")
                                            }
                                            if (structured.hints.isNotEmpty()) {
                                                append("**引导问题**\n\n")
                                                structured.hints.forEachIndexed { index, hint ->
                                                    append(index + 1).append(". ").append(hint.replace("\\n", "\n")).append("\n")
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        MarkdownWebView(
                                            markdown = guidanceMarkdown.trim(),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                                        )
                                    }
                                    if (structured.steps.isNotEmpty() || structured.answer != null) {
                                        Spacer(Modifier.height(14.dp))
                                        OutlinedButton(
                                            onClick = { showFullSolution = !showFullSolution },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text(if (showFullSolution) "收起完整解析" else "看完整解析", fontSize = 13.sp)
                                        }
                                        if (showFullSolution) {
                                            val md = buildString {
                                                structured.steps.forEachIndexed { i, step ->
                                                    append("**第 ${i + 1} 步**：").append(step.replace("\\n", "\n")).append("\n\n")
                                                }
                                                structured.answer?.let { append("**答案**：").append(it) }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            MarkdownWebView(
                                                markdown = md,
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            currentStep = PhotoSolveStep.Idle
                                            selectedBitmap?.recycle()
                                            selectedBitmap = null
                                            selectedImageUri = null
                                            result = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("重新拍照", color = colors.text2)
                                    }

                                    Button(
                                        onClick = { /* TODO: 收藏功能，后续实现 */ },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("收藏")
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    knowledgeDialog?.let { entity ->
        AlertDialog(
            onDismissRequest = { knowledgeDialog = null },
            title = { Text(entity.title, fontSize = 16.sp) },
            text = {
                Column {
                    if (entity.formula.isNotBlank()) {
                        Text(entity.formula, fontSize = 12.sp, color = colors.text2)
                    }
                    if (entity.content.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(entity.content.take(160), fontSize = 12.sp, color = colors.text2, lineHeight = 18.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = entity.id
                    knowledgeDialog = null
                    onStartPractice(id)
                }) { Text("去练习") }
            },
            dismissButton = {
                TextButton(onClick = { knowledgeDialog = null }) { Text("关闭") }
            }
        )
    }
}

/**
 * 处理图片编辑完成的回调
 */
fun onImageEditComplete(
    croppedBitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit
) {
    onConfirm(croppedBitmap)
}

/**
 * 从 URI 解码并缩放到安全尺寸，防止 OOM
 */
private fun decodeSampledBitmapFromUri(
    context: android.content.Context,
    uri: Uri,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? {
    return try {
        // 先读尺寸（不解码像素）
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        // 计算采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        // 解码缩放后的位图
        val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return null

        // 按 EXIF 方向转正（手机竖拍常见，否则题目会横躺）
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it != decoded) decoded.recycle() }
        } else decoded
    } catch (e: Exception) {
        LogFileManager.logError("PhotoSolve", "decodeSampledBitmapFromUri error: ${e.message}", e)
        null
    }
}

/**
 * 计算合适的采样率，使图片尺寸不超过目标尺寸
 */
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Bitmap 转 Base64（压缩至 JPEG quality 85）
 * 注意：此方法应在 IO/Default 线程执行，避免阻塞 UI
 */
private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
    val byteArray = outputStream.toByteArray()
    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
    return "data:image/jpeg;base64,$base64"
}


/** 解析模型的结构化 JSON；失败返回 null，UI 退回 Markdown 渲染。 */
private fun parseStructuredSolution(raw: String): StructuredSolution? {
    val text = raw.trim()
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    return try {
        Gson().fromJson(text.substring(start, end + 1), StructuredSolution::class.java)
    } catch (e: Exception) {
        LogFileManager.logError("PhotoSolve", "parseStructuredSolution failed: ${e.message}")
        null
    }
}
