package com.mathkatex.verify.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

sealed class FormulaConvertStep {
    object Idle : FormulaConvertStep()
    object ImageSelected : FormulaConvertStep()
    object Extracting : FormulaConvertStep()
    object Completed : FormulaConvertStep()
}

data class ExtractedFormula(
    val latex: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaConvertScreen() {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf<FormulaConvertStep>(FormulaConvertStep.Idle) }
    var extractedFormulas by remember { mutableStateOf<List<ExtractedFormula>>(emptyList()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedImageUri = Uri.parse("file://tmp_camera_photo")
            currentStep = FormulaConvertStep.ImageSelected
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            currentStep = FormulaConvertStep.ImageSelected
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "拍照转公式 [F-08]",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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
                            cameraLauncher.launch(null)
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
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color = Color(0xFFBBDEFB),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    is FormulaConvertStep.Idle -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFFBDBDBD)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "点击上方按钮获取图片",
                                fontSize = 14.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "图片已加载",
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "点击下方按钮开始识别",
                                fontSize = 12.sp,
                                color = Color(0xFF8BC34A)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 提示文本
            Text(
                text = "📌 请确保图片中公式清晰可见，避免反光和阴影",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // 开始识别按钮
            Button(
                onClick = {
                    if (currentStep == FormulaConvertStep.ImageSelected) {
                        currentStep = FormulaConvertStep.Extracting
                        // 模拟公式提取（OCR）
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            extractedFormulas = listOf(
                                ExtractedFormula(
                                    latex = "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                                    description = "一元二次方程求根公式"
                                ),
                                ExtractedFormula(
                                    latex = "\\int_{a}^{b} f(x) dx = F(b) - F(a)",
                                    description = "定积分基本公式"
                                ),
                                ExtractedFormula(
                                    latex = "\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}",
                                    description = "等差数列求和公式"
                                )
                            )
                            currentStep = FormulaConvertStep.Completed
                        }, 2500)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = currentStep == FormulaConvertStep.ImageSelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C4DFF)
                )
            ) {
                Text(
                    text = if (currentStep == FormulaConvertStep.Extracting) "识别中..." else "开始识别公式",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            // 识别中状态
            if (currentStep == FormulaConvertStep.Extracting) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF7C4DFF)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "正在提取图片中的公式...",
                            fontSize = 14.sp,
                            color = Color(0xFF6A1B9A)
                        )
                    }
                }
            }
            
            // 提取结果展示
            extractedFormulas.forEachIndexed { index, formula ->
                Spacer(modifier = Modifier.height(16.dp))
                
                // 公式卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 公式序号和描述
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        Color(0xFF7C4DFF),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = formula.description,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        // KaTeX 渲染的公式
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            KaTeXWebView(
                                latex = formula.latex,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // LaTeX 源码（可复制）
                        Text(
                            text = "LaTeX 源码：",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF888888),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF263238))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = formula.latex,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF80CBC4)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // 操作按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("LaTeX", formula.latex)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("复制LaTeX", fontSize = 13.sp)
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "重新识别中...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("重新识别", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            
            // 底部操作按钮（当有结果时）
            if (currentStep == FormulaConvertStep.Completed) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentStep = FormulaConvertStep.Idle
                            extractedFormulas = emptyList()
                            selectedImageUri = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("重新拍照")
                    }
                    
                    Button(
                        onClick = {
                            Toast.makeText(context, "已保存 ${extractedFormulas.size} 个公式", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存全部")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
