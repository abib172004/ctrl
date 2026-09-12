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
    val selectedLog: LogEntry? = null
) {
    val logsFiltres: List<LogEntry>
        get() = if (filtreStatut == null) logs else logs.filter { it.statut == filtreStatut }
}

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
                _uiState.update { it.copy(logs = list) }
            }
        }
    }

    fun setFiltre(statut: StatutExecution?) {
        _uiState.update { it.copy(filtreStatut = statut) }
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
