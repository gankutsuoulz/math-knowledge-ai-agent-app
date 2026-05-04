package com.mathkatex.verify.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val CDN_CLOUDFLARE = "https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.16.9"

fun buildHtmlContent(latex: String): String {
    val escapedLatex = latex
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "")
    
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="$CDN_CLOUDFLARE/katex.min.css">
    <script src="$CDN_CLOUDFLARE/katex.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body {
            width: 100%; height: auto; min-height: 60px;
            background: #fff;
            display: flex; align-items: center; justify-content: center;
            padding: 4px;
            overflow: hidden;
        }
        #formula {
            text-align: center;
            padding: 4px;
            font-size: 20px;
            color: #333;
            width: 100%;
            overflow: hidden;
        }
        .katex { font-size: 1em; }
        .katex-display { margin: 0 auto; max-width: 100%; }
    </style>
</head>
<body>
    <div id="formula"></div>
    <script>
        try {
            katex.render("$escapedLatex", document.getElementById("formula"), {
                displayMode: true,
                throwOnError: false,
                strict: false,
                trust: true,
                output: 'html'
            });
        } catch(e) {
            document.getElementById("formula").textContent = "$escapedLatex";
        }
    </script>
</body>
</html>
    """.trimIndent()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KaTeXWebView(latex: String, modifier: Modifier = Modifier) {
    val html = buildHtmlContent(latex)
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.WHITE)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        forceDark = WebSettings.FORCE_DARK_OFF
                    }
                }
                webViewClient = WebViewClient()
                isHorizontalScrollBarEnabled = false
                isVerticalScrollBarEnabled = false
                loadDataWithBaseURL(CDN_CLOUDFLARE, html, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier.heightIn(min = 60.dp)
    )
}

@Composable
fun FormulaCard(title: String, latex: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF888888)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            KaTeXWebView(
                latex = latex,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun KaTeXVerifyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "KaTeX 公式渲染验证",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val formulas = listOf(
            Triple("1. 基础分数", "\\frac{a}{b}", "分数的基本表示"),
            Triple("2. 二次根号", "\\sqrt{x^2+y^2}", "勾股定理"),
            Triple("3. 求和公式", "\\sum_{i=1}^{n} x_i", "累加求和")
        )
        
        formulas.forEach { (title, latex, desc) ->
            FormulaCard(title = title, latex = latex, description = desc)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "测试说明",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "上方显示的公式由KaTeX渲染，如能正常显示说明WebView+KaTeX方案可行。",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}