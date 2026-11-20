package com.mathkatex.verify.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import com.mathkatex.verify.data.local.ExampleEntity
import com.mathkatex.verify.data.local.ExampleRepository
import com.mathkatex.verify.ui.theme.AppColors
import com.mathkatex.verify.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

private data class AnswerState(
    val selected: String = "",
    val fill: String = "",
    val submitted: Boolean = false,
    val correct: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(onBack: () -> Unit = {}) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var examples by remember { mutableStateOf<List<ExampleEntity>>(emptyList()) }
    var favorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var difficulty by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, AnswerState>() }

    LaunchedEffect(Unit) {
        ExampleRepository.init(context)
        examples = ExampleRepository.getAll()
        favorites = ExampleRepository.favoriteIds()
    }

    val shown = if (difficulty == 0) examples else examples.filter { it.difficulty == difficulty }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("例题练习", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(0 to "全部", 1 to "基础", 2 to "中等", 3 to "困难").forEach { (value, label) ->
                    FilterPill(colors, label, difficulty == value, Modifier.weight(1f)) { difficulty = value }
                }
            }

            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无例题", color = colors.text3, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(shown, key = { it.id }) { example ->
                        val state = answers[example.id] ?: AnswerState()
                        ExampleCard(
                            example = example,
                            state = state,
                            favorite = example.id in favorites,
                            onSelect = { answers[example.id] = state.copy(selected = it) },
                            onFill = { answers[example.id] = state.copy(fill = it) },
                            onSubmit = {
                                val userAnswer = if (example.type == "choice") state.selected else state.fill
                                val correct = normalizeAnswer(userAnswer) == normalizeAnswer(example.answer)
                                answers[example.id] = state.copy(submitted = true, correct = correct)
                                scope.launch { ExampleRepository.record(example.id, correct, userAnswer) }
                            },
                            onToggleFavorite = {
                                val next = example.id !in favorites
                                favorites = if (next) favorites + example.id else favorites - example.id
                                scope.launch { ExampleRepository.toggleFavorite(example.id, next) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(colors: AppColors, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.primary else colors.surface)
            .border(1.dp, if (selected) colors.primary else colors.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.5.sp,
            color = if (selected) Color.White else colors.text2,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ExampleCard(
    example: ExampleEntity,
    state: AnswerState,
    favorite: Boolean,
    onSelect: (String) -> Unit,
    onFill: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val colors = LocalAppColors.current
    val options = remember(example.id) { parseOptions(example.options) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    difficultyName(example.difficulty),
                    fontSize = 11.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.primarySoft)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(example.knowledgeTitle, fontSize = 11.sp, color = colors.text3)
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (favorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "收藏",
                        tint = if (favorite) colors.primary else colors.text3,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            MarkdownWebView(
                markdown = example.question,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp, max = 220.dp),
            )

            if (example.type == "choice" && options.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                options.forEach { option ->
                    val selected = state.selected == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.primarySoft else colors.surface2)
                            .border(
                                1.dp,
                                if (selected) colors.primary else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable(enabled = !state.submitted) { onSelect(option) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            plainMath(option),
                            fontSize = 13.sp,
                            color = if (selected) colors.primary else colors.text,
                        )
                    }
                }
            } else if (example.type == "fill") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.fill,
                    onValueChange = onFill,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入答案", fontSize = 13.sp, color = colors.text3) },
                    singleLine = true,
                    enabled = !state.submitted,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.line,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))
            if (!state.submitted) {
                Button(
                    onClick = onSubmit,
                    enabled = state.selected.isNotBlank() || state.fill.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                ) {
                    Text("提交答案", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    if (state.correct) "答对了" else "还差一点",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.correct) colors.success else colors.danger,
                )
                Spacer(Modifier.height(4.dp))
                Text("答案：${plainMath(example.answer)}", fontSize = 13.sp, color = colors.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = colors.line, thickness = 1.dp)
                Spacer(Modifier.height(6.dp))
                MarkdownWebView(
                    markdown = example.solution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp, max = 260.dp),
                )
            }
        }
    }
}

private fun parseOptions(json: String): List<String> = try {
    JsonParser.parseString(json).asJsonArray.map { it.asString }
} catch (_: Exception) {
    emptyList()
}

private fun normalizeAnswer(value: String): String = value
    .replace(Regex("\\s+"), "")
    .replace("$", "")
    .replace("\\", "")
    .replace("，", ",")
    .replace("（", "(")
    .replace("）", ")")
    .lowercase()

private fun difficultyName(level: Int): String = when (level) {
    1 -> "基础"
    2 -> "中等"
    else -> "困难"
}
