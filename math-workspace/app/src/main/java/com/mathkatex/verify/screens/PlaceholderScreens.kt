package com.mathkatex.verify.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 尚未开发的入口：先保留占位，导航和文案不丢。 */

@Composable
fun ErrorGuideScreen() {
    Placeholder(icon = Icons.Default.ErrorOutline, text = "错误辅导功能开发中...")
}

@Composable
fun FormulaConvertScreen() {
    Placeholder(icon = Icons.Default.Functions, text = "公式转换功能开发中...")
}

@Composable
fun LibraryScreen() {
    Placeholder(icon = Icons.Default.LibraryBooks, text = "例题库管理功能开发中...")
}

@Composable
fun SearchScreen() {
    Placeholder(icon = Icons.Default.Search, text = "搜索功能开发中...")
}

@Composable
private fun Placeholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(text)
        }
    }
}
