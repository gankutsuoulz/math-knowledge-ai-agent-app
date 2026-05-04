以下是完整的代码实现，包括 `MainActivity.kt`（含 `ViewModel`）和 `katex.html` 文件。所有资源均通过本地 assets 加载，WebView 配置严格限制网络访问，确保安全。

---

### 1. `katex.html`（存放于 `app/src/main/assets/katex/katex.html`）

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <!-- 引用本地 KaTeX 资源 -->
    <link rel="stylesheet" href="katex.min.css">
    <script src="katex.min.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            background-color: #ffffff;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 16px;
            font-family: sans-serif;
        }
        #latex-output {
            text-align: center;
            word-wrap: break-word;
        }
        .error {
            color: #d32f2f;
            font-size: 14px;
            word-break: break-all;
        }
    </style>
</head>
<body>
    <div id="latex-output">输入公式并点击渲染</div>
    <script>
        function renderLatex(latex) {
            var output = document.getElementById('latex-output');
            if (!latex || latex.trim() === '') {
                output.innerHTML = '输入公式并点击渲染';
                return;
            }
            try {
                katex.render(latex, output, {
                    throwOnError: false,
                    displayMode: true,
                    output: 'html',
                    trust: true
                });
            } catch (e) {
                output.innerHTML = '<span class="error">' + e.message + '</span>';
            }
        }
    </script>
</body>
</html>
```

---

### 2. `MainActivity.kt`

```kotlin
package com.example.katexdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaTeXTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: LatexViewModel = viewModel()
                    LatexScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun KaTeXTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

// ------------- ViewModel -------------
class LatexViewModel : ViewModel() {
    private val _input = MutableStateFlow("E = mc^2")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _isWebViewReady = MutableStateFlow(false)
    val isWebViewReady: StateFlow<Boolean> = _isWebViewReady.asStateFlow()

    fun updateInput(text: String) {
        _input.value = text
    }

    fun markWebViewReady() {
        _isWebViewReady.value = true
    }

    fun setWebViewNotReady() {
        _isWebViewReady.value = false
    }
}

// ------------- Composable Screen -------------
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LatexScreen(
    viewModel: LatexViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputState by viewModel.input.collectAsState()
    val webViewReady by viewModel.isWebViewReady.collectAsState()

    // 保存 WebView 引用
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 输入框
        OutlinedTextField(
            value = inputState,
            onValueChange = { viewModel.updateInput(it) },
            label = { Text("LaTeX 公式") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            placeholder = { Text("例如: \\int_a^b f(x)dx") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 渲染按钮
        Button(
            onClick = {
                val latex = inputState.trim()
                if (latex.isEmpty()) {
                    Toast.makeText(context, "请输入公式", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!webViewReady) {
                    Toast.makeText(context, "WebView 尚未就绪，请稍后", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // 安全转义 LaTeX 字符串用于 JavaScript
                val escapedLatex = JSONObject.quote(latex)
                val js = "renderLatex($escapedLatex)"
                webView?.evaluateJavascript(js, null)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("渲染公式")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // WebView 显示区
        AndroidView(
            factory = { ctx ->
                WebView(ctx).also { wv ->
                    webView = wv
                    configureWebView(wv, viewModel)
                }
            },
            update = { /* 无需在重组时额外操作 */ },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

// ------------- WebView 安全配置 -------------
@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(webView: WebView, viewModel: LatexViewModel) {
    webView.settings.apply {
        javaScriptEnabled = true
        // 允许访问本地 assets 文件
        allowFileAccess = true
        allowContentAccess = true
        // 禁止访问网络，仅允许本地资源
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        // 其他安全设置
        savePassword = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // 禁用缩放控件
        builtInZoomControls = false
        displayZoomControls = false
        // 禁止不必要的特性
        domStorageEnabled = false
        databaseEnabled = false
        // 防止漏洞接口
        @Suppress("DEPRECATION")
        removeJavascriptInterface("searchBoxJavaBridge_")
        removeJavascriptInterface("accessibility")
        removeJavascriptInterface("accessibilityTraversal")
    }

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            viewModel.markWebViewReady()
        }

        // 禁止网页跳