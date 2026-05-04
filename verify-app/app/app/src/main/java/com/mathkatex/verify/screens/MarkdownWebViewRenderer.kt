package com.mathkatex.verify.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val CDN_KATEX = "https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.16.9"

/**
 * 将 Markdown 文本转换为 HTML
 * 支持: 粗体、斜体、标题、列表、代码块、块引用、链接、换行
 * 以及 $...$ (行内公式) 和 $$...$$ (行间公式)
 */
fun markdownToHtml(markdown: String): String {
    val sb = StringBuilder()
    sb.append("<!DOCTYPE html>\n")
    sb.append("<html>\n<head>\n")
    sb.append("  <meta charset=\"UTF-8\">\n")
    sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n")
    sb.append("  <link rel=\"stylesheet\" href=\"$CDN_KATEX/katex.min.css\">\n")
    sb.append("  <script src=\"$CDN_KATEX/katex.min.js\"></script>\n")
    sb.append("  <style>\n")
    sb.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n")
    sb.append("    html, body { width: 100%; background: transparent; padding: 0; }\n")
    sb.append("    p { margin: 6px 0; line-height: 1.6; }\n")
    sb.append("    h1 { font-size: 20px; font-weight: bold; margin: 12px 0 8px; color: #212121; }\n")
    sb.append("    h2 { font-size: 18px; font-weight: bold; margin: 10px 0 6px; color: #212121; }\n")
    sb.append("    h3 { font-size: 16px; font-weight: 600; margin: 8px 0 4px; color: #424242; }\n")
    sb.append("    strong { font-weight: bold; }\n")
    sb.append("    em { font-style: italic; }\n")
    sb.append("    code { font-family: monospace; background: #F5F5F5; color: #E91E63; padding: 1px 4px; border-radius: 3px; font-size: 13px; }\n")
    sb.append("    pre { background: #263238; color: #80CBC4; padding: 10px 12px; border-radius: 6px; overflow-x: auto; margin: 8px 0; }\n")
    sb.append("    pre code { background: transparent; color: inherit; padding: 0; }\n")
    sb.append("    blockquote { border-left: 3px solid #BBDEFB; padding-left: 10px; margin: 8px 0; color: #555; }\n")
    sb.append("    ul, ol { margin: 6px 0; padding-left: 20px; }\n")
    sb.append("    li { margin: 3px 0; }\n")
    sb.append("    hr { border: none; border-top: 1px solid #E0E0E0; margin: 10px 0; }\n")
    sb.append("    a { color: #1976D2; text-decoration: underline; }\n")
    sb.append("    .katex { font-size: 1.1em; }\n")
    sb.append("    .katex-display { margin: 12px 0; text-align: center; }\n")
    sb.append("  </style>\n")
    sb.append("</head>\n<body>\n")

    // Process markdown line by line for block-level elements
    val lines = markdown.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            // Fenced code block
            line.startsWith("```") -> {
                val lang = line.removePrefix("```").trim()
                sb.append("<pre><code")
                if (lang.isNotEmpty()) sb.append(" class=\"language-$lang\"")
                sb.append(">")
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    sb.append(escapeHtml(lines[i]))
                    sb.append("\n")
                    i++
                }
                sb.append("</code></pre>\n")
                i++
            }
            // ATX heading
            line.startsWith("#") -> {
                val match = Regex("^(#{1,6})\\s+(.*)").find(line)
                if (match != null) {
                    val level = match.groupValues[1].length
                    val text = match.groupValues[2].trim()
                    sb.append("<h$level>")
                    sb.append(processInline(text))
                    sb.append("</h$level>\n")
                }
                i++
            }
            // Thematic break
            line.matches(Regex("""^[-]{3,}$|^[*]{3,}$|^[_]{3,}$""")) -> {
                sb.append("<hr>\n")
                i++
            }
            // Bullet list item
            line.matches(Regex("^[-*+]\\s+.*")) -> {
                sb.append("<ul>\n")
                while (i < lines.size && lines[i].matches(Regex("^[-*+]\\s+.*"))) {
                    sb.append("  <li>")
                    sb.append(processInline(lines[i].removePrefix(lines[i].first().toString()).trim()))
                    sb.append("</li>\n")
                    i++
                }
                sb.append("</ul>\n")
            }
            // Ordered list item
            line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                sb.append("<ol>\n")
                while (i < lines.size && lines[i].matches(Regex("^\\d+\\.\\s+.*"))) {
                    sb.append("  <li>")
                    sb.append(processInline(lines[i].replaceFirst(Regex("^\\d+\\.\\s+"), "")))
                    sb.append("</li>\n")
                    i++
                }
                sb.append("</ol>\n")
            }
            // Block quote
            line.startsWith("> ") -> {
                sb.append("<blockquote>")
                while (i < lines.size && lines[i].startsWith("> ")) {
                    sb.append(processInline(lines[i].removePrefix("> ")))
                    sb.append("<br>")
                    i++
                }
                sb.append("</blockquote>\n")
            }
            // Indented code block
            line.startsWith("    ") || line.startsWith("\t") -> {
                sb.append("<pre><code>")
                while (i < lines.size && (lines[i].startsWith("    ") || lines[i].startsWith("\t"))) {
                    sb.append(escapeHtml(lines[i].replaceFirst(Regex("^(?:    |\t)"), "")))
                    sb.append("\n")
                    i++
                }
                sb.append("</code></pre>\n")
            }
            // Empty line
            line.isBlank() -> {
                i++
            }
            // Display formula \x[...\x] on its own line
            line.trim().let { t -> t.startsWith("\\[" ) && t.endsWith("\\]" ) } -> {
                val formula = line.trim().removeSurrounding("\\[", "\\]")
                sb.append("<span class=\"katex-display\" data-formula=\"")
                sb.append(escapeHtmlAttr(formula))
                sb.append("\"></span>\n")
                i++
            }
            // Display formula $$...$$ on its own line
            line.trim().startsWith("$$") && line.trim().endsWith("$$") -> {
                val formula = line.trim().removeSurrounding("$$", "$$")
                sb.append("<span class=\"katex-display\" data-formula=\"")
                sb.append(escapeHtmlAttr(formula))
                sb.append("\"></span>\n")
                i++
            }
            // Paragraph
            else -> {
                sb.append("<p>")
                sb.append(processInline(line))
                sb.append("</p>\n")
                i++
            }
        }
    }

    sb.append("</body>\n</html>")
    return sb.toString()
}

/**
 * Process inline elements: bold, italic, code, links, and KaTeX formulas
 */
private fun processInline(text: String): String {
    val result = StringBuilder()
    var i = 0

    while (i < text.length) {
        // Check for KaTeX display mode $$...$$
        if (i < text.length - 1 && text.substring(i, i + 2) == "$$") {
            val end = text.indexOf("$$", i + 2)
            if (end != -1) {
                val formula = text.substring(i + 2, end)
                result.append("<span class=\"katex-display\" data-formula=\"")
                result.append(escapeHtmlAttr(formula))
                result.append("\"></span>")
                i = end + 2
                continue
            }
        }
        // Check for LaTeX display mode \[...\]
        if (i < text.length - 1 && text.substring(i, i + 2) == "\\[") {
            val end = text.indexOf("\\]", i + 2)
            if (end != -1) {
                val formula = text.substring(i + 2, end)
                result.append("<span class=\"katex-display\" data-formula=\"")
                result.append(escapeHtmlAttr(formula))
                result.append("\"></span>")
                i = end + 2
                continue
            }
        }
        // Check for KaTeX inline mode $...$
        if (i < text.length && text[i] == '$') {
            val end = text.indexOf('$', i + 1)
            if (end != -1 && end > i + 1) {
                val formula = text.substring(i + 1, end)
                // Make sure it's not a single $ followed by space + another $
                if (!formula.contains("\n")) {
                    result.append("<span class=\"katex-inline\" data-formula=\"")
                    result.append(escapeHtmlAttr(formula))
                    result.append("\"></span>")
                    i = end + 1
                    continue
                }
            }
        }
        // Check for LaTeX inline mode \(...\)
        if (i < text.length - 1 && text.substring(i, i + 2) == "\\(") {
            val end = text.indexOf("\\)", i + 2)
            if (end != -1) {
                val formula = text.substring(i + 2, end)
                result.append("<span class=\"katex-inline\" data-formula=\"")
                result.append(escapeHtmlAttr(formula))
                result.append("\"></span>")
                i = end + 2
                continue
            }
        }
        // Bold + italic (***text***)
        if (i <= text.length - 3 && text.substring(i, i + 3) == "***") {
            val end = text.indexOf("***", i + 3)
            if (end != -1) {
                result.append("<strong><em>")
                result.append(processInline(text.substring(i + 3, end)))
                result.append("</em></strong>")
                i = end + 3
                continue
            }
        }
        // Bold (**text**)
        if (i <= text.length - 2 && text.substring(i, i + 2) == "**") {
            val end = text.indexOf("**", i + 2)
            if (end != -1) {
                result.append("<strong>")
                result.append(processInline(text.substring(i + 2, end)))
                result.append("</strong>")
                i = end + 2
                continue
            }
        }
        // Italic (*text* or _text_)
        if (i <= text.length - 2 && (text.substring(i, i + 2) == "*" || text.substring(i, i + 2) == "_")) {
            val marker = text.substring(i, i + 2)
            val end = text.indexOf(marker, i + 2)
            if (end != -1) {
                result.append("<em>")
                result.append(processInline(text.substring(i + 2, end)))
                result.append("</em>")
                i = end + 2
                continue
            }
        }
        // Inline code (`code`)
        if (i < text.length && text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end != -1) {
                result.append("<code>")
                result.append(escapeHtml(text.substring(i + 1, end)))
                result.append("</code>")
                i = end + 1
                continue
            }
        }
        // Link [text](url)
        val linkMatch = Regex("""\[([^\]]+)\]\(([^)]+)\)""").find(text.substring(i))
        if (linkMatch != null && linkMatch.range.first == 0) {
            result.append("<a href=\"")
            result.append(escapeHtmlAttr(linkMatch.groupValues[2]))
            result.append("\">")
            result.append(linkMatch.groupValues[1])
            result.append("</a>")
            i += linkMatch.range.last + 1
            continue
        }
        // Regular character
        result.append(escapeHtml(text[i].toString()))
        i++
    }

    return result.toString()
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

private fun buildRenderScript(): String {
    return """
        (function() {
            function escapeHtml(text) {
                return text
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/\"/g, '&quot;');
            }

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
                            el.textContent = formula;
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
                            el.textContent = '$' + formula + '$';
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

private fun buildFullHtml(markdown: String): String {
    val htmlContent = markdownToHtml(markdown)
    // Extract just the body content area for injection
    // We'll use a simpler approach: embed the full page
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="$CDN_KATEX/katex.min.css">
    <script src="$CDN_KATEX/katex.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { width: 100%; background: transparent; padding: 0; }
        #content { padding: 4px 2px; }
        p { margin: 6px 0; line-height: 1.7; }
        h1 { font-size: 20px; font-weight: bold; margin: 14px 0 8px; color: #212121; }
        h2 { font-size: 18px; font-weight: bold; margin: 12px 0 6px; color: #212121; }
        h3 { font-size: 16px; font-weight: 600; margin: 8px 0 4px; color: #424242; }
        h4, h5, h6 { font-size: 15px; font-weight: 600; margin: 6px 0 3px; color: #424242; }
        strong { font-weight: bold; }
        em { font-style: italic; }
        code { font-family: monospace; background: rgba(233,30,99,0.08); color: #E91E63; padding: 1px 5px; border-radius: 3px; font-size: 0.9em; }
        pre { background: #263238; color: #80CBC4; padding: 10px 12px; border-radius: 6px; overflow-x: auto; margin: 10px 0; }
        pre code { background: transparent; color: inherit; padding: 0; font-size: 13px; line-height: 1.5; }
        blockquote { border-left: 3px solid #BBDEFB; padding-left: 12px; margin: 10px 0; color: #555; }
        ul, ol { margin: 6px 0; padding-left: 22px; }
        li { margin: 4px 0; line-height: 1.6; }
        hr { border: none; border-top: 1px solid #E0E0E0; margin: 12px 0; }
        a { color: #1976D2; text-decoration: underline; }
        .katex-inline { padding: 0 1px; }
        .katex-display { margin: 12px 0; text-align: center; }
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
                loadDataWithBaseURL(CDN_KATEX, html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(CDN_KATEX, html, "text/html", "UTF-8", null)
        },
        modifier = modifier.heightIn(min = minHeight.dp)
    )
}