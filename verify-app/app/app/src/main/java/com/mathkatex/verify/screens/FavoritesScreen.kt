package com.mathkatex.verify.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathkatex.verify.data.FavoriteItem
import com.mathkatex.verify.data.FavoriteType
import com.mathkatex.verify.data.FavoritesRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * 收藏夹页面 [F-05]
 * 
 * 功能：
 * - Tab导航：收藏的知识点 / 收藏的错题
 * - 显示收藏列表（标题、话题、年级、收藏时间）
 * - 删除收藏功能
 * - 空状态展示
 * - 点击查看详情
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("收藏的知识点", "收藏的错题")
    
    // 获取当前Tab对应的收藏数据
    val currentFavorites = remember(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            FavoritesRepository.getKnowledgeFavorites()
        } else {
            FavoritesRepository.getMistakeFavorites()
        }
    }
    
    // 用于触发重组的状态
    var refreshKey by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 模块标识
        Text(
            text = "[ F-05 收藏夹 ]",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Medium else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (index == 0) Icons.Default.MenuBook else Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
        
        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (currentFavorites.isEmpty()) {
                // 空状态
                EmptyFavoritesState(
                    isKnowledgeTab = selectedTabIndex == 0,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // 收藏列表
                FavoritesList(
                    favorites = currentFavorites,
                    favoriteType = if (selectedTabIndex == 0) FavoriteType.KNOWLEDGE else FavoriteType.MISTAKE,
                    onRemove = { item ->
                        FavoritesRepository.removeFavorite(item.id)
                        refreshKey++
                    },
                    key = refreshKey
                )
            }
        }
    }
}

/**
 * 空状态组件
 */
@Composable
fun EmptyFavoritesState(
    isKnowledgeTab: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 插图占位（使用emoji代替）
        Text(
            text = if (isKnowledgeTab) "📚" else "📝",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = if (isKnowledgeTab) "暂无收藏的知识点" else "暂无收藏的错题",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = if (isKnowledgeTab) {
                "从知识点速查页面收藏感兴趣的内容\n方便日后复习巩固"
            } else {
                "练习中做错的题目会自动保存在这里\n帮助您查漏补缺"
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * 收藏列表组件
 */
@Composable
fun FavoritesList(
    favorites: List<FavoriteItem>,
    favoriteType: FavoriteType,
    onRemove: (FavoriteItem) -> Unit,
    key: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = favorites,
            key = { it.id }
        ) { item ->
            FavoriteListItem(
                item = item,
                onRemove = { onRemove(item) }
            )
        }
    }
}

/**
 * 收藏列表项组件
 */
@Composable
fun FavoriteListItem(
    item: FavoriteItem,
    onRemove: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleted by remember { mutableStateOf(false) }
    
    // 删除动画颜色
    val backgroundColor by animateColorAsState(
        targetValue = if (isDeleted) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "backgroundColor"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 跳转详情 */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标
            Icon(
                imageVector = if (item.type == FavoriteType.KNOWLEDGE) {
                    Icons.Default.MenuBook
                } else {
                    Icons.Default.FavoriteBorder
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 话题标签
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = item.topic,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.height(22.dp)
                    )
                    
                    // 年级标签
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = item.grade,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.height(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "收藏于 ${formatDate(item.dateAdded)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            // 删除按钮
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("确认删除")
            },
            text = {
                Text("确定要删除「${item.title}」吗？删除后将无法恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        isDeleted = true
                        onRemove()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}