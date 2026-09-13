package com.example.presentation.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.accessibility.CtrlAccessibilityService
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.domain.model.Macro
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val macros: List<Macro> = emptyList(),
    val searchQuery: String = "",
    val isAccessibilityEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val testFeedbackMessage: String? = null
) {
    val macrosFiltrees: List<Macro>
        get() = if (searchQuery.isBlank()) {
            macros
        } else {
            macros.filter {
                it.nom.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.trigger.label.contains(searchQuery, ignoreCase = true)
            }
        }

    val totalActives: Int get() = macros.count { it.active }
    val totalExecutions: Int get() = macros.sumOf { it.nbExecutions }
    val tauxSuccesGlobal: String
        get() {
            if (totalExecutions == 0) return "—"
            val totalEchecs = macros.sumOf { it.nbEchecs }
            val succes = (totalExecutions - totalEchecs).coerceAtLeast(0)
            return "${((succes.toDouble() / totalExecutions) * 100).toInt()}%"
        }
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CtrlDatabase.getInstance(application).ctrlDao()
    val macroRepository = MacroRepositoryImpl(dao, application)
    val variableRepository = VariableRepositoryImpl(dao)
    val logRepository = LogRepositoryImpl(dao)
    private val evaluerConditionsUseCase = EvaluerConditionsUseCase(application, variableRepository)
    val executerMacroUseCase = ExecuterMacroUseCase(
        application,
        evaluerConditionsUseCase,
        macroRepository,
        variableRepository,
        logRepository
    )

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        chargerMacros()
        verifierAccessibilite()
    }

    fun verifierAccessibilite() {
        val enabled = CtrlAccessibilityService.isEnabled(getApplication())
        _uiState.update { it.copy(isAccessibilityEnabled = enabled) }
    }

    private fun chargerMacros() {
        viewModelScope.launch {
            macroRepository.getMacros().collect { liste ->
                _uiState.update { it.copy(macros = liste) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleMacroActive(macroId: String, active: Boolean) {
        viewModelScope.launch {
            macroRepository.setMacroActive(macroId, active)
        }
    }

    fun testerMacro(macro: Macro) {
        viewModelScope.launch {
            val result = executerMacroUseCase.executer(macro, forceManuelle = true)
            _uiState.update {
                it.copy(testFeedbackMessage = "Test \"${macro.nom}\" : ${result.message}")
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(testFeedbackMessage = null) }
    }

    fun enregistrerMacro(macro: Macro) {
        viewModelScope.launch {
            macroRepository.insertOrUpdate(macro)
            _uiState.update {
                it.copy(testFeedbackMessage = "Macro \"${macro.nom}\" enregistrée avec succès dans la base de données Room.")
            }
        }
    }

    fun supprimerMacro(macroId: String) {
        viewModelScope.launch {
            macroRepository.delete(macroId)
            _uiState.update {
                it.copy(testFeedbackMessage = "Macro supprimée de la base de données Room.")
            }
        }
    }

    fun declencherArretUrgence() {
        com.example.service.CtrlForegroundService.arretUrgence(getApplication())
        _uiState.update {
            it.copy(testFeedbackMessage = "Arrêt d'urgence déclenché : Toutes les macros ont été interrompues.")
        }
    }
}
