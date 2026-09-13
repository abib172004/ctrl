package com.example.presentation.macros

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.room.CtrlDatabase
import com.example.data.repository.LogRepositoryImpl
import com.example.data.repository.MacroRepositoryImpl
import com.example.data.repository.VariableRepositoryImpl
import com.example.domain.model.*
import com.example.domain.usecase.EvaluerConditionsUseCase
import com.example.domain.usecase.ExecuterMacroUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class StatutFiltreMacro(val label: String) {
    TOUTES("Toutes"),
    ACTIVES("Actives"),
    INACTIVES("Inactives")
}

enum class TriMacro(val label: String) {
    RECENT("Plus récentes"),
    NOM_AZ("Nom (A-Z)"),
    EXECUTIONS("Plus exécutées")
}

data class MacrosUiState(
    val macros: List<Macro> = emptyList(),
    val searchQuery: String = "",
    val filtreStatut: StatutFiltreMacro = StatutFiltreMacro.TOUTES,
    val filtreCategorie: CategorieTrigger? = null,
    val tri: TriMacro = TriMacro.RECENT,
    val feedbackMessage: String? = null
) {
    val macrosFiltrees: List<Macro>
        get() {
            var res = macros

            // Filtre statut
            res = when (filtreStatut) {
                StatutFiltreMacro.TOUTES -> res
                StatutFiltreMacro.ACTIVES -> res.filter { it.active }
                StatutFiltreMacro.INACTIVES -> res.filter { !it.active }
            }

            // Filtre catégorie
            if (filtreCategorie != null) {
                res = res.filter { it.trigger.categorie == filtreCategorie }
            }

            // Recherche texte
            if (searchQuery.isNotBlank()) {
                res = res.filter {
                    it.nom.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.trigger.label.contains(searchQuery, ignoreCase = true)
                }
            }

            // Tri
            return when (tri) {
                TriMacro.RECENT -> res.sortedByDescending { it.dateCreation }
                TriMacro.NOM_AZ -> res.sortedBy { it.nom.lowercase() }
                TriMacro.EXECUTIONS -> res.sortedByDescending { it.nbExecutions }
            }
        }

    val totalMacros: Int get() = macros.size
    val totalActives: Int get() = macros.count { it.active }
    val totalInactives: Int get() = macros.count { !it.active }
}

class MacrosViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CtrlDatabase.getInstance(application).ctrlDao()
    private val macroRepository = MacroRepositoryImpl(dao, application)
    private val variableRepository = VariableRepositoryImpl(dao)
    private val logRepository = LogRepositoryImpl(dao)
    private val evaluerConditionsUseCase = EvaluerConditionsUseCase(application, variableRepository)
    private val executerMacroUseCase = ExecuterMacroUseCase(
        application,
        evaluerConditionsUseCase,
        macroRepository,
        variableRepository,
        logRepository
    )

    private val _uiState = MutableStateFlow(MacrosUiState())
    val uiState: StateFlow<MacrosUiState> = _uiState.asStateFlow()

    init {
        chargerMacros()
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

    fun setFiltreStatut(statut: StatutFiltreMacro) {
        _uiState.update { it.copy(filtreStatut = statut) }
    }

    fun setFiltreCategorie(cat: CategorieTrigger?) {
        _uiState.update { it.copy(filtreCategorie = cat) }
    }

    fun setTri(tri: TriMacro) {
        _uiState.update { it.copy(tri = tri) }
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
                it.copy(feedbackMessage = "Exécution de \"${macro.nom}\" : ${result.message}")
            }
        }
    }

    fun dupliquerMacro(macro: Macro) {
        viewModelScope.launch {
            val copie = macro.copy(
                id = UUID.randomUUID().toString(),
                nom = "${macro.nom} (Copie)",
                dateCreation = System.currentTimeMillis(),
                nbExecutions = 0,
                nbEchecs = 0,
                derniereExecution = null
            )
            macroRepository.insertOrUpdate(copie)
            _uiState.update {
                it.copy(feedbackMessage = "Macro dupliquée sous \"${copie.nom}\".")
            }
        }
    }

    fun supprimerMacro(macroId: String) {
        viewModelScope.launch {
            macroRepository.delete(macroId)
            _uiState.update {
                it.copy(feedbackMessage = "Macro supprimée avec succès.")
            }
        }
    }

    fun ajouterTemplate(modele: Macro) {
        viewModelScope.launch {
            macroRepository.insertOrUpdate(modele)
            _uiState.update {
                it.copy(feedbackMessage = "Modèle \"${modele.nom}\" ajouté à vos macros !")
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
