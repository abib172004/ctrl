package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.LogEntry
import com.example.domain.model.StatutExecution

@Entity(tableName = "journal_execution")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val macroId: String,
    val macroNom: String,
    val timestamp: Long,
    val statut: String,
    val dureeMs: Long,
    val message: String,
    val details: String? = null
) {
    fun toDomain(): LogEntry {
        val statutEnum = try {
            StatutExecution.valueOf(statut)
        } catch (_: Exception) {
            StatutExecution.SUCCES
        }
        return LogEntry(
            id = id,
            macroId = macroId,
            macroNom = macroNom,
            timestamp = timestamp,
            statut = statutEnum,
            dureeMs = dureeMs,
            message = message,
            details = details
        )
    }

    companion object {
        fun fromDomain(l: LogEntry): LogEntryEntity {
            return LogEntryEntity(
                id = l.id,
                macroId = l.macroId,
                macroNom = l.macroNom,
                timestamp = l.timestamp,
                statut = l.statut.name,
                dureeMs = l.dureeMs,
                message = l.message,
                details = l.details
            )
        }
    }
}
