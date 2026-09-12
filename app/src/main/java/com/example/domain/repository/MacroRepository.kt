package com.example.domain.repository

import com.example.domain.model.Macro
import kotlinx.coroutines.flow.Flow

interface MacroRepository {
    fun getMacros(): Flow<List<Macro>>
    suspend fun getMacroById(id: String): Macro?
    suspend fun insertOrUpdate(macro: Macro)
    suspend fun delete(id: String)
    suspend fun setMacroActive(id: String, active: Boolean)
    suspend fun recordExecution(id: String, succes: Boolean)
    suspend fun exportMacrosJson(): String
    suspend fun importMacrosJson(json: String): Int
}
