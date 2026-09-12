package com.example.domain.repository

import com.example.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getLogs(): Flow<List<LogEntry>>
    fun getLogsForMacro(macroId: String): Flow<List<LogEntry>>
    suspend fun insertLog(log: LogEntry)
    suspend fun clearLogs()
}
