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
    val macrosFiltrees: List<Macro> = emptyList(),
    val searchQuery: String = "",
    val filtreStatut: StatutFiltreMacro = StatutFiltreMacro.TOUTES,
    val filtreCategorie: CategorieTrigger? = null,
    val tri: TriMacro = TriMacro.RECENT,
    val feedbackMessage: String? = null,
    val totalMacros: Int = 0,
    val totalActives: Int = 0,
    val totalInactives: Int = 0
)

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

    private fun recalculerFiltres(
        macros: List<Macro>,
        query: String,
        statut: StatutFiltreMacro,
        cat: CategorieTrigger?,
        tri: TriMacro,
        feedback: String? = _uiState.value.feedbackMessage
    ): MacrosUiState {
        var res = macros

        res = when (statut) {
            StatutFiltreMacro.TOUTES -> res
            StatutFiltreMacro.ACTIVES -> res.filter { it.active }
            StatutFiltreMacro.INACTIVES -> res.filter { !it.active }
        }

        if (cat != null) {
            res = res.filter { it.trigger.categorie == cat }
        }

        if (query.isNotBlank()) {
            res = res.filter {
                it.nom.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.trigger.label.contains(query, ignoreCase = true)
            }
        }

        val filtrees = when (tri) {
            TriMacro.RECENT -> res.sortedByDescending { it.dateCreation }
            TriMacro.NOM_AZ -> res.sortedBy { it.nom.lowercase() }
            TriMacro.EXECUTIONS -> res.sortedByDescending { it.nbExecutions }
        }

        return MacrosUiState(
            macros = macros,
            macrosFiltrees = filtrees,
            searchQuery = query,
            filtreStatut = statut,
            filtreCategorie = cat,
            tri = tri,
            feedbackMessage = feedback,
            totalMacros = macros.size,
            totalActives = macros.count { it.active },
            totalInactives = macros.count { !it.active }
        )
    }

    private fun chargerMacros() {
        viewModelScope.launch {
            macroRepository.getMacros().collect { liste ->
                _uiState.update { curr ->
                    recalculerFiltres(
                        macros = liste,
                        query = curr.searchQuery,
                        statut = curr.filtreStatut,
                        cat = curr.filtreCategorie,
                        tri = curr.tri,
                        feedback = curr.feedbackMessage
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { curr ->
            recalculerFiltres(
                macros = curr.macros,
                query = query,
                statut = curr.filtreStatut,
                cat = curr.filtreCategorie,
                tri = curr.tri
            )
        }
    }

    fun setFiltreStatut(statut: StatutFiltreMacro) {
        _uiState.update { curr ->
            recalculerFiltres(
                macros = curr.macros,
                query = curr.searchQuery,
                statut = statut,
                cat = curr.filtreCategorie,
                tri = curr.tri
            )
        }
    }

    fun setFiltreCategorie(cat: CategorieTrigger?) {
        _uiState.update { curr ->
            recalculerFiltres(
                macros = curr.macros,
                query = curr.searchQuery,
                statut = curr.filtreStatut,
                cat = cat,
                tri = curr.tri
            )
        }
    }

    fun setTri(tri: TriMacro) {
        _uiState.update { curr ->
            recalculerFiltres(
                macros = curr.macros,
                query = curr.searchQuery,
                statut = curr.filtreStatut,
                cat = curr.filtreCategorie,
                tri = tri
            )
        }
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
