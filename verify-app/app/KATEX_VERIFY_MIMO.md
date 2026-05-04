# KaTeX + Jetpack Compose 数学公式渲染项目

## 项目结构

```
/projects/math-workspace/math-app/verify-katex/
├── build.gradle.kts                    (项目级)
├── settings.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts               (模块级)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/math/verifykatex/
│       │   └── MainActivity.kt
│       └── assets/
│           └── katex.html
```

---

## 1. 项目级 `build.gradle.kts`

```kotlin
// /projects/math-workspace/math-app/verify-katex/build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}
```

## 2. `settings.gradle.kts`

```kotlin
// /projects/math-workspace/math-app/verify-katex/settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "verify-katex"
include(":app")
```

## 3. `gradle.properties`

```properties
# /projects/math-workspace/math-app/verify-katex/gradle.properties
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m
```

## 4. 模块级 `app/build.gradle.kts`

```kotlin
// /projects/math-workspace/math-app/verify-katex/app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.math.verifykatex"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.math.verifykatex"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)

    // Core Compose
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    // WebView (关键依赖)
    implementation("androidx.webkit:webkit:1.8.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

## 5. `AndroidManifest.xml`

```xml
<!-- /projects/math-workspace/math-app/verify-katex/app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="KaTeX Verify"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

## 6. `assets/katex.html` — KaTeX 渲染模板

```html
<!-- /projects/math-workspace/math-app/verify-katex/app/src/main/assets/katex.html -->
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">

    <!-- KaTeX CSS -->
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css"
          crossorigin="anonymous">

    <!-- KaTeX JS -->
    <script defer
            src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"
            crossorigin="anonymous">
    </script>

    <!-- KaTeX Auto-render (可选，用于自动渲染) -->
    <script defer
            src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js"
            crossorigin="anonymous">
    </script>

    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            padding: 16px;
            background: transparent;
            color: #1a1a2e;
            line-height: 1.6;
            -webkit-text-size-adjust: 100%;
        }

        /* 暗色主题 */
        body.dark-theme {
            color: #e0e0e0;
        }

        .formula-section {
            margin-bottom: 24px;
            padding: 16px;
            border-radius: 12px;
            background: rgba(255, 255, 255, 0.7);
            backdrop-filter: blur(10px);
            border: 1px solid rgba(0, 0, 0, 0.08);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
        }

        body.dark-theme .formula-section {
            background: rgba(40, 40, 60, 0.8);
            border: 1px solid rgba(255, 255, 255, 0.08);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
        }

        .formula-label {
            font-size: 13px;
            font-weight: 600;
            color: #6366f1;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        body.dark-theme .formula-label {
            color: #818cf8;
        }

        .formula-content {
            font-size: 20px;
            overflow-x: auto;
            padding: 8px 0;
            text-align: center;
        }

        .formula-content .katex-display {
            margin: 0;
        }

        .formula-inline {
            font-size: 18px;
            padding: 4px 0;
        }

        .section-title {
            font-size: 18px;
            font-weight: 700;
            margin: 28px 0 16px 0;
            padding-bottom: 8px;
            border-bottom: 2px solid #6366f1;
            color: #1a1a2e;
        }

        body.dark-theme .section-title {
            color: #e0e0e0;
            border-bottom-color: #818cf8;
        }

        .loading-indicator {
            text-align: center;
            padding: 40px;
            color: #999;
            font-size: 14px;
        }

        /* 确保 KaTeX 公式在 WebView 中正确显示 */
        .katex {
            font-size: 1.15em;
        }

        .katex-display > .katex {
            font-size: 1.3em;
        }

        /* 方程组样式 */
        .equation-system {
            display: flex;
            justify-content: center;
        }
    </style>
</head>
<body>
    <div id="formula-container">
        <div class="loading-indicator">⏳ 正在加载 KaTeX 公式引擎...</div>
    </div>

    <script>
        /**
         * KaTeX 渲染引擎 - Android WebView 接口
         * 通过 Android 调用 window.renderFormulas(jsonData) 传入公式
         */

        // 等待 KaTeX 加载完成
        function waitForKaTeX(callback, maxRetries) {
            maxRetries = maxRetries || 50;
            var retries = 0;

            function check() {
                if (typeof katex !== 'undefined') {
                    callback();
                } else if (retries < maxRetries) {
                    retries++;
                    setTimeout(check, 200);
                } else {
                    document.getElementById('formula-container').innerHTML =
                        '<div class="loading-indicator">❌ KaTeX 加载失败，请检查网络连接</div>';
                }
            }
            check();
        }

        /**
         * 渲染单个公式到指定元素
         */
        function renderSingleFormula(latex, displayMode) {
            try {
                return katex.renderToString(latex, {
                    throwOnError: false,
                    displayMode: displayMode !== false,
                    trust: true,
                    strict: false,
                    macros: {
                        "\\R": "\\mathbb{R}",
                        "\\N": "\\mathbb{N}",
                        "\\Z": "\\mathbb{Z}",
                        "\\C": "\\mathbb{C}",
                        "\\d": "\\mathrm{d}"
                    }
                });
            } catch (e) {
                return '<span style="color:red;">渲染错误: ' + e.message + '</span>';
            }
        }

        /**
         * 主渲染函数 - Android 端调用入口
         * @param {string} jsonData - JSON 字符串，格式:
         *   {
         *     "darkMode": false,
         *     "formulas": [
         *       { "label": "分数", "latex": "\\frac{a}{b}", "display": true },
         *       ...
         *     ]
         *   }
         */
        window.renderFormulas = function(jsonData) {
            waitForKaTeX(function() {
                try {
                    var data = JSON.parse(jsonData);
                    var container = document.getElementById('formula-container');
                    var html = '';

                    // 主题切换
                    if (data.darkMode) {
                        document.body.classList.add('dark-theme');
                    } else {
                        document.body.classList.remove('dark-theme');
                    }

                    // 渲染每个公式
                    data.formulas.forEach(function(item, index) {
                        if (item.type === 'title') {
                            html += '<div class="section-title">' + item.label + '</div>';
                        } else {
                            var rendered = renderSingleFormula(item.latex, item.display);
                            var contentClass = item.display !== false
                                ? 'formula-content'
                                : 'formula-content formula-inline';
                            html += '<div class="formula-section">';
                            html += '<div class="formula-label">' + item.label + '</div>';
                            html += '<div class="' + contentClass + '">' + rendered + '</div>';
                            html += '</div>';
                        }
                    });

                    container.innerHTML = html;

                    // 通知 Android 渲染完成
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onRenderComplete('success');
                    }
                } catch (e) {
                    document.getElementById('formula-container').innerHTML =
                        '<div class="loading-indicator">❌ 渲染失败: ' + e.message + '</div>';
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onRenderComplete('error: ' + e.message);
                    }
                }
            });
        };

        /**
         * 动态追加公式（不刷新整个页面）
         */
        window.appendFormula = function(latex, label, displayMode) {
            waitForKaTeX(function() {
                var container = document.getElementById('formula-container');
                var rendered = renderSingleFormula(latex, displayMode);
                var div = document.createElement('div');
                div