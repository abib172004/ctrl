package com.example.domain.model

import java.util.UUID

/**
 * Modèle central de Macro pour Ctrl
 */
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val nom: String,
    val description: String = "",
    val active: Boolean = true,
    val trigger: Trigger,
    val conditions: List<Condition> = emptyList(),
    val actions: List<ActionMacro> = emptyList(),
    val dateCreation: Long = System.currentTimeMillis(),
    val derniereExecution: Long? = null,
    val nbExecutions: Int = 0,
    val nbEchecs: Int = 0,
    val estGenereeParIA: Boolean = false
) {
    val tauxSuccesPourcentage: Int
        get() {
            if (nbExecutions == 0) return 100
            val succes = (nbExecutions - nbEchecs).coerceAtLeast(0)
            return ((succes.toDouble() / nbExecutions) * 100).toInt()
        }
}
