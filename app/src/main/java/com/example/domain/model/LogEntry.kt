package com.example.domain.model

data class LogEntry(
    val id: Long = 0,
    val macroId: String,
    val macroNom: String,
    val timestamp: Long = System.currentTimeMillis(),
    val statut: StatutExecution,
    val dureeMs: Long,
    val message: String,
    val details: String? = null
)

enum class StatutExecution(val label: String) {
    SUCCES("Succès"),
    ECHEC("Échec"),
    CONDITIONS_NON_REMPLIES("Ignorée"),
    EN_COURS("En cours")
}

data class ExecutionResult(
    val succes: Boolean,
    val statut: StatutExecution,
    val dureeMs: Long,
    val message: String,
    val erreur: String? = null
)
