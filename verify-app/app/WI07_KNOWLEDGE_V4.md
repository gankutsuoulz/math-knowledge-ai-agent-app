# 知识点速查模块修复

根据您提供的问题，我将修复ViewModel注入和空状态逻辑Bug。以下是修复后的完整代码：

## 修复后的ViewModel代码

```kotlin
package com.example.mathapp.ui.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mathapp.data.model.KnowledgeItem
import com.example.mathapp.data.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val repository: KnowledgeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<KnowledgeItem>>(emptyList())
    val searchResults: StateFlow<List<KnowledgeItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadKnowledgeItems()
    }

    private fun loadKnowledgeItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val items = repository.getAllKnowledgeItems()
                _searchResults.value = items
            } catch (e: Exception) {
                _error.value = "加载知识点失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = if (query.isBlank()) {
                    repository.getAllKnowledgeItems()
                } else {
                    repository.searchKnowledgeItems(query)
                }
                _searchResults.value = results
            } catch (e: Exception) {
                _error.value = "搜索失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        loadKnowledgeItems()
    }

    // 修复问题1：hasSearchResults()仅判断搜索查询是否非空
    fun hasSearchResults(): Boolean {
        return _searchQuery.value.isNotBlank()
    }

    fun clearError() {
        _error.value = null
    }
}
```

## 修复后的Fragment代码

```kotlin
package com.example.mathapp.ui.knowledge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mathapp.databinding.FragmentKnowledgeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class KnowledgeFragment : Fragment() {

    private var _binding: FragmentKnowledgeBinding? = null
    private val binding get() = _binding!!
    
    // 修复问题2：使用Hilt的@HiltViewModel注解，无需提供Factory
    private val viewModel: KnowledgeViewModel by viewModels()

    private lateinit var adapter: KnowledgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKnowledgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = KnowledgeAdapter { item ->
            // 处理知识点点击事件
            // 可以导航到详情页面
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@KnowledgeFragment.adapter
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            viewModel.search(text.toString())
        }
        
        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text?.clear()
            viewModel.clearSearch()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResults.collect { results ->
                        adapter.submitList(results)
                        updateEmptyState()
                    }
                }
                
                launch {
                    viewModel.searchQuery.collect { query ->
                        binding.clearSearchButton.visibility = 
                            if (query.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = 
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }
                
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            // 显示错误信息
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState() {
        // 修复问题1：使用修复后的hasSearchResults()方法
        val isSearching = viewModel.hasSearchResults()
        val hasResults = viewModel.searchResults.value.isNotEmpty()
        
        binding.emptyStateTextView.visibility = when {
            isSearching && !hasResults -> View.VISIBLE
            !isSearching && !hasResults -> View.VISIBLE
            else -> View.GONE
        }
        
        binding.emptyStateTextView.text = when {
            isSearching && !hasResults -> "未找到匹配的知识点"
            !isSearching && !hasResults -> "暂无知识点数据"
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

## 修复说明

### 问题1修复：空状态逻辑Bug
- **原问题**：`hasSearchResults()`同时判断搜索查询和结果集
- **修复方案**：`hasSearchResults()`现在仅判断`_searchQuery.value.isNotBlank()`
- **影响**：现在空状态逻辑更清晰，搜索状态和结果状态分离

### 问题2修复：ViewModel注入缺失
- **原问题**：Fragment使用`by viewModels()`未提供Factory
- **修复方案**：
  1. 在ViewModel类上添加`@HiltViewModel`注解
  2. 使用`@Inject constructor`进行依赖注入
  3. 在Fragment上添加`@AndroidEntryPoint`注解
  4. Fragment中使用`by viewModels()`委托（Hilt会自动提供Factory）

### 额外改进
1. 添加了错误处理和加载状态
2. 改进了搜索功能，支持实时搜索
3. 添加了空状态UI逻辑
4. 使用了StateFlow进行状态管理
5. 添加了生命周期感知的协程收集

## 使用说明

1. 确保项目已配置Hilt依赖
2. 在Application类上添加`@HiltAndroidApp`注解
3. 在Activity类上添加`@AndroidEntryPoint`注解
4. 确保`KnowledgeRepository`已通过Hilt模块提供

这些修复将使知识点速查模块更加健壮和易于维护。