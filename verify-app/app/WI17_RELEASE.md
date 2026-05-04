# 发布准备模块 - 完整代码

## 项目结构总览

```
mathknowledge/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── themes.xml
│   │   │   │   │   └── dimens.xml
│   │   │   │   ├── values-sw600dp/
│   │   │   │   │   └── dimens.xml
│   │   │   │   ├── values-sw720dp/
│   │   │   │   │   └── dimens.xml
│   │   │   │   ├── mipmap-anydpi-v26/
│   │   │   │   │   ├── ic_launcher.xml
│   │   │   │   │   └── ic_launcher_round.xml
│   │   │   │   ├── mipmap-hdpi/
│   │   │   │   ├── mipmap-mdpi/
│   │   │   │   ├── mipmap-xhdpi/
│   │   │   │   ├── mipmap-xxhdpi/
│   │   │   │   └── mipmap-xxxhdpi/
│   │   │   └── kotlin/
│   │   │       └── com/mathknowledge/app/
│   │   │           ├── MathKnowledgeApp.kt
│   │   │           ├── MainActivity.kt
│   │   │           ├── release/
│   │   │           │   ├── ReleaseChecker.kt
│   │   │           │   ├── PrivacyPolicyManager.kt
│   │   │           │   └── AppMetadata.kt
│   │   │           ├── ui/
│   │   │           │   ├── theme/
│   │   │           │   │   └── Theme.kt
│   │   │           │   └── adaptive/
│   │   │           │       └── ScreenAdaptive.kt
│   │   │           └── util/
│   │   │               ├── SignUtil.kt
│   │   │               └── BuildConfigHelper.kt
│   │   ├── debug/
│   │   │   └── AndroidManifest.xml
│   │   └── release/
│   │       └── AndroidManifest.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties
├── keystore/
│   └── (签名文件存放目录)
├── .github/
│   └── workflows/
│       └── android-release.yml
├── scripts/
│   ├── build_release.sh
│   └── verify_signature.sh
├── docs/
│   ├── PRIVACY_POLICY.md
│   ├── RELEASE_CHECKLIST.md
│   └── STORE_LISTING.md
└── gradle/
    └── libs.versions.toml
```

---

## 1. 签名配置

### `keystore/keystore.properties` (不提交到Git)

```properties
# ============================================
# 签名配置文件 - 请勿提交到版本控制
# ============================================
storeFile=mathknowledge-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=mathknowledge
keyPassword=YOUR_KEY_PASSWORD
storeType=JKS
```

### `app/build.gradle.kts` - 签名配置部分

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
}

// ============================================
// 签名配置管理
// ============================================
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

fun loadKeystoreProperties(): Properties {
    val properties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
    if (keystorePropertiesFile.exists()) {
        try {
            val inputStream = FileInputStream(keystorePropertiesFile)
            properties.load(inputStream)
            inputStream.close()
        } catch (e: IOException) {
            println("⚠️ 无法加载 keystore.properties: ${e.message}")
        }
    } else {
        println("⚠️ keystore.properties 文件不存在，将使用默认调试签名")
    }
    return properties
}

val keystoreProperties = loadKeystoreProperties()
```

### `app/build.gradle.kts` - 完整文件

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
}

import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

// ============================================
// 签名配置管理
// ============================================
fun loadKeystoreProperties(): Properties {
    val properties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
    if (keystorePropertiesFile.exists()) {
        try {
            val inputStream = FileInputStream(keystorePropertiesFile)
            properties.load(inputStream)
            inputStream.close()
        } catch (e: IOException) {
            println("⚠️ 无法加载 keystore.properties: ${e.message}")
        }
    } else {
        println("⚠️ keystore.properties 文件不存在，将使用默认调试签名")
    }
    return properties
}

val keystoreProperties = loadKeystoreProperties()

android {
    namespace = "com.mathknowledge.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mathknowledge.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig 字段
        buildConfigField("String", "APP_NAME", "\"MathKnowledge\"")
        buildConfigField("String", "APP_VERSION", "\"${versionName}\"")
        buildConfigField("String", "BUILD_DATE", "\"${java.time.LocalDateTime.now()}\"")
        buildConfigField("String", "BUILD_TYPE", "\"${buildType}\"")

        // ProGuard
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }

    // ============================================
    // 构建类型配置
    // ============================================
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("String", "API_BASE_URL", "\"https://api-dev.mathknowledge.com/\"")
            buildConfigField("String", "API_KEY", "\"dev-api-key-placeholder\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            buildConfigField("Boolean", "ENABLE_ANALYTICS", "false")
            buildConfigField("String", "ENVIRONMENT", "\"development\"")

            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            isOptimizeCode = true

            buildConfigField("String", "API_BASE_URL", "\"https://api.mathknowledge.com/\"")
            buildConfigField("String", "API_KEY", "\"${System.getenv("PROD_API_KEY") ?: "prod-api-key-placeholder"}\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            buildConfigField("Boolean", "ENABLE_ANALYTICS", "true")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")

            // Release 签名配置
            signingConfig = if (keystoreProperties.containsKey("storeFile")) {
                signingConfigs.create("release") {
                    storeFile = rootProject.file("keystore/${keystoreProperties["storeFile"]}")
                    storePassword = keystoreProperties["storePassword"] as String
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                    storeType = keystoreProperties["storeType"] as? String ?: "JKS"
                }
            } else {
                signingConfigs.getByName("debug")
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Staging 环境
        create("staging") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"

            buildConfigField("String", "API_BASE_URL", "\"https://api-staging.mathknowledge.com/\"")
            buildConfigField("String", "API_KEY", "\"staging-api-key-placeholder\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            buildConfigField("Boolean", "ENABLE_ANALYTICS", "false")
            buildConfigField("String", "ENVIRONMENT", "\"staging\"")

            signingConfig = if (keystoreProperties.containsKey("storeFile")) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    // ============================================
    // 签名配置
    // ============================================
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/${keystoreProperties["storeFile"] ?: "mathknowledge-release.jks"}")
            storePassword = keystoreProperties["storePassword"] as? String ?: ""
            keyAlias = keystoreProperties["keyAlias"] as? String ?: ""
            keyPassword = keystoreProperties["keyPassword"] as? String ?: ""
            storeType = keystoreProperties["storeType"] as? String ?: "JKS"
        }
    }

    // ============================================
    // 编译选项
    // ============================================
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // ============================================
    // 构建特性
    // ============================================
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = false
        renderScript = false
        shaders = false
        viewBinding = false
    }

    // ============================================
    // 资源压缩
    // ============================================
    androidResources {
        // 移除未使用的资源
        removeUnusedResources = true
    }

    // ============================================
    // 包体大小优化
    // ============================================
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }

    // ============================================
    // 动态功能模块（如需要）
    // ============================================
    // dynamicFeatures = mutableSetOf(":feature:practice", ":feature:exam")
}

// ============================================
// 依赖项
// ============================================
dependencies {
    // Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // DataStore
    implementation(libs.datastore.preferences)

    // Image Loading
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)

    // Debug
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

// ============================================
// 版本号自动管理
// ============================================
fun incrementVersionCode(): Int {
    val versionPropsFile = rootProject.file("version.properties")
    val properties = Properties()

    if (versionPropsFile.exists()) {
        properties.load(versionPropsFile.inputStream())
    }

    val currentCode = properties.getProperty("VERSION_CODE", "1").toInt()
    val newCode = currentCode + 1

    properties.setProperty("VERSION_CODE", newCode.toString())
    versionPropsFile.outputStream().use { properties.store(it, null) }

    return newCode
}

android.applicationVariants.all {
    val variant = this
    if (variant.buildType.name == "release") {
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.versionCodeOverride = incrementVersionCode()
        }
    }
}
```

---

## 2. 签名验证工具

### `app/src/main/kotlin/com/mathknowledge/app/util/SignUtil.kt`

```kotlin
package com.mathknowledge.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * 签名验证工具类
 * 用于验证应用签名的完整性和正确性
 */
object SignUtil {

    private const val TAG = "SignUtil"

    // Release 签名的 SHA-256 指纹（需要替换为实际值）
    private const val RELEASE_SHA256_FINGERPRINT =
        "YOUR_RELEASE_SHA256_FINGERPRINT_HERE"

    // Debug 签名的 SHA-256 指纹
    private const val DEBUG_SHA256_FINGERPRINT =
        "YOUR_DEBUG_SHA256_FINGERPRINT_HERE"

    /**
     * 获取应用签名的 SHA-256 指纹
     */
    fun getSignatureFingerprint(context: Context): String? {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.firstOrNull()?.let { signature ->
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signature.toByteArray())
                digest.joinToString(":") { "%02X".format(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取签名指纹失败", e)
            null
        }
    }

    /**
     * 验证是否为 Release 签名
     */
    fun isReleaseSignature(context: Context): Boolean {
        val fingerprint = getSignatureFingerprint(context)
        return fingerprint == RELEASE_SHA256_FINGERPRINT
    }

    /**
     * 验证是否为 Debug 签名
     */
    fun isDebugSignature(context: Context): Boolean {
        val fingerprint = getSignatureFingerprint(context)
        return fingerprint == DEBUG_SHA256_FINGERPRINT
    }

    /**
     * 验证签名是否有效
     */
    fun isSignatureValid(context: Context): Boolean {
        val fingerprint = getSignatureFingerprint(context) ?: return false
        return fingerprint == RELEASE_SHA256_FINGERPRINT ||
                fingerprint == DEBUG_SHA256_FINGERPRINT
    }

    /**
     * 打印签名信息（仅 Debug 模式）
     */
    fun printSignatureInfo(context: Context) {
        if (!com.mathknowledge.app.BuildConfig.DEBUG) return

        val fingerprint = getSignatureFingerprint(context)
        Log.d(TAG, "========================================")
        Log.d(TAG, "应用签名信息")
        Log.d(TAG, "包名: ${context.packageName}")
        Log.d(TAG, "SHA-256: $fingerprint")
        Log.d(TAG, "环境: ${com.mathknowledge.app.BuildConfig.ENVIRONMENT}")
        Log.d(TAG, "版本: ${com.mathknowledge.app.BuildConfig.APP_VERSION}")
        Log.d(TAG, "构建日期: ${com.mathknowledge.app.BuildConfig.BUILD_DATE}")
        Log.d(TAG, "========================================")
    }

    /**
     * 获取用于 Google Play Console 的签名证书指纹
     */
    fun getPlayConsoleFingerprint(context: Context): String? {
        return getSignatureFingerprint(context)
    }
}
```

---

## 3. BuildConfig 辅助类

### `app/src/main/kotlin/com/mathknowledge/app/util/BuildConfigHelper.kt`

```kotlin
package com.mathknowledge.app.util

import com.mathknowledge.app.BuildConfig

/**
 * BuildConfig 辅助类
 * 提供统一的配置访问接口
 */
object BuildConfigHelper {

    // ============================================
    // 环境配置
    // ============================================

    enum class Environment {
        DEVELOPMENT,
        STAGING,
        PRODUCTION;

        companion object {
            fun fromString(value: String): Environment {
                return when (value.lowercase()) {
                    "development" -> DEVELOPMENT
                    "staging" -> STAGING
                    "production" -> PRODUCTION
                    else -> DEVELOPMENT
                }
            }
        }
    }

    val currentEnvironment: Environment
        get() = Environment.fromString(BuildConfig.ENVIRONMENT)

    val isProduction: Boolean
        get() = currentEnvironment == Environment.PRODUCTION

    val isStaging: Boolean
        get() = currentEnvironment == Environment.STAGING

    val isDevelopment: Boolean
        get() = currentEnvironment == Environment.DEVELOPMENT

    // ============================================
    // API 配置
    // ============================================

    val apiBaseUrl: String
        get() = BuildConfig.API_BASE_URL

    val apiKey: String
        get() = BuildConfig.API_KEY

    // ============================================
    // 功能开关
    // ============================================

    val isLoggingEnabled: Boolean
        get() = BuildConfig.ENABLE_LOGGING

    val isAnalyticsEnabled: Boolean
        get() = BuildConfig.ENABLE_ANALYTICS

    // ============================================
    // 应用信息
    // ============================================

    val appName: String
        get() = BuildConfig.APP_NAME

    val appVersion: String
        get() = BuildConfig.APP_VERSION

    val buildDate: String
        get() = BuildConfig.BUILD_DATE

    val isDebug: Boolean
        get() = BuildConfig.DEBUG

    val buildType: String
        get() = BuildConfig.BUILD_TYPE

    // ============================================
    // 调试信息
    // ============================================

    fun getDebugInfo(): String {
        return buildString {
            appendLine("========== 应用信息 ==========")
            appendLine("应用名称: $appName")
            appendLine("版本号: $appVersion")
            appendLine("构建类型: $buildType")
            appendLine("环境: $currentEnvironment")
            appendLine("构建日期: $buildDate")
            appendLine("API地址: $apiBaseUrl")
            appendLine("日志开关: $isLoggingEnabled")
            appendLine("分析开关: $isAnalyticsEnabled")
            appendLine("Debug模式: $isDebug")
            appendLine("==============================")
        }
    }
}
```

---

## 4. 屏幕适配

### `app/src/main/kotlin/com/mathknowledge/app/ui/adaptive/ScreenAdaptive.kt`

```kotlin
package com.mathknowledge.app.ui.adaptive

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * 屏幕适配工具类
 * 支持多种屏幕尺寸和密度的适配
 */
object ScreenAdaptive {

    // ============================================
    // 设计基准尺寸（以 360dp 宽度为基准）
    // ============================================
    private const val DESIGN_WIDTH_DP = 360f
    private const val DESIGN_HEIGHT_DP = 800f

    // ============================================
    // 屏幕尺寸分类
    // ============================================
    enum class ScreenSize {
        SMALL,      // < 320dp
        NORMAL,     // 320dp - 480dp
        LARGE,      // 480dp - 720dp
        EXTRA_LARGE // > 720dp
    }

    enum class ScreenOrientation {
        PORTRAIT,
        LANDSCAPE
    }

    // ============================================
    // 获取屏幕信息
    // ============================================

    fun getScreenWidthDp(context: Context): Float {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels / displayMetrics.density
    }

    fun getScreenHeightDp(context: Context): Float {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.heightPixels / displayMetrics.density
    }

    fun getScreenDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    fun getScreenDensityDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    // ============================================
    // 屏幕尺寸分类
    // ============================================

    fun getScreenSize(context: Context): ScreenSize {
        val widthDp = getScreenWidthDp(context)
        return when {
            widthDp < 320f -> ScreenSize.SMALL
            widthDp < 480f -> ScreenSize.NORMAL
            widthDp < 720f -> ScreenSize.LARGE
            else -> ScreenSize.EXTRA_LARGE
        }
    }

    fun getScreenOrientation(context: Context): ScreenOrientation {
        val orientation = context.resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ScreenOrientation.LANDSCAPE
        } else {
            ScreenOrientation.PORTRAIT
        }
    }

    // ============================================
    // 等比缩放适配
    // ============================================

    /**
     * 基于宽度的等比缩放 dp 值
     */
    @Composable
    fun scaledDp(designValue: Dp): Dp {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.toFloat()
        val scale = screenWidthDp / DESIGN_WIDTH_DP
        return (designValue.value * scale).dp
    }

    /**
     * 基于宽度的等比缩放 sp 值
     */
    @Composable
    fun scaledSp(designValue: TextUnit): TextUnit {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.toFloat()
        val scale = screenWidthDp / DESIGN_WIDTH_DP
        return (designValue.value * scale).sp
    }

    // ============================================
    // 响应式尺寸
    // ============================================

    /**
     * 根据屏幕尺寸返回响应式 padding
     */
    @Composable
    @ReadOnlyComposable
    fun responsivePadding(
        small: Dp = 8.dp,
        medium: Dp = 16.dp,
        large: Dp = 24.dp,
        extraLarge: Dp = 32.dp
    ): Dp {
        val configuration = LocalConfiguration.current
        val widthDp = configuration.screenWidthDp
        return when {
            widthDp < 320 -> small
            widthDp < 480 -> medium
            widthDp < 720 -> large
            else -> extraLarge
        }
    }

    /**
     * 根据屏幕尺寸返回响应式字体大小
     */
    @Composable
    @ReadOnlyComposable
    fun responsiveFontSize(
        small: TextUnit = 12.sp,
        medium: TextUnit = 14.sp,
        large: TextUnit = 16.sp,
        extraLarge: TextUnit = 18.sp
    ): TextUnit {
        val configuration = LocalConfiguration.current
        val widthDp = configuration.screenWidthDp
        return when {
            widthDp < 320 -> small
            widthDp < 480 -> medium
            widthDp < 720 -> large
            else -> extraLarge
        }
    }

    /**
     * 根据屏幕尺寸返回响应式网格列数
     */
    @Composable
    @ReadOnlyComposable
    fun responsiveGridColumns(
        small: Int = 1,
        medium: Int = 2,
        large: Int = 3,
        extraLarge: Int = 4
    ): Int {
        val configuration = LocalConfiguration.current
        val widthDp = configuration.screenWidthDp
        return when {
            widthDp < 320 -> small
            widthDp < 480 -> medium
            widthDp < 720 -> large
            else -> extraLarge
        }
    }
}

// ============================================
// Compose 扩展函数
// ============================================

/**
 * 获取响应式 dp 值
 */
@Composable
fun responsiveDp(
    small: Dp = 8.dp,
    medium: Dp = 16.dp,
    large: Dp = 24.dp
): Dp {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp < 360 -> small
        configuration.screenWidthDp < 600 -> medium
        else -> large
    }
}

/**
 * 获取响应式字体大小
 */
@Composable
fun responsiveSp(
    small: TextUnit = 12.sp,
    medium: TextUnit = 14.sp,
    large: TextUnit = 16.sp
): TextUnit {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp < 360 -> small
        configuration.screenWidthDp < 600 -> medium
        else -> large
    }
}

/**
 * 判断是否为平板
 */
@Composable
@ReadOnlyComposable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 600
}

/**
 * 判断是否为大屏平板
 */
@Com