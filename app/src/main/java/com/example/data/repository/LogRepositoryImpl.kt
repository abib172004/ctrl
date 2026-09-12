package com.example.data.repository

import com.example.data.local.room.CtrlDao
import com.example.data.local.room.LogEntryEntity
import com.example.domain.model.LogEntry
import com.example.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogRepositoryImpl(
    private val dao: CtrlDao
) : LogRepository {

    override fun getLogs(): Flow<List<LogEntry>> {
        return dao.getAllLogs().map { list -> list.map { it.toDomain() } }
    }

    override fun getLogsForMacro(macroId: String): Flow<List<LogEntry>> {
        return dao.getLogsForMacro(macroId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertLog(log: LogEntry) {
        dao.insertLog(LogEntryEntity.fromDomain(log))
    }

    override suspend fun clearLogs() {
        dao.clearAllLogs()
    }
}
