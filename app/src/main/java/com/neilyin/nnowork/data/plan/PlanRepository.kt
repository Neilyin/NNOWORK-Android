package com.neilyin.nnowork.data.plan

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.datastore.appDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PlanRepository(private val context: Context) {
    private val key = stringPreferencesKey("resignation_plan_json")
    private val adapter = JsonProvider.adapter<ResignationPlan>()

    val plan: Flow<ResignationPlan> = context.appDataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: ResignationPlan()
    }

    suspend fun current(): ResignationPlan = plan.first()

    suspend fun update(transform: (ResignationPlan) -> ResignationPlan) {
        context.appDataStore.edit { prefs ->
            val currentValue = prefs[key]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: ResignationPlan()
            prefs[key] = adapter.toJson(transform(currentValue))
        }
    }

    suspend fun reset() {
        context.appDataStore.edit { prefs ->
            prefs[key] = adapter.toJson(ResignationPlan())
        }
    }
}
