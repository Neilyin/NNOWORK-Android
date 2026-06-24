package com.neilyin.nnowork.data.plan

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = false)
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = false)
data class ResignationPlan(
    val targetAmount: Double = 0.0,
    val currentSaved: Double = 0.0,
    val hasResume: Boolean = false,
    val todoItems: List<TodoItem> = emptyList(),
    val completedTodoItems: List<TodoItem> = emptyList(),
)
