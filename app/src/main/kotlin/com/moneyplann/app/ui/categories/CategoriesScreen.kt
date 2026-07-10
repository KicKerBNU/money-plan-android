package com.moneyplann.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.ui.components.CategoryIconShape
import com.moneyplann.app.ui.components.CategoryIconView
import com.moneyplann.app.ui.components.EmptyStateView
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.components.ScreenHeader
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.CategoriesTheme
import com.moneyplann.app.ui.theme.CategoryColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val categories: List<Category> = emptyList(),
)

class CategoriesViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val _state = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                _state.update { it.copy(isLoading = false, categories = api.fetchCategories()) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun create(name: String, iconKey: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.createCategory(name, iconKey)
                load()
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    fun update(category: Category, name: String, iconKey: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.updateCategory(category.id, name, iconKey)
                load()
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            try {
                api.deleteCategory(category.id)
                load()
            } catch (_: Exception) {
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val screenBackground = CategoriesTheme.ScreenBackground
    var isEditMode by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var toDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete category?") },
            text = { Text("This category will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(toDelete!!)
                    if (editingCategory?.id == toDelete?.id) {
                        editingCategory = null
                    }
                    toDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = screenBackground,
            topBar = {
                ScreenHeader(
                    title = "Categories",
                    backgroundColor = screenBackground,
                    actions = {
                        if (state.categories.isNotEmpty()) {
                            TextButton(onClick = { isEditMode = !isEditMode }) {
                                Text(
                                    if (isEditMode) "Done" else "Edit",
                                    color = AppColors.ActionBlue,
                                )
                            }
                        }
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add category")
                        }
                        TextButton(onClick = onBack) {
                            Text("Back", color = AppColors.ActionBlue)
                        }
                    },
                )
            },
        ) { padding ->
            when {
                state.isLoading -> LoadingStateView(Modifier.padding(padding))
                state.errorMessage != null -> ErrorStateView(state.errorMessage!!, Modifier.padding(padding))
                else -> PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.load() },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    if (state.categories.isEmpty()) {
                        EmptyStateView(
                            title = "No categories yet",
                            message = "Add categories to organize your expenses.",
                            actionTitle = "Add category",
                            onAction = { showAddSheet = true },
                            modifier = Modifier.padding(24.dp),
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            items(state.categories, key = { it.id }) { category ->
                                CategoryGridTile(
                                    category = category,
                                    isEditMode = isEditMode,
                                    onClick = {
                                        if (!isEditMode) editingCategory = category
                                    },
                                    onDelete = { toDelete = category },
                                )
                            }
                            item {
                                NewCategoryTile(onClick = { showAddSheet = true })
                            }
                        }
                    }
                }
            }
        }

        if (showAddSheet) {
            CategoryFormSheet(
                modifier = Modifier.fillMaxSize(),
                onDismiss = { showAddSheet = false },
                onSave = { name, iconKey ->
                    viewModel.create(name, iconKey) { showAddSheet = false }
                },
            )
        }

        editingCategory?.let { category ->
            CategoryFormSheet(
                modifier = Modifier.fillMaxSize(),
                editing = category,
                onDismiss = { editingCategory = null },
                onSave = { name, iconKey ->
                    viewModel.update(category, name, iconKey) { editingCategory = null }
                },
            )
        }
    }
}

@Composable
private fun CategoryGridTile(
    category: Category,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = CategoryColors.accentForCategory(category.name)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.TopStart) {
            CategoryIconView(
                categoryName = category.name,
                iconKey = category.icon,
                accentColor = accent,
                chipBaseSurface = CategoriesTheme.ScreenBackground,
                size = 58.dp,
                iconSize = 24.dp,
                shape = CategoryIconShape.RoundedSquare,
            )
            if (isEditMode && !category.isDefault) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .offset(x = (-8).dp, y = (-8).dp),
                ) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = "Delete category",
                        tint = CategoriesTheme.DeleteBadge,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Text(
            category.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NewCategoryTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.5.dp,
                    color = CategoriesTheme.NewTileBorder,
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add category",
                tint = CategoriesTheme.NewTileText,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            "New",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = CategoriesTheme.NewTileText,
            textAlign = TextAlign.Center,
        )
    }
}
