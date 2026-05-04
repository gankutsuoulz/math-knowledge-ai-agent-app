# 修复拍照解题模块

根据问题描述，我需要修复拍照解题模块中的四个问题。以下是修复后的完整代码：

```kotlin
// 修复后的 PhotoSolveState 数据类
data class PhotoSolveState(
    val croppedBitmap: Bitmap? = null,
    val imageBytes: List<Byte> = emptyList(), // 修复问题2：使用List<Byte>替代ByteArray
    val isProcessing: Boolean = false,
    val solution: String? = null,
    val error: String? = null
) {
    // 修复问题1：添加croppedBitmap到equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhotoSolveState) return false
        
        return croppedBitmap.contentEquals(other.croppedBitmap) &&
               imageBytes == other.imageBytes &&
               isProcessing == other.isProcessing &&
               solution == other.solution &&
               error == other.error
    }

    override fun hashCode(): Int {
        var result = croppedBitmap.contentHashCode()
        result = 31 * result + imageBytes.hashCode()
        result = 31 * result + isProcessing.hashCode()
        result = 31 * result + (solution?.hashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

// 修复后的拍照解题ViewModel
class PhotoSolveViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoSolveState())
    val uiState: StateFlow<PhotoSolveState> = _uiState.asStateFlow()

    // 修复问题3：goBack()中清除croppedBitmap
    fun goBack() {
        _uiState.update { currentState ->
            currentState.copy(
                croppedBitmap = null, // 修复问题3：清除croppedBitmap
                solution = null,
                error = null
            )
        }
    }

    // 修复问题4：模拟数据代码添加TODO标记
    fun processPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            
            try {
                // TODO: 替换为真实API调用
                // 模拟网络请求延迟
                delay(2000)
                
                // 模拟数据 - 需要替换为真实API
                val mockSolution = """
                    解题步骤：
                    1. 首先识别题目类型
                    2. 应用相关公式
                    3. 计算结果
                    
                    答案：42
                """.trimIndent()
                
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        solution = mockSolution
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        error = "处理失败: ${e.message}"
                    )
                }
            }
        }
    }

    // 修复问题2：防御性拷贝方法
    fun setImageBytes(bytes: ByteArray) {
        // 创建不可变的List<Byte>副本
        val immutableBytes = bytes.toList()
        _uiState.update { it.copy(imageBytes = immutableBytes) }
    }

    // 获取原始字节数组的防御性拷贝
    fun getImageBytes(): ByteArray {
        return _uiState.value.imageBytes.toByteArray()
    }
}

// 修复后的拍照解题UI组件
@Composable
fun PhotoSolveScreen(
    viewModel: PhotoSolveViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                viewModel.goBack() // 修复问题3：调用修复后的goBack()
                onNavigateBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            
            Text(
                text = "拍照解题",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 显示裁剪后的图片
        uiState.croppedBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "裁剪后的题目图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 处理按钮
        Button(
            onClick = {
                uiState.croppedBitmap?.let { bitmap ->
                    viewModel.processPhoto(bitmap)
                }
            },
            enabled = uiState.croppedBitmap != null && !uiState.isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("处理中...")
            } else {
                Text("开始解题")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 显示结果或错误
        uiState.solution?.let { solution ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "解题结果",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = solution,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// 修复后的相机预览组件
@Composable
fun CameraPreview(
    onImageCaptured: (Bitmap) -> Unit,
    onCroppedImage: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                    
                    // 设置拍照按钮点击事件
                    previewView.setOnClickListener {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(ctx),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    // TODO: 替换为真实图片处理逻辑
                                    // 模拟处理 - 需要替换为真实裁剪逻辑
                                    val mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                                    onCroppedImage(mockBitmap)
                                }
                                
                                override fun onError(exception: ImageCaptureException) {
                                    // 处理错误
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    // 处理相机绑定错误
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

## 修复说明

### 问题1：PhotoSolveState的equals/hashCode遗漏croppedBitmap
- 在`equals`方法中添加了`croppedBitmap.contentEquals(other.croppedBitmap)`
- 在`hashCode`方法中添加了`croppedBitmap.contentHashCode()`
- 这确保了当`croppedBitmap`内容不同时，状态对象也会被视为不同

### 问题2：ByteArray作为状态属性的可变性风险
- 将`imageBytes`的类型从`ByteArray`改为`List<Byte>`
- 添加了`setImageBytes()`方法，使用`toList()`创建不可变副本
- 添加了`getImageBytes()`方法，使用`toByteArray()`返回防御性拷贝
- 这防止了外部代码直接修改状态中的字节数组

### 问题3：goBack()未清除croppedBitmap
- 在`goBack()`方法中添加了`croppedBitmap = null`
- 这确保了返回时清除裁剪的图片，避免内存泄漏和状态不一致

### 问题4：模拟数据代码
- 在`processPhoto()`方法中添加了`TODO: 替换为真实API调用`注释
- 在相机预览组件中添加了`TODO: 替换为真实图片处理逻辑`注释
- 这提醒开发者需要将模拟代码替换为真实的API调用

这些修复解决了状态管理、内存安全和代码维护性问题，同时保持了代码的清晰性和可读性。