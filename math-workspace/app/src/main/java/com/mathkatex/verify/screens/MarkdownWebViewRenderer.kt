package com.mathkatex.verify.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// 本地打包 KaTeX，断网也能渲染公式（assets/katex）
private const val KATEX_BASE = "file:///android_asset/katex"

/**
 * 将 Markdown 文本转换为 HTML，支持 KaTeX 公式渲染。
 *
 * 修正了以下问题：
 * - 公式中带 \、_ 等特殊字符被 Markdown 解析器提前拦截
 * - 公式渲染失败后 fallback 显示不友好
 * - 可读性配色（深色文字 + 白色/浅灰背景）
 */
fun markdownToHtml(markdown: String): String {
    // 第一步：保护所有公式内容，防止被后续 Markdown 处理干扰
    val (protectedMd, formulas) = protectFormulas(markdown)

    // 第二步：处理 Markdown（此时公式已被保护，不会干扰）
    val html = markdownToHtmlInner(protectedMd)

    // 第三步：恢复公式为 KaTeX 标签
    return restoreFormulas(html, formulas)
}

/**
 * 将文本中的 $...$ 和 $$...$$ 公式替换为占位符，返回占位文本和公式列表
 */
private fun protectFormulas(markdown: String): Pair<String, MutableList<FormulaEntry>> {
    val formulas = mutableListOf<FormulaEntry>()
    val sb = StringBuilder()
    var i = 0
    var placeholderIndex = 0

    while (i < markdown.length) {
        // Display mode $$...$$
        if (i <= markdown.length - 2 && markdown.substring(i, i + 2) == "$$") {
            val end = markdown.indexOf("$$", i + 2)
            if (end != -1) {
                val raw = markdown.substring(i, end + 2) // with delimiters
                formulas.add(FormulaEntry(raw, true))
                sb.append("\u0000FORMULA_DISPLAY_${placeholderIndex}\u0000")
                placeholderIndex++
                i = end + 2
                continue
            }
        }
        // Inline mode $...$
        if (markdown[i] == '$') {
            val end = findInlineFormulaEnd(markdown, i)
            if (end != -1) {
                val raw = markdown.substring(i, end + 1) // with delimiters
                formulas.add(FormulaEntry(raw, false))
                sb.append("\u0000FORMULA_INLINE_${placeholderIndex}\u0000")
                placeholderIndex++
                i = end + 1
                continue
            }
        }
        sb.append(markdown[i])
        i++
    }

    return sb.toString() to formulas
}

/**
 * 找到行内公式的结束位置 $，跳过 \$ 转义
 */
private fun findInlineFormulaEnd(text: String, start: Int): Int {
    var i = start + 1
    while (i < text.length) {
        if (text[i] == '\\') {
            i += 2 // skip escaped char
            continue
        }
        if (text[i] == '$') {
            return i
        }
        if (text[i] == '\n') {
            return -1 // multiline inline formula not supported
        }
        i++
    }
    return -1
}

data class FormulaEntry(val raw: String, val isDisplay: Boolean)

/**
 * 将受保护的占位符恢复为 KaTeX 标签
 */
private fun restoreFormulas(html: String, formulas: List<FormulaEntry>): String {
    var result = html
    for ((index, entry) in formulas.withIndex()) {
        val formulaContent = entry.raw
            // strip $/$$ delimiters
            .let { if (it.startsWith("$$") && it.endsWith("$$")) it.substring(2, it.length - 2) else it }
            .let { if (it.startsWith("$") && it.endsWith("$")) it.substring(1, it.length - 1) else it }
            .trim()
        val tag = if (entry.isDisplay) {
            "<span class=\"katex-display\" data-formula=\"${escapeHtmlAttr(formulaContent)}\"></span>"
        } else {
            "<span class=\"katex-inline\" data-formula=\"${escapeHtmlAttr(formulaContent)}\"></span>"
        }
        val placeholder = if (entry.isDisplay) "\u0000FORMULA_DISPLAY_${index}\u0000" else "\u0000FORMULA_INLINE_${index}\u0000"
        result = result.replace(placeholder, tag)
    }
    return result
}

/**
 * Markdown → HTML 转换（公式已保护，不会被干扰）
 */
private fun markdownToHtmlInner(markdown: String): String {
    val sb = StringBuilder()

    val lines = markdown.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.startsWith("```") -> {
                val lang = line.removePrefix("```").trim()
                sb.append("<pre><code")
                if (lang.isNotEmpty()) sb.append(" class=\"language-$lang\"")
                sb.append(">\n")
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    sb.append(escapeHtml(lines[i]))
                    sb.append("\n")
                    i++
                }
                sb.append("</code></pre>\n")
                i++
            }
            line.startsWith("#") -> {
                val match = Regex("^(#{1,6})\\s+(.*)").find(line)
                if (match != null) {
                    val level = match.groupValues[1].length
                    sb.append("<h$level>")
                    sb.append(match.groupValues[2])
                    sb.append("</h$level>\n")
                }
                i++
            }
            line.matches(Regex("""^-{3,}$|^[*]{3,}$|^[_]{3,}$""")) -> {
                sb.append("<hr>\n"); i++
            }
            line.matches(Regex("^[-*+]\\s+.*")) -> {
                sb.append("<ul>\n")
                while (i < lines.size && lines[i].matches(Regex("^[-*+]\\s+.*"))) {
                    sb.append("  <li>")
                    sb.append(processInlineSimple(lines[i].removePrefix(lines[i].first().toString()).trim()))
                    sb.append("</li>\n")
                    i++
                }
                sb.append("</ul>\n")
            }
            line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                sb.append("<ol>\n")
                while (i < lines.size && lines[i].matches(Regex("^\\d+\\.\\s+.*"))) {
                    sb.append("  <li>")
                    sb.append(processInlineSimple(lines[i].replaceFirst(Regex("^\\d+\\.\\s+"), "")))
                    sb.append("</li>\n")
                    i++
                }
                sb.append("</ol>\n")
            }
            line.startsWith("> ") -> {
                sb.append("<blockquote>\n")
                while (i < lines.size && lines[i].startsWith("> ")) {
                    sb.append("  <p>")
                    sb.append(processInlineSimple(lines[i].removePrefix("> ")))
                    sb.append("</p>\n")
                    i++
                }
                sb.append("</blockquote>\n")
            }
            line.isBlank() -> { i++ }
            else -> {
                sb.append("<p>")
                sb.append(processInlineSimple(line))
                sb.append("</p>\n")
                i++
            }
        }
    }

    return sb.toString()
}

/**
 * 处理行内元素（公式已被保护，只处理 **粗体**、*斜体*、`代码`）
 */
private fun processInlineSimple(text: String): String {
    var result = text

    // Bold (**text**)
    result = result.replace(Regex("""\*\*(.+?)\*\*""")) { "<strong>${it.groupValues[1]}</strong>" }
    // Italic (*text*)
    result = result.replace(Regex("""\*(.+?)\*""")) { "<em>${it.groupValues[1]}</em>" }
    // Inline code (`code`)
    result = result.replace(Regex("""`(.+?)`""")) { "<code>${escapeHtml(it.groupValues[1])}</code>" }

    return result
}

private fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

private fun escapeHtmlAttr(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

private fun buildFullHtml(markdown: String): String {
    val htmlContent = markdownToHtml(markdown)
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="$KATEX_BASE/katex.min.css">
    <script src="$KATEX_BASE/katex.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { width: 100%; background: transparent; padding: 0; }
        #content { padding: 4px 2px; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            color: #212121;
            font-size: 15px;
            line-height: 1.7;
        }
        p { margin: 6px 0; line-height: 1.7; }
        h1 { font-size: 20px; font-weight: bold; margin: 14px 0 8px; color: #1a1a1a; }
        h2 { font-size: 18px; font-weight: bold; margin: 12px 0 6px; color: #1a1a1a; }
        h3 { font-size: 16px; font-weight: 600; margin: 8px 0 4px; color: #333; }
        h4, h5, h6 { font-size: 15px; font-weight: 600; margin: 6px 0 3px; color: #333; }
        strong { font-weight: bold; color: #1a1a1a; }
        em { font-style: italic; }
        code {
            font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
            background: #f0f0f0;
            color: #d63384;
            padding: 1px 5px;
            border-radius: 4px;
            font-size: 0.9em;
        }
        pre {
            background: #1e1e2e;
            color: #cdd6f4;
            padding: 12px 14px;
            border-radius: 8px;
            overflow-x: auto;
            margin: 10px 0;
        }
        pre code { background: transparent; color: inherit; padding: 0; font-size: 13px; line-height: 1.5; }
        blockquote {
            border-left: 4px solid #90caf9;
            padding: 6px 12px;
            margin: 10px 0;
            background: #f8fbff;
            border-radius: 0 6px 6px 0;
        }
        ul, ol { margin: 6px 0; padding-left: 22px; }
        li { margin: 4px 0; line-height: 1.6; }
        hr { border: none; border-top: 1px solid #e0e0e0; margin: 12px 0; }
        a { color: #1565c0; text-decoration: underline; }
        .katex { font-size: 1.15em; }
        .katex-inline { padding: 0 1px; }
        .katex-display {
            margin: 14px 0;
            text-align: center;
            padding: 8px 0;
            background: #fafafa;
            border-radius: 6px;
        }
    </style>
</head>
<body>
    <div id="content">
$htmlContent
    </div>
    <script>
${buildRenderScript()}
    </script>
</body>
</html>
    """.trimIndent()
}

private fun buildRenderScript(): String {
    return """
        (function() {
            function renderAll() {
                // Render display formulas ($$...$$)
                document.querySelectorAll('.katex-display').forEach(function(el) {
                    var formula = el.getAttribute('data-formula');
                    if (formula) {
                        try {
                            katex.render(formula, el, {
                                displayMode: true,
                                throwOnError: false,
                                strict: false,
                                trust: true,
                                output: 'html'
                            });
                        } catch(e) {
                            el.innerHTML = '<span style="color:#e53935;font-style:italic;">' +
                                el.getAttribute('data-formula') + '</span>';
                        }
                    }
                });

                // Render inline formulas ($...$)
                document.querySelectorAll('.katex-inline').forEach(function(el) {
                    var formula = el.getAttribute('data-formula');
                    if (formula) {
                        try {
                            katex.render(formula, el, {
                                displayMode: false,
                                throwOnError: false,
                                strict: false,
                                trust: true,
                                output: 'html'
                            });
                        } catch(e) {
                            el.innerHTML = '<span style="color:#e53935;">$' +
                                el.getAttribute('data-formula') + '$</span>';
                        }
                    }
                });
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', renderAll);
            } else {
                renderAll();
            }
        })();
    """.trimIndent()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownWebView(
    markdown: String,
    modifier: Modifier = Modifier,
    minHeight: Int = 100
) {
    val html = remember(markdown) { buildFullHtml(markdown) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
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
                loadDataWithBaseURL(KATEX_BASE, html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(KATEX_BASE, html, "text/html", "UTF-8", null)
        },
        modifier = modifier.heightIn(min = minHeight.dp)
    )
}

// ====================================================================
// 为了兼容性保留的旧版本 markdownToHtml 单参数函数
// 注意：编译器可能会调用正确的重载版本
