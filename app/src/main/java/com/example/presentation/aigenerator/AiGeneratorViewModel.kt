package com.example.presentation.aigenerator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.MacroRepositoryImpl
import com.example.domain.model.Macro
import com.example.domain.usecase.GenererMacroParIAUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiGeneratorUiState(
    val prompt: String = "",
    val isGenerating: Boolean = false,
    val macroGeneree: Macro? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)

class AiGeneratorViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CtrlDatabase.getInstance(application).ctrlDao()
    private val macroRepository = MacroRepositoryImpl(dao)
    private val genererMacroUseCase = GenererMacroParIAUseCase()

    private val _uiState = MutableStateFlow(AiGeneratorUiState())
    val uiState: StateFlow<AiGeneratorUiState> = _uiState.asStateFlow()

    fun onPromptChange(newPrompt: String) {
        _uiState.update { it.copy(prompt = newPrompt, errorMessage = null) }
    }

    fun appliquerExemple(exemple: String) {
        _uiState.update { it.copy(prompt = exemple, errorMessage = null) }
    }

    fun generer() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez décrire l'automatisation souhaitée.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null, macroGeneree = null) }
            val result = genererMacroUseCase.genererMacro(prompt)
            result.fold(
                onSuccess = { macro ->
                    _uiState.update { it.copy(isGenerating = false, macroGeneree = macro) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isGenerating = false, errorMessage = err.message) }
                }
            )
        }
    }

    fun validerEtEnregistrer() {
        val macro = _uiState.value.macroGeneree ?: return
        viewModelScope.launch {
            macroRepository.insertOrUpdate(macro)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun resetSaved() {
        _uiState.update { it.copy(isSaved = false, macroGeneree = null, prompt = "") }
    }
}
