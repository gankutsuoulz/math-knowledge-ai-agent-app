package com.mathkatex.verify.screens

import androidx.compose.foundation.BorderStroke
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.mathkatex.verify.screens.StructuredSolution
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathkatex.verify.data.local.ExampleEntity
import com.mathkatex.verify.data.local.ExampleRepository
import com.mathkatex.verify.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionScreen(
    title: String,
    emptyText: String,
    onBack: () -> Unit,
    onStartPractice: (String) -> Unit = {},
    load: suspend () -> List<ExampleEntity>,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<ExampleEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ExampleRepository.init(context)
        items = load()
        loaded = true
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding),
        ) {
            if (loaded && items.isEmpty()) {
                Text(emptyText, color = colors.text3, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.id }) { example ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.line),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        example.knowledgeTitle,
                                        fontSize = 12.sp,
                                        color = colors.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(colors.primarySoft)
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(example.grade, fontSize = 11.sp, color = colors.text3)
                                }
                                Spacer(Modifier.height(8.dp))
                                MarkdownWebView(
                                    markdown = example.question,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 40.dp, max = 200.dp),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("答案：${plainMath(example.answer)}", fontSize = 13.sp, color = colors.text, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onBack: () -> Unit = {}, onStartPractice: (String) -> Unit = {}) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var examples by remember { mutableStateOf<List<ExampleEntity>>(emptyList()) }
    var photos by remember { mutableStateOf<List<com.mathkatex.verify.data.local.PhotoSolveRecordEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var photoDetail by remember { mutableStateOf<com.mathkatex.verify.data.local.PhotoSolveRecordEntity?>(null) }
    var zoomBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(Unit) {
        ExampleRepository.init(context)
        com.mathkatex.verify.data.local.PhotoSolveRepository.init(context)
        examples = ExampleRepository.favoriteExamples()
        photos = com.mathkatex.verify.data.local.PhotoSolveRepository.favorites()
        loaded = true
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("我的收藏", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        }
    ) { padding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("加载中…", color = colors.text3, fontSize = 13.sp)
            }
        } else if (examples.isEmpty() && photos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("还没有收藏，去拍照解题或练习里点收藏吧", color = colors.text3, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(colors.background).padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(photos, key = { it.id }) { photo ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { photoDetail = photo },
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.line),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "拍照题",
                                    fontSize = 12.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.primarySoft)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(photo.createdAt)),
                                    fontSize = 11.sp, color = colors.text3,
                                )
                                IconButton(onClick = {
                                    scope.launch {
                                        com.mathkatex.verify.data.local.PhotoSolveRepository.remove(photo.id)
                                        photos = photos.filterNot { it.id == photo.id }
                                        Toast.makeText(context, "已删除收藏", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除收藏", tint = colors.text3, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            val photoBitmap = remember(photo.imagePath) {
                                runCatching { BitmapFactory.decodeFile(photo.imagePath) }.getOrNull()
                            }
                            if (photoBitmap != null) {
                                Image(
                                    bitmap = photoBitmap.asImageBitmap(),
                                    contentDescription = "题目原图",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { zoomBitmap = photoBitmap },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            MarkdownWebView(
                                markdown = photoDisplayMarkdown(photo),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 320.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { photoDetail = photo }, modifier = Modifier.fillMaxWidth()) {
                                Text("查看完整解析", fontSize = 12.sp)
                            }
                        }
                    }
                }
                items(examples, key = { it.id }) { example ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.line),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    example.knowledgeTitle,
                                    fontSize = 12.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.primarySoft)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .clickable {
                                            if (example.knowledgeId.isNotEmpty()) onStartPractice(example.knowledgeId)
                                        },
                                )
                                Spacer(Modifier.weight(1f))
                                Text(example.grade, fontSize = 11.sp, color = colors.text3)
                                IconButton(onClick = {
                                    scope.launch {
                                        ExampleRepository.toggleFavorite(example.id, false)
                                        examples = examples.filterNot { it.id == example.id }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除收藏", tint = colors.text3, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            MarkdownWebView(
                                markdown = example.question,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp, max = 200.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("答案：${plainMath(example.answer)}", fontSize = 13.sp, color = colors.text, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    photoDetail?.let { record ->
        Dialog(
            onDismissRequest = { photoDetail = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.93f),
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("拍照解题结果", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Spacer(Modifier.height(10.dp))
                    MarkdownWebView(
                        markdown = photoDisplayMarkdown(record),
                        modifier = Modifier.fillMaxWidth().height(480.dp),
                        interactive = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { photoDetail = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("关闭")
                    }
                }
            }
        }
    }

    zoomBitmap?.let { bmp ->
        Dialog(
            onDismissRequest = { zoomBitmap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                color = Color.Black,
            ) {
                Box(Modifier.fillMaxSize().clickable { zoomBitmap = null }, contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "原图放大",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                    )
                }
            }
        }
    }
}

/** 把拍照记录的结构化 JSON 还原成可渲染的 Markdown，避免收藏里显示原始 JSON。 */
private fun photoDisplayMarkdown(record: com.mathkatex.verify.data.local.PhotoSolveRecordEntity): String {
    val s = try {
        val json = record.structuredJson.takeIf { it.isNotBlank() } ?: record.rawResponse
        val start = json.indexOf('{'); val end = json.lastIndexOf('}')
        if (start < 0 || end <= start) null
        else Gson().fromJson(json.substring(start, end + 1), StructuredSolution::class.java)
    } catch (e: Exception) {
        null
    }
    if (s == null) return record.rawResponse
    return buildString {
        s.question?.takeIf { it.isNotBlank() }?.let { append(it).append("\n\n") }
        if (s.verdict != null) append("**判定**：").append(s.verdict).append("\n\n")
        if (s.knowledge.isNotEmpty()) append("**知识点**：").append(s.knowledge.joinToString("、")).append("\n\n")
        s.stuck?.takeIf { it.isNotBlank() }?.let { append("**卡点**：").append(it).append("\n\n") }
        if (s.hints.isNotEmpty()) {
            append("**引导问题**\n")
            s.hints.forEachIndexed { i, h -> append(i + 1).append(". ").append(h).append("\n") }
            append("\n")
        }
        if (s.steps.isNotEmpty()) {
            append("**完整解析**\n")
            s.steps.forEachIndexed { i, step -> append(i + 1).append(". ").append(step).append("\n") }
            append("\n")
        }
        s.answer?.takeIf { it.isNotBlank() }?.let { append("**答案**：").append(it) }
    }.trim()
}

@Composable
fun WrongBookScreen(onBack: () -> Unit = {}, onStartPractice: (String) -> Unit = {}) {
    CollectionScreen(
        title = "错题本",
        emptyText = "还没有错题，去练习页试试",
        onBack = onBack,
        load = { ExampleRepository.wrongExamples() },
    )
}
