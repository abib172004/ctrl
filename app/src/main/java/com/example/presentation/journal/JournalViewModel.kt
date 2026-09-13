package com.example.presentation.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.domain.model.LogEntry
import com.example.domain.model.StatutExecution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JournalUiState(
    val logs: List<LogEntry> = emptyList(),
    val filtreStatut: StatutExecution? = null,
    val selectedLog: LogEntry? = null,
    val logsFiltres: List<LogEntry> = emptyList()
)

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CtrlDatabase.getInstance(application).ctrlDao()
    private val logRepository = LogRepositoryImpl(dao)

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        chargerLogs()
    }

    private fun chargerLogs() {
        viewModelScope.launch {
            logRepository.getLogs().collect { list ->
                _uiState.update { current ->
                    current.copy(
                        logs = list,
                        logsFiltres = filtrer(list, current.filtreStatut)
                    )
                }
            }
        }
    }

    fun setFiltre(statut: StatutExecution?) {
        _uiState.update { current ->
            current.copy(
                filtreStatut = statut,
                logsFiltres = filtrer(current.logs, statut)
            )
        }
    }

    private fun filtrer(logs: List<LogEntry>, statut: StatutExecution?): List<LogEntry> {
        return if (statut == null) logs else logs.filter { it.statut == statut }
    }

    fun selectLog(log: LogEntry?) {
        _uiState.update { it.copy(selectedLog = log) }
    }

    fun effacerJournal() {
        viewModelScope.launch {
            logRepository.clearLogs()
        }
    }
}
