# 修复后的发布准备模块代码

## 修复后的完整代码

```markdown
# WI17_RELEASE.md

## 发布准备模块修复

### 1. 签名配置修复（问题1和问题2）

```gradle
// 修复前：重复的signingConfigs定义
// 修复后：只保留一个signingConfigs定义
android {
    signingConfigs {
        release {
            // 从环境变量或local.properties读取签名配置
            storeFile file(project.hasProperty('storeFile') ? project.storeFile : System.getenv('STORE_FILE') ?: 'release.keystore')
            storePassword project.hasProperty('storePassword') ? project.storePassword : System.getenv('STORE_PASSWORD') ?: ''
            keyAlias project.hasProperty('keyAlias') ? project.keyAlias : System.getenv('KEY_ALIAS') ?: ''
            keyPassword project.hasProperty('keyPassword') ? project.keyPassword : System.getenv('KEY_PASSWORD') ?: ''
        }
    }
    
    buildTypes {
        release {
            // 修复前：直接使用signingConfigs.release（可能为空）
            // 修复后：使用getByName安全引用
            signingConfig signingConfigs.getByName("release")
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            // 其他配置...
        }
        debug {
            // debug构建类型不使用签名配置
            minifyEnabled false
        }
    }
}
```

### 2. 版本号管理修复（问题3）

```gradle
// 修复前：incrementVersionCode在所有构建类型中都执行
// 修复后：只对release构建递增版本号
android {
    defaultConfig {
        versionCode 1
        versionName "1.0.0"
    }
    
    // 在applicationVariants中处理版本号递增
    applicationVariants.configureEach { variant ->
        if (variant.buildType.name == 'release') {
            variant.outputs.configureEach {
                // 只对release构建递增版本号
                outputFileName = "math-app-${variant.versionName}-${variant.versionCode}.apk"
            }
        }
    }
}

// 在CI/CD中单独处理版本号递增
// 例如在GitHub Actions中：
// - name: Increment version code
//   run: |
//     VERSION_CODE=$(grep 'versionCode' app/build.gradle | grep -o '[0-9]*')
//     NEW_VERSION_CODE=$((VERSION_CODE + 1))
//     sed -i "s/versionCode $VERSION_CODE/versionCode $NEW_VERSION_CODE/" app/build.gradle
```

### 3. BuildConfig字段修复（问题4）

```gradle
android {
    defaultConfig {
        // 修复前：buildType引用错误
        // 修复后：使用正确的buildType.name
        buildConfigField "String", "BUILD_TYPE", "\"${buildType.name}\""
        buildConfigField "String", "BUILD_VARIANT", "\"${variant.name}\""
        buildConfigField "boolean", "IS_RELEASE", "${buildType.name == 'release'}"
    }
    
    // 在applicationVariants中正确引用buildType
    applicationVariants.configureEach { variant ->
        variant.buildConfigField "String", "VARIANT_NAME", "\"${variant.name}\""
        variant.buildConfigField "String", "BUILD_TYPE_NAME", "\"${variant.buildType.name}\""
    }
}
```

### 4. ScreenAdaptive.kt 完整实现（问题5）

```kotlin
// 修复前：ScreenAdaptive.kt被截断
// 修复后：补全完整的函数实现

package com.math.workspace.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

object ScreenAdaptive {
    
    // 设计稿宽度（dp）
    private const val DESIGN_WIDTH = 360f
    
    // 设计稿高度（dp）
    private const val DESIGN_HEIGHT = 640f
    
    /**
     * 获取屏幕宽度（dp）
     */
    fun getScreenWidthDp(context: Context): Float {
        val displayMetrics = getDisplayMetrics(context)
        return displayMetrics.widthPixels / displayMetrics.density
    }
    
    /**
     * 获取屏幕高度（dp）
     */
    fun getScreenHeightDp(context: Context): Float {
        val displayMetrics = getDisplayMetrics(context)
        return displayMetrics.heightPixels / displayMetrics.density
    }
    
    /**
     * 获取屏幕宽度（px）
     */
    fun getScreenWidthPx(context: Context): Int {
        val displayMetrics = getDisplayMetrics(context)
        return displayMetrics.widthPixels
    }
    
    /**
     * 获取屏幕高度（px）
     */
    fun getScreenHeightPx(context: Context): Int {
        val displayMetrics = getDisplayMetrics(context)
        return displayMetrics.heightPixels
    }
    
    /**
     * 计算宽度缩放比例
     */
    fun getWidthScale(context: Context): Float {
        val screenWidthDp = getScreenWidthDp(context)
        return screenWidthDp / DESIGN_WIDTH
    }
    
    /**
     * 计算高度缩放比例
     */
    fun getHeightScale(context: Context): Float {
        val screenHeightDp = getScreenHeightDp(context)
        return screenHeightDp / DESIGN_HEIGHT
    }
    
    /**
     * 根据设计稿dp值转换为实际dp值
     */
    fun dp2dp(context: Context, designDp: Float): Float {
        return designDp * getWidthScale(context)
    }
    
    /**
     * 根据设计稿dp值转换为实际px值
     */
    fun dp2px(context: Context, designDp: Float): Int {
        val actualDp = dp2dp(context, designDp)
        return (actualDp * getDisplayMetrics(context).density + 0.5f).toInt()
    }
    
    /**
     * 根据设计稿px值转换为实际px值
     */
    fun px2px(context: Context, designPx: Float): Int {
        val designDp = designPx / getDisplayMetrics(context).density
        return dp2px(context, designDp)
    }
    
    /**
     * 获取DisplayMetrics
     */
    private fun getDisplayMetrics(context: Context): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }
    
    /**
     * 检查是否为平板设备
     */
    fun isTablet(context: Context): Boolean {
        val screenWidthDp = getScreenWidthDp(context)
        return screenWidthDp >= 600f
    }
    
    /**
     * 获取屏幕密度
     */
    fun getScreenDensity(context: Context): Float {
        return getDisplayMetrics(context).density
    }
    
    /**
     * 获取屏幕密度DPI
     */
    fun getScreenDensityDpi(context: Context): Int {
        return getDisplayMetrics(context).densityDpi
    }
}
```

### 5. GitHub Actions CI/CD配置（问题6）

```yaml
# .github/workflows/android-release.yml
name: Android Release Build

on:
  push:
    branches: [ main, release/* ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
      
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Increment version code
      if: github.ref == 'refs/heads/main' || startsWith(github.ref, 'refs/heads/release/')
      run: |
        VERSION_CODE=$(grep 'versionCode' app/build.gradle | grep -o '[0-9]*')
        NEW_VERSION_CODE=$((VERSION_CODE + 1))
        sed -i "s/versionCode $VERSION_CODE/versionCode $NEW_VERSION_CODE/" app/build.gradle
        echo "New version code: $NEW_VERSION_CODE"
        
    - name: Build debug APK
      run: ./gradlew assembleDebug
      
    - name: Build release APK
      run: ./gradlew assembleRelease
      env:
        STORE_FILE: ${{ secrets.STORE_FILE }}
        STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        
    - name: Upload debug APK
      uses: actions/upload-artifact@v3
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/*.apk
        
    - name: Upload release APK
      uses: actions/upload-artifact@v3
      with:
        name: release-apk
        path: app/build/outputs/apk/release/*.apk
        
    - name: Run unit tests
      run: ./gradlew testDebugUnitTest
      
    - name: Run lint checks
      run: ./gradlew lintDebug
      
    - name: Generate release notes
      if: github.ref == 'refs/heads/main' || startsWith(github.ref, 'refs/heads/release/')
      run: |
        echo "## Release Notes" > release-notes.md
        echo "" >> release-notes.md
        echo "### Changes:" >> release-notes.md
        git log --oneline -10 >> release-notes.md
        
    - name: Create GitHub Release
      if: github.ref == 'refs/heads/main' || startsWith(github.ref, 'refs/heads/release/')
      uses: softprops/action-gh-release@v1
      with:
        files: |
          app/build/outputs/apk/release/*.apk
        body_path: release-notes.md
        draft: false
        prerelease: false
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 6. 完整的 build.gradle 配置

```gradle
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'kotlin-android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
    id 'androidx.navigation.safeargs.kotlin'
    id 'com.google.gms.google-services'
    id 'com.google.firebase.crashlytics'
}

android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.math.workspace"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        
        // BuildConfig字段
        buildConfigField "String", "BUILD_TYPE", "\"${buildType.name}\""
        buildConfigField "String", "BUILD_VARIANT", "\"${variant.name}\""
        buildConfigField "boolean", "IS_RELEASE", "${buildType.name == 'release'}"
        buildConfigField "String", "API_BASE_URL", "\"https://api.mathworkspace.com/\""
    }
    
    signingConfigs {
        release {
            storeFile file(project.hasProperty('storeFile') ? project.storeFile : System.getenv('STORE_FILE') ?: 'release.keystore')
            storePassword project.hasProperty('storePassword') ? project.storePassword : System.getenv('STORE_PASSWORD') ?: ''
            keyAlias project.hasProperty('keyAlias') ? project.keyAlias : System.getenv('KEY_ALIAS') ?: ''
            keyPassword project.hasProperty('keyPassword') ? project.keyPassword : System.getenv('KEY_PASSWORD') ?: ''
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.getByName("release")
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            
            // 优化配置
            crunchPngs true
            crunchPngsInDebug false
            
            // 版本信息
            versionNameSuffix "-release"
        }
        
        debug {
            minifyEnabled false
            debuggable true
            versionNameSuffix "-debug"
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = '11'
    }
    
    buildFeatures {
        viewBinding true
        dataBinding true
        buildConfig true
    }
    
    // 版本号递增逻辑
    applicationVariants.configureEach { variant ->
        if (variant.buildType.name == 'release') {
            variant.outputs.configureEach {
                outputFileName = "math-app-${variant.versionName}-${variant.versionCode}.apk"
            }
        }
        
        // 为每个变体添加BuildConfig字段
        variant.buildConfigField "String", "VARIANT_NAME", "\"${variant.name}\""
        variant.buildConfigField "String", "BUILD_TYPE_NAME", "\"${variant.buildType.name}\""
    }
    
    packagingOptions {
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/LICENSE'
        exclude 'META-INF/LICENSE.txt'
        exclude 'META-INF/NOTICE'
        exclude 'META-INF/NOTICE.txt'
    }
}

dependencies {
    // AndroidX
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
    
    // Navigation
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'
    
    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    
    // Room
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    
    // Hilt
    implementation 'com.google.dagger:hilt-android:2.48.1'
    kapt 'com.google.dagger:hilt-android-compiler:2.48.1'
    implementation 'androidx.hilt:hilt-navigation-compose:1.1.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-analytics'
    implementation 'com.google.firebase:firebase-crashlytics'
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
    
    // Glide
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    kapt 'com.github.bumptech.glide:compiler:4.16.0'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
    
    // LeakCanary (debug only)
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
}

// 项目级 build.gradle
// build.gradle (Project)
plugins {
    id 'com.android.application' version '8.2.0' apply false
    id 'com.android.library' version '8.2.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.20' apply false
    id 'com.google.dagger.hilt.android' version '2.48.1' apply false
    id 'androidx.navigation.safeargs.kotlin' version '2.7.6' apply false
    id 'com.google.gms.google-services' version '4.4.0' apply false
    id 'com.google.firebase.crashlytics' version '2.9.9' apply false
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

## 修复总结

1. **签名配置修复**：移除了重复的signingConfigs定义，只保留一个release签名配置，并使用`getByName`安全引用。

2. **版本号管理修复**：将版本号递增逻辑移到CI/CD流程中，只对release构建递增版本号。

3. **BuildConfig字段修复**：使用正确的`buildType.name`引用，避免空指针异常。

4. **ScreenAdaptive.kt修复**：补全了完整的屏幕适配工具类实现，包含所有必要的方法。

5. **CI/CD配置修复**：提供了完整的GitHub Actions配置，包括构建、测试、发布等完整流程。

6. **整体代码结构优化**：确保所有配置正确引用，避免重复定义和逻辑错误。

这些修复将确保发布准备模块正常工作，支持完整的CI/CD流程和版本管理。