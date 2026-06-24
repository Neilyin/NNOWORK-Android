package com.neilyin.nnowork.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.plan.TodoItem
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PlanViewModel(
    private val repository: PlanRepository = ServiceLocator.planRepository,
) : ViewModel() {
    val plan: StateFlow<ResignationPlan> = repository.plan.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ResignationPlan(),
    )

    fun setTarget(value: Double) = update { it.copy(targetAmount = value.coerceAtLeast(0.0)) }
    fun setSaved(value: Double) = update { it.copy(currentSaved = value.coerceAtLeast(0.0)) }
    fun addFunds(value: Double) = update { it.copy(currentSaved = (it.currentSaved + value).coerceAtLeast(0.0)) }
    fun toggleResume() = update { it.copy(hasResume = !it.hasResume) }
    fun addTodo(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        update { it.copy(todoItems = it.todoItems + TodoItem(title = trimmed)) }
    }
    fun completeTodo(item: TodoItem) = update {
        it.copy(
            todoItems = it.todoItems.filterNot { todo -> todo.id == item.id },
            completedTodoItems = it.completedTodoItems + item,
        )
    }
    fun restoreTodo(item: TodoItem) = update {
        it.copy(
            completedTodoItems = it.completedTodoItems.filterNot { todo -> todo.id == item.id },
            todoItems = it.todoItems + item,
        )
    }
    fun deleteTodo(item: TodoItem) = update {
        it.copy(
            todoItems = it.todoItems.filterNot { todo -> todo.id == item.id },
            completedTodoItems = it.completedTodoItems.filterNot { todo -> todo.id == item.id },
        )
    }

    private fun update(transform: (ResignationPlan) -> ResignationPlan) {
        viewModelScope.launch { repository.update(transform) }
    }
}

@Composable
fun PlanScreen(viewModel: PlanViewModel = viewModel()) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    PlanContent(
        plan = plan,
        onTarget = viewModel::setTarget,
        onSaved = viewModel::setSaved,
        onQuickAdd = viewModel::addFunds,
        onToggleResume = viewModel::toggleResume,
        onAddTodo = viewModel::addTodo,
        onCompleteTodo = viewModel::completeTodo,
        onRestoreTodo = viewModel::restoreTodo,
        onDeleteTodo = viewModel::deleteTodo,
    )
}

@Composable
private fun PlanContent(
    plan: ResignationPlan,
    onTarget: (Double) -> Unit,
    onSaved: (Double) -> Unit,
    onQuickAdd: (Double) -> Unit,
    onToggleResume: () -> Unit,
    onAddTodo: (String) -> Unit,
    onCompleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
) {
    val progress = if (plan.targetAmount <= 0.0) 0f else (plan.currentSaved / plan.targetAmount).coerceIn(0.0, 1.0).toFloat()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelColors.darkGray)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text(
                "離職計畫",
                style = pixelTextStyle(30.sp, FontWeight.Bold, PixelColors.white),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        item { HeaderCard() }
        item {
            FundCard(
                plan = plan,
                progress = progress,
                onTarget = onTarget,
                onSaved = onSaved,
                onQuickAdd = onQuickAdd,
            )
        }
        item {
            ResumeCard(
                checked = plan.hasResume,
                onToggle = onToggleResume,
            )
        }
        item {
            TodoComposer(onAdd = onAddTodo)
        }
        items(plan.todoItems, key = { it.id }) { item ->
            TodoRow(
                item = item,
                completed = false,
                onComplete = { onCompleteTodo(item) },
                onRestore = {},
                onDelete = { onDeleteTodo(item) },
            )
        }
        if (plan.completedTodoItems.isNotEmpty()) {
            item {
                Text("已完成", style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.white))
            }
            items(plan.completedTodoItems, key = { it.id }) { item ->
                TodoRow(
                    item = item,
                    completed = true,
                    onComplete = {},
                    onRestore = { onRestoreTodo(item) },
                    onDelete = { onDeleteTodo(item) },
                )
            }
        }
        if (plan.todoItems.isEmpty() && plan.completedTodoItems.isEmpty()) {
            item {
                Text(
                    "還沒有待辦事項，先寫下一件能讓自由更靠近的小事。",
                    style = pixelTextStyle(14.sp, color = PixelColors.gray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun HeaderCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.TravelExplore, null, tint = PixelColors.blue, modifier = Modifier.size(42.dp))
        Spacer(Modifier.size(14.dp))
        Column {
            Text("自由需要準備", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.black))
            Text("好好規劃，邁向更好的未來", style = pixelTextStyle(13.sp, color = PixelColors.gray))
        }
    }
}

@Composable
private fun FundCard(
    plan: ResignationPlan,
    progress: Float,
    onTarget: (Double) -> Unit,
    onSaved: (Double) -> Unit,
    onQuickAdd: (Double) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle(icon = Icons.Filled.Savings, title = "自由預備金", color = PixelColors.green)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("目標進度", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.gray))
            Spacer(Modifier.weight(1f))
            Text("${(progress * 100).toInt()}%", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.green))
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .border(3.dp, PixelColors.black, RectangleShape),
            color = PixelColors.green,
            trackColor = PixelColors.white,
        )
        MoneyField("目標金額", plan.targetAmount, onTarget)
        MoneyField("目前已存", plan.currentSaved, onSaved)
        if (plan.targetAmount > plan.currentSaved) {
            Text(
                "還差 ${currency(plan.targetAmount - plan.currentSaved)} 就達標了",
                style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.orange),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(5_000.0, 10_000.0).forEach { amount ->
                PixelButton(
                    onClick = { onQuickAdd(amount) },
                    backgroundColor = PixelColors.green.copy(alpha = 0.25f),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+ ${currency(amount)}", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
                }
            }
        }
    }
}

@Composable
private fun MoneyField(label: String, value: Double, onSave: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(if (value == 0.0) "" else value.toLong().toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.gray))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelColors.white)
                .border(3.dp, PixelColors.black, RectangleShape)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$", style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.black))
            Spacer(Modifier.size(8.dp))
            BasicTextField(
                value = text,
                onValueChange = { valueText -> text = valueText.filter { it.isDigit() } },
                singleLine = true,
                textStyle = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.black),
                cursorBrush = SolidColor(PixelColors.black),
                modifier = Modifier.weight(1f),
            )
            PixelButton(
                onClick = { onSave(text.toDoubleOrNull() ?: 0.0) },
                backgroundColor = PixelColors.blue,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("儲存", style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.white))
            }
        }
    }
}

@Composable
private fun ResumeCard(checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Description, null, tint = PixelColors.blue, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("履歷準備", style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.black))
            Text(
                if (checked) "太好了，隨時可以出發" else "記得更新履歷和作品集",
                style = pixelTextStyle(13.sp, color = if (checked) PixelColors.green else PixelColors.gray),
            )
        }
        PixelButton(
            onClick = onToggle,
            backgroundColor = if (checked) PixelColors.green else PixelColors.gray,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        ) {
            Icon(if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = PixelColors.black)
        }
    }
}

@Composable
private fun TodoComposer(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(icon = Icons.Filled.CheckCircle, title = "待辦事項", color = PixelColors.orange)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = pixelTextStyle(16.sp, FontWeight.Medium, PixelColors.black),
                cursorBrush = SolidColor(PixelColors.black),
                modifier = Modifier
                    .weight(1f)
                    .background(PixelColors.white)
                    .border(3.dp, PixelColors.black, RectangleShape)
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("新增待辦事項...", style = pixelTextStyle(15.sp, color = PixelColors.gray))
                    inner()
                },
            )
            Spacer(Modifier.size(10.dp))
            PixelButton(
                onClick = {
                    onAdd(text)
                    text = ""
                },
                backgroundColor = if (text.isBlank()) PixelColors.gray else PixelColors.orange,
                enabled = text.isNotBlank(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            ) {
                Icon(Icons.Filled.Add, null, tint = PixelColors.black)
            }
        }
    }
}

@Composable
private fun TodoRow(
    item: TodoItem,
    completed: Boolean,
    onComplete: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = if (completed) PixelColors.white.copy(alpha = 0.75f) else PixelColors.white)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelButton(
            onClick = if (completed) onRestore else onComplete,
            backgroundColor = if (completed) PixelColors.green else PixelColors.orange,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        ) {
            Icon(if (completed) Icons.Filled.Restore else Icons.Filled.RadioButtonUnchecked, null, tint = PixelColors.black)
        }
        Spacer(Modifier.size(12.dp))
        Text(
            item.title,
            style = pixelTextStyle(16.sp, FontWeight.Medium, if (completed) PixelColors.gray else PixelColors.black)
                .copy(textDecoration = if (completed) TextDecoration.LineThrough else null),
            modifier = Modifier.weight(1f),
        )
        PixelButton(
            onClick = onDelete,
            backgroundColor = PixelColors.red,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        ) {
            Icon(Icons.Filled.Delete, null, tint = PixelColors.white)
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.size(8.dp))
        Text(title, style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.black))
    }
}

private fun currency(value: Double): String =
    NumberFormat.getNumberInstance(Locale.TAIWAN).format(value.toLong())

@Preview(name = "離職計畫", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlanContentPreview() {
    PlanContent(
        plan = ResignationPlan(
            targetAmount = 500_000.0,
            currentSaved = 180_000.0,
            hasResume = true,
            todoItems = listOf(
                TodoItem(id = "1", title = "更新履歷"),
                TodoItem(id = "2", title = "整理作品集"),
                TodoItem(id = "3", title = "研究想投的公司"),
            ),
            completedTodoItems = listOf(
                TodoItem(id = "4", title = "建立自由預備金帳戶"),
            ),
        ),
        onTarget = {},
        onSaved = {},
        onQuickAdd = {},
        onToggleResume = {},
        onAddTodo = {},
        onCompleteTodo = {},
        onRestoreTodo = {},
        onDeleteTodo = {},
    )
}
