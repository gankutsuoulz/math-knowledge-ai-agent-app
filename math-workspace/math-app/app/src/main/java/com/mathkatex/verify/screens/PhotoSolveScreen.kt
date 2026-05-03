package com.mathkatex.verify.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
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
import com.mathkatex.verify.data.SettingsManager
import com.mathkatex.verify.data.service.LLMService
import com.mathkatex.verify.util.LogFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

sealed class PhotoSolveStep {
    object Idle : PhotoSolveStep()
    object ImageSelected : PhotoSolveStep()
    object Analyzing : PhotoSolveStep()
    object Completed : PhotoSolveStep()
    object Error : PhotoSolveStep()
}

data class SolveResult(
    val rawResponse: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSolveScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val llmService = remember { LLMService() }
    
    var currentStep by remember { mutableStateOf<PhotoSolveStep>(PhotoSolveStep.Idle) }
    var result by remember { mutableStateOf<SolveResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Initialize LLM service with saved settings
    LaunchedEffect(Unit) {
        llmService.updateConfig(
            apiKey = SettingsManager.apiKey,
            provider = SettingsManager.getLLMProvider()
        )
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedImageUri = null
            currentStep = PhotoSolveStep.ImageSelected
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            // Load bitmap from URI
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    currentStep = PhotoSolveStep.ImageSelected
                }
            } catch (e: Exception) {
                errorMessage = "图片加载失败: ${e.message}"
                currentStep = PhotoSolveStep.Error
            }
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
                        text = "拍照解题 [F-01]",
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
                    .height(200.dp)
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
                    is PhotoSolveStep.Idle, is PhotoSolveStep.Error -> {
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
                        selectedBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } ?: run {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "提示：请确保图片中题目清晰可见，避免反光和阴影",
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分析按钮
            Button(
                onClick = {
                    if (currentStep == PhotoSolveStep.ImageSelected && selectedBitmap != null) {
                        currentStep = PhotoSolveStep.Analyzing
                        errorMessage = null
                        
                        scope.launch(Dispatchers.Main) {
                            val base64 = bitmapToBase64(selectedBitmap!!)
                            val apiResult = llmService.analyzeMathImage(
                                imageBase64 = base64,
                                prompt = "请仔细分析这张图片中的数学题目，给出完整的解题步骤和最终答案。"
                            )
                            
                            apiResult.fold(
                                onSuccess = { response ->
                                    LogFileManager.logResponse("PhotoSolve", response)
                                    result = SolveResult(rawResponse = response)
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
                enabled = currentStep == PhotoSolveStep.ImageSelected && selectedBitmap != null,
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
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
                                color = Color(0xFFD32F2F)
                            )
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Loading state
            when (currentStep) {
                is PhotoSolveStep.Analyzing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
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
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
                is PhotoSolveStep.Completed -> {
                    result?.let { solveResult ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Photo,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "识别结果",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                
                                MarkdownKaTeXText(
                                    content = solveResult.rawResponse,
                                    modifier = Modifier.padding(bottom = 16.dp),
                                    textColor = Color(0xFF424242)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            currentStep = PhotoSolveStep.Idle
                                            selectedBitmap = null
                                            selectedImageUri = null
                                            result = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
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
                                        onClick = { /* TODO: 收藏功能 */ },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
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
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    // Compress to JPEG with quality 85 to reduce size while maintaining clarity
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    val byteArray = outputStream.toByteArray()
    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
    // Return as data URI
    return "data:image/jpeg;base64,$base64"
}
