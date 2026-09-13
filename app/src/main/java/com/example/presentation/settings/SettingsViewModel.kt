package com.example.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.BuildConfig
import com.example.data.accessibility.CtrlAccessibilityService
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.data.triggers.scheduler.TriggerScheduler
import com.example.domain.model.TypeVariable
import com.example.domain.model.Variable
import com.example.service.CtrlForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isForegroundServiceActive: Boolean = true,
    val variables: List<Variable> = emptyList(),
    val exportJsonResult: String? = null,
    val feedbackMessage: String? = null,
    val geminiApiKey: String = "",
    val isSecretKeyConfigured: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CtrlDatabase.getInstance(application).ctrlDao()
    private val variableRepository = VariableRepositoryImpl(dao)
    private val macroRepository = MacroRepositoryImpl(dao, application)
    private val prefs = application.getSharedPreferences("ctrl_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        actualiserStatutAccessibilite()
        chargerVariables()
        chargerApiKey()
    }

    private fun chargerApiKey() {
        val savedKey = prefs.getString("gemini_api_key", "") ?: ""
        val buildKey = BuildConfig.GEMINI_API_KEY
        val hasSecret = buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY"
        _uiState.update { it.copy(geminiApiKey = savedKey, isSecretKeyConfigured = hasSecret) }
    }

    fun sauvegarderGeminiApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("gemini_api_key", trimmed).apply()
        _uiState.update { 
            it.copy(
                geminiApiKey = trimmed,
                feedbackMessage = if (trimmed.isNotBlank()) "Clé API Gemini enregistrée avec succès." else "Clé API Gemini effacée."
            ) 
        }
    }

    fun actualiserStatutAccessibilite() {
        val enabled = CtrlAccessibilityService.isEnabled(getApplication())
        _uiState.update { it.copy(isAccessibilityEnabled = enabled) }
    }

    private fun chargerVariables() {
        viewModelScope.launch {
            variableRepository.getVariables().collect { list ->
                _uiState.update { it.copy(variables = list) }
            }
        }
    }

    fun toggleForegroundService(active: Boolean) {
        _uiState.update { it.copy(isForegroundServiceActive = active) }
        if (active) {
            CtrlForegroundService.demarrer(getApplication())
        } else {
            CtrlForegroundService.arreter(getApplication())
        }
    }

    fun ajouterOuModifierVariable(nom: String, valeur: String, type: TypeVariable) {
        viewModelScope.launch {
            variableRepository.saveVariable(
                Variable(nom = nom.trim(), valeur = valeur.trim(), type = type)
            )
            _uiState.update { it.copy(feedbackMessage = "Variable '$nom' enregistrée.") }
        }
    }

    fun supprimerVariable(nom: String) {
        viewModelScope.launch {
            variableRepository.deleteVariable(nom)
            _uiState.update { it.copy(feedbackMessage = "Variable '$nom' supprimée.") }
        }
    }

    fun exporterConfiguration() {
        viewModelScope.launch {
            val json = macroRepository.exportMacrosJson()
            _uiState.update { it.copy(exportJsonResult = json) }
        }
    }

    fun importerConfiguration(json: String) {
        viewModelScope.launch {
            val count = macroRepository.importMacrosJson(json)
            // Le flux d'import écrit directement en base (bypass insertOrUpdate) :
            // on replanifie explicitement les triggers temporels des macros importées.
            val macrosActives = macroRepository.getMacros().first().filter { it.active }
            TriggerScheduler.reschedulerTout(getApplication(), macrosActives)
            _uiState.update {
                it.copy(feedbackMessage = "$count macro(s) importée(s) avec succès.")
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, exportJsonResult = null) }
    }
}
