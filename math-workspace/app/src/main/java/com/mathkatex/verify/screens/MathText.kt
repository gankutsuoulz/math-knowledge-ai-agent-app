package com.mathkatex.verify.screens

/** 把简单 LaTeX 转成可读纯文本，用于按钮/选项这类不适合塞 WebView 的地方。 */
fun plainMath(input: String): String {
    var s = input.trim().replace("$", "")
    s = s.replace(Regex("""\\frac\{([^{}]*)\}\{([^{}]*)\}"""), "$1/$2")
    s = s.replace(Regex("""\\sqrt\{([^{}]*)\}"""), "√($1)")
    s = s.replace("\\le", "≤").replace("\\ge", "≥")
    s = s.replace("\\times", "×").replace("\\div", "÷").replace("\\cdot", "·")
    s = s.replace("\\pi", "π").replace("\\angle", "∠").replace("\\triangle", "△")
    val sup = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        'n' to 'ⁿ', '+' to '⁺', '-' to '⁻',
    )
    s = s.replace(Regex("""\^\{?([0-9n+\-])\}?""")) { m ->
        m.groupValues[1].map { sup[it] ?: it }.joinToString("")
    }
    s = s.replace("\\", "")
    return s.trim()
}
