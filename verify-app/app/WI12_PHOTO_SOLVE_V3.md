# 拍照解题模块修复

根据问题描述，我将修复三个关键问题并提供完整的修复代码。

## 修复方案

### 问题1：equals/hashCode中的Bitmap比较方式错误
- 使用`===`引用比较或`Bitmap.sameAs()`替代不存在的`contentEquals`方法

### 问题2：imageBytes未在goBack()中清除
- 在`goBack()`方法中添加`imageBytes = null`

### 问题3：CameraPreview参数未使用
- 删除未使用的`onImageCaptured`参数，或正确调用回调

## 修复后的完整代码

```kotlin
package com.example.mathapp.photosolve

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.mathapp.R
import java.util.Arrays

class PhotoSolveActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PhotoSolveActivity"
    }
    
    private var currentBitmap: Bitmap? = null
    private var imageBytes: ByteArray? = null
    private var cameraPreview: CameraPreview? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_solve)
        
        // 初始化相机预览
        setupCameraPreview()
    }
    
    private fun setupCameraPreview() {
        // 修复问题3：删除未使用的onImageCaptured参数
        // 原代码：cameraPreview = CameraPreview(this, onImageCaptured = { ... })
        // 修复后：使用正确的回调方式或删除未使用参数
        cameraPreview = CameraPreview(this)
        
        // 如果需要处理拍照回调，使用正确的回调接口
        cameraPreview?.setOnImageCapturedListener(object : CameraPreview.OnImageCapturedListener {
            override fun onImageCaptured(bitmap: Bitmap) {
                handleCapturedImage(bitmap)
            }
        })
    }
    
    private fun handleCapturedImage(bitmap: Bitmap) {
        currentBitmap = bitmap
        imageBytes = bitmapToByteArray(bitmap)
        
        // 处理解题逻辑
        processImageForSolving(bitmap)
    }
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    
    private fun processImageForSolving(bitmap: Bitmap) {
        // 实现解题逻辑
        Log.d(TAG, "Processing image for solving")
    }
    
    // 修复问题2：在goBack()中清除imageBytes
    fun goBack() {
        // 清除所有资源
        currentBitmap?.recycle()
        currentBitmap = null
        imageBytes = null  // 修复：添加这行代码
        
        // 关闭相机预览
        cameraPreview?.releaseCamera()
        cameraPreview = null
        
        finish()
    }
    
    // 修复问题1：修复equals/hashCode中的Bitmap比较方式
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        
        val that = other as PhotoSolveActivity
        
        // 修复：使用===引用比较或Bitmap.sameAs()
        // 方案1：使用===引用比较（推荐，性能更好）
        val bitmapEqual = currentBitmap === that.currentBitmap
        
        // 方案2：如果需要内容比较，使用Bitmap.sameAs()
        // val bitmapEqual = currentBitmap?.sameAs(that.currentBitmap) ?: (that.currentBitmap == null)
        
        return bitmapEqual && Arrays.equals(imageBytes, that.imageBytes)
    }
    
    override fun hashCode(): Int {
        // 修复：使用安全的hashCode计算
        var result = currentBitmap?.hashCode() ?: 0
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
    
    // 辅助方法：计算ByteArray的hashCode
    private fun ByteArray?.contentHashCode(): Int {
        return this?.sumOf { it.toInt() } ?: 0
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 确保资源被释放
        currentBitmap?.recycle()
        currentBitmap = null
        imageBytes = null
        cameraPreview?.releaseCamera()
        cameraPreview = null
    }
}

// CameraPreview类的修复版本
class CameraPreview(private val context: android.content.Context) {
    
    interface OnImageCapturedListener {
        fun onImageCaptured(bitmap: Bitmap)
    }
    
    private var listener: OnImageCapturedListener? = null
    private var camera: android.hardware.Camera? = null
    
    fun setOnImageCapturedListener(listener: OnImageCapturedListener) {
        this.listener = listener
    }
    
    // 修复问题3：删除未使用的onImageCaptured参数
    // 原代码可能有：fun startPreview(onImageCaptured: (Bitmap) -> Unit)
    // 修复后：使用正确的回调接口
    fun startPreview() {
        try {
            camera = android.hardware.Camera.open()
            // 设置相机参数和预览
            setupCameraPreview()
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error opening camera", e)
        }
    }
    
    private fun setupCameraPreview() {
        // 相机预览设置逻辑
    }
    
    fun captureImage() {
        camera?.takePicture(null, null, object : android.hardware.Camera.PictureCallback {
            override fun onPictureTaken(data: ByteArray?, camera: android.hardware.Camera?) {
                data?.let { bytes ->
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    listener?.onImageCaptured(bitmap)
                }
            }
        })
    }
    
    fun releaseCamera() {
        camera?.release()
        camera = null
    }
}
```

## 关键修复说明

### 1. equals/hashCode修复
- 使用`===`进行引用比较，避免Bitmap内容比较的性能问题
- 如果确实需要内容比较，使用`Bitmap.sameAs()`方法
- 修复了hashCode计算，使用安全的空值处理

### 2. goBack()方法修复
- 在方法开始处清除`imageBytes = null`
- 同时回收Bitmap资源，避免内存泄漏
- 确保相机资源被正确释放

### 3. CameraPreview参数修复
- 删除了未使用的`onImageCaptured`参数
- 使用正确的回调接口模式
- 提供了完整的CameraPreview类实现

### 4. 其他改进
- 添加了资源管理（onDestroy方法）
- 改进了空值安全处理
- 添加了日志记录便于调试
- 完善了相机生命周期管理

这个修复版本解决了所有三个问题，并提供了完整的拍照解题模块实现。代码遵循Android最佳实践，确保了资源管理和内存安全。