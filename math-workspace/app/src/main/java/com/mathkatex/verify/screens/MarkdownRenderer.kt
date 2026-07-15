package com.mathkatex.verify.screens

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.commonmark.node.*
import org.commonmark.parser.Parser

/**
 * Markdown + KaTeX 混合渲染组件
 * 支持 Markdown 格式（粗体、斜体、列表、代码块等）和 $...$ / $$...$$ 行内公式标记
 */
@Composable
fun MarkdownKaTeXText(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF424242)
) {
    val annotatedString = remember(content) {
        parseMdKaTeX(content, textColor)
    }
    
    BasicText(
        text = annotatedString,
        modifier = modifier,
        style = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, color = textColor)
    )
}

private fun parseMdKaTeX(markdown: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        try {
            val doc = Parser.builder().build().parse(markdown)
            renderRecurse(this, doc.firstChild, baseColor, 0)
        } catch (e: Exception) {
            append(markdown)
        }
    }
}

private fun renderRecurse(
    builder: androidx.compose.ui.text.AnnotatedString.Builder,
    node: org.commonmark.node.Node?,
    baseColor: Color,
    level: Int
) {
    when (node) {
        is Document -> {
            var child = node.firstChild
            while (child != null) { renderRecurse(builder, child, baseColor, level); child = child.next }
        }
        is Paragraph -> {
            if (level > 0) builder.append(" ")
            var child = node.firstChild
            while (child != null) { renderRecurse(builder, child, baseColor, level + 1); child = child.next }
            builder.append("\n")
        }
        is Heading -> {
            builder.append("\n")
            val sz = when (node.level) { 1 -> 20.sp; 2 -> 18.sp; 3 -> 16.sp; else -> 15.sp }
            val wt = if (node.level <= 2) FontWeight.Bold else FontWeight.Medium
            builder.pushStyle(SpanStyle(fontSize = sz, fontWeight = wt, color = baseColor))
            var child = node.firstChild
            while (child != null) { renderRecurse(builder, child, baseColor, level + 1); child = child.next }
            builder.pop(); builder.append("\n")
        }
        is StrongEmphasis -> {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor))
            var child = node.firstChild
            while (child != null) { renderRecurse(builder, child, baseColor, level); child = child.next }
            builder.pop()
        }
        is Emphasis -> {
            builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor))
            var child = node.firstChild
            while (child != null) { renderRecurse(builder, child, baseColor, level); child = child.next }
            builder.pop()
        }
        is Code -> {
            builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = baseColor.copy(alpha = 0.1f), color = Color(0xFFE91E63), fontSize = 13.sp))
            builder.append(node.literal ?: ""); builder.pop()
        }
        is FencedCodeBlock -> {
            builder.append("\n")
            builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF263238), color = Color(0xFF80CBC4), fontSize = 12.sp))
            builder.append(node.literal ?: ""); builder.pop(); builder.append("\n")
        }
        is Text -> renderKaTeX(builder, node.literal ?: "", baseColor)
        is SoftLineBreak -> builder.append(" ")
        is HardLineBreak -> builder.append("\n")
        is Link -> {
            builder.pushStyle(SpanStyle(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline))
            var child = node.firstChild
            while (child != null) { renderRecurse(builder, child, baseColor, level); child = child.next }
            builder.pop()
        }
        is BulletList -> {
            builder.append("\n")
            var child = node.firstChild
            while (child != null) {
                if (child is ListItem) {
                    builder.append("• ")
                    var ic = child.firstChild
                    while (ic != null) { renderRecurse(builder, ic, baseColor, level + 1); ic = ic.next }
                    builder.append("\n")
                }
                child = child.next
            }
        }
        is OrderedList -> {
            builder.append("\n")
            var child = node.firstChild
            var idx = 1
            while (child != null) {
                if (child is ListItem) {
                    builder.append("$idx. ")
                    var ic = child.firstChild
                    while (ic != null) { renderRecurse(builder, ic, baseColor, level + 1); ic = ic.next }
                    builder.append("\n"); idx++
                }
                child = child.next
            }
        }
        is ThematicBreak -> builder.append("\n${"─".repeat(20)}\n")
        is BlockQuote -> {
            builder.append("> ")
            var child = node.firstChild
            while (child != null) { renderRecurse(builder, child, baseColor, level + 1); child = child.next }
        }
        is IndentedCodeBlock -> {
            builder.append("\n")
            builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF263238), color = Color(0xFF80CBC4), fontSize = 12.sp))
            builder.append(node.literal ?: ""); builder.pop(); builder.append("\n")
        }
        else -> {
            var child = node?.firstChild
            while (child != null) { renderRecurse(builder, child, baseColor, level); child = child.next }
        }
    }
}

private fun renderKaTeX(builder: androidx.compose.ui.text.AnnotatedString.Builder, text: String, baseColor: Color) {
    val regex = Regex("""\$\$(.+?)\$\$|\$(.+?)\$""")
    val matches = regex.findAll(text).toList()
    var last = 0
    for (i in matches.indices) {
        val m = matches[i]
        if (m.range.first > last) {
            builder.pushStyle(SpanStyle(color = baseColor))
            builder.append(text.substring(last, m.range.first))
            builder.pop()
        }
        val display = m.groupValues[1].isNotEmpty()
        builder.pushStyle(SpanStyle(
            fontFamily = FontFamily.Monospace,
            color = if (display) Color(0xFF1976D2) else Color(0xFF7B1FA2),
            fontWeight = FontWeight.Medium,
            fontSize = if (display) 15.sp else 14.sp,
            background = if (display) Color(0xFFE3F2FD).copy(alpha = 0.5f) else Color(0xFFF3E5F5).copy(alpha = 0.5f)
        ))
        builder.append(if (display) "【公式】" else "「公式」")
        builder.pop()
        last = m.range.last + 1
    }
    if (last < text.length) {
        builder.pushStyle(SpanStyle(color = baseColor))
        builder.append(text.substring(last))
        builder.pop()
    }
}
