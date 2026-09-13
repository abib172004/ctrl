package com.example.presentation.macrobuilder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.MacroRepositoryImpl
import com.example.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MacroBuilderUiState(
    val etapeActuelle: Int = 1, // 1: Trigger, 2: Conditions, 3: Actions & Enregistrement
    val nom: String = "",
    val description: String = "",
    val triggerSelectionne: Trigger = Trigger.HeureFixe(heure = 8, minute = 0),
    val conditions: List<Condition> = emptyList(),
    val actions: List<ActionMacro> = listOf(
        ActionMacro.EnvoyerNotification(titre = "Ctrl", message = "Action automatique exécutée")
    ),
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class MacroBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CtrlDatabase.getInstance(application).ctrlDao()
    private val macroRepository = MacroRepositoryImpl(dao, application)

    private val _uiState = MutableStateFlow(MacroBuilderUiState())
    val uiState: StateFlow<MacroBuilderUiState> = _uiState.asStateFlow()

    fun setEtape(etape: Int) {
        _uiState.update { it.copy(etapeActuelle = etape) }
    }

    fun setTrigger(trigger: Trigger) {
        _uiState.update { it.copy(triggerSelectionne = trigger) }
    }

    fun ajouterCondition(condition: Condition) {
        _uiState.update { it.copy(conditions = it.conditions + condition) }
    }

    fun supprimerCondition(index: Int) {
        _uiState.update {
            val list = it.conditions.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(conditions = list)
        }
    }

    fun ajouterAction(action: ActionMacro) {
        _uiState.update { it.copy(actions = it.actions + action) }
    }

    fun supprimerAction(index: Int) {
        _uiState.update {
            val list = it.actions.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(actions = list)
        }
    }

    fun updateNom(nom: String) {
        _uiState.update { it.copy(nom = nom, errorMessage = null) }
    }

    fun updateDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun enregistrerMacro() {
        val state = _uiState.value
        if (state.nom.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez donner un nom à votre macro.") }
            return
        }

        val nouvelleMacro = Macro(
            id = "macro_" + UUID.randomUUID().toString().take(8),
            nom = state.nom.trim(),
            description = state.description.trim(),
            active = true,
            trigger = state.triggerSelectionne,
            conditions = state.conditions,
            actions = state.actions,
            dateCreation = System.currentTimeMillis()
        )

        viewModelScope.launch {
            macroRepository.insertOrUpdate(nouvelleMacro)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun reset() {
        _uiState.value = MacroBuilderUiState()
    }
}
