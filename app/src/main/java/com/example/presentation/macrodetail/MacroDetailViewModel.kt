package com.example.presentation.macrodetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.domain.model.ExecutionResult
import com.example.domain.model.LogEntry
import com.example.domain.model.Macro
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MacroDetailUiState(
    val macro: Macro? = null,
    val logs: List<LogEntry> = emptyList(),
    val isTesting: Boolean = false,
    val lastTestResult: ExecutionResult? = null,
    val isDeleted: Boolean = false
)

class MacroDetailViewModel(
    application: Application,
    private val macroId: String
) : AndroidViewModel(application) {

    private val dao = CtrlDatabase.getInstance(application).ctrlDao()
    private val macroRepository = MacroRepositoryImpl(dao)
    private val variableRepository = VariableRepositoryImpl(dao)
    private val logRepository = LogRepositoryImpl(dao)
    private val evaluerUseCase = EvaluerConditionsUseCase(application, variableRepository)
    private val executerUseCase = ExecuterMacroUseCase(
        application,
        evaluerUseCase,
        macroRepository,
        variableRepository,
        logRepository
    )

    private val _uiState = MutableStateFlow(MacroDetailUiState())
    val uiState: StateFlow<MacroDetailUiState> = _uiState.asStateFlow()

    init {
        chargerDonnees()
    }

    private fun chargerDonnees() {
        viewModelScope.launch {
            val macro = macroRepository.getMacroById(macroId)
            _uiState.update { it.copy(macro = macro) }
        }

        viewModelScope.launch {
            logRepository.getLogsForMacro(macroId).collect { logs ->
                _uiState.update { it.copy(logs = logs) }
            }
        }
    }

    fun toggleActive(active: Boolean) {
        viewModelScope.launch {
            macroRepository.setMacroActive(macroId, active)
            val updated = macroRepository.getMacroById(macroId)
            _uiState.update { it.copy(macro = updated) }
        }
    }

    fun testerMacro() {
        val macro = _uiState.value.macro ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, lastTestResult = null) }
            val result = executerUseCase.executer(macro, forceManuelle = true)
            val updated = macroRepository.getMacroById(macroId)
            _uiState.update {
                it.copy(
                    isTesting = false,
                    lastTestResult = result,
                    macro = updated
                )
            }
        }
    }

    fun supprimerMacro() {
        viewModelScope.launch {
            macroRepository.delete(macroId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
