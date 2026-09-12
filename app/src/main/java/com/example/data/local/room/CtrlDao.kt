package com.example.data.local.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CtrlDao {

    // --- MACROS ---
    @Query("SELECT * FROM macros ORDER BY dateCreation DESC")
    fun getAllMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE id = :id")
    suspend fun getMacroById(id: String): MacroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMacro(macro: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacroById(id: String)

    @Delete
    suspend fun deleteMacro(macro: MacroEntity)

    @Query("UPDATE macros SET active = :active WHERE id = :id")
    suspend fun updateMacroActive(id: String, active: Boolean)

    @Query("UPDATE macros SET nbExecutions = nbExecutions + 1, nbEchecs = nbEchecs + :echecIncrement, derniereExecution = :timestamp WHERE id = :id")
    suspend fun recordMacroExecution(id: String, echecIncrement: Int, timestamp: Long)

    @Query("SELECT * FROM macros")
    suspend fun getAllMacrosSync(): List<MacroEntity>

    @Query("SELECT * FROM macros WHERE active = 1 ORDER BY dateCreation DESC")
    fun getActiveMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE nom LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR triggerSummary LIKE '%' || :query || '%' OR actionsSummary LIKE '%' || :query || '%' ORDER BY dateCreation DESC")
    fun searchMacros(query: String): Flow<List<MacroEntity>>

    @Query("SELECT COUNT(*) FROM macros")
    suspend fun getMacrosCount(): Int

    @Query("SELECT COUNT(*) FROM macros WHERE active = 1")
    suspend fun getActiveMacrosCount(): Int

    // --- VARIABLES ---
    @Query("SELECT * FROM variables_globales ORDER BY nom ASC")
    fun getAllVariables(): Flow<List<VariableEntity>>

    @Query("SELECT * FROM variables_globales WHERE nom = :nom")
    suspend fun getVariableByName(nom: String): VariableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateVariable(variable: VariableEntity)

    @Query("DELETE FROM variables_globales WHERE nom = :nom")
    suspend fun deleteVariableByName(nom: String)

    // --- LOGS ---
    @Query("SELECT * FROM journal_execution ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM journal_execution WHERE macroId = :macroId ORDER BY timestamp DESC LIMIT 100")
    fun getLogsForMacro(macroId: String): Flow<List<LogEntryEntity>>

    @Insert
    suspend fun insertLog(log: LogEntryEntity)

    @Query("DELETE FROM journal_execution")
    suspend fun clearAllLogs()
}
