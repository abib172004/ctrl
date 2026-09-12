package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Macro

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val description: String,
    val active: Boolean,
    val triggerJson: String,
    val conditionsJson: String,
    val actionsJson: String,
    val dateCreation: Long,
    val derniereExecution: Long?,
    val nbExecutions: Int = 0,
    val nbEchecs: Int = 0,
    val estGenereeParIA: Boolean = false,
    val triggerType: String = "",
    val triggerSummary: String = "",
    val actionsCount: Int = 0,
    val actionsSummary: String = ""
) {
    fun toDomain(): Macro {
        return Macro(
            id = id,
            nom = nom,
            description = description,
            active = active,
            trigger = JsonConverters.jsonToTrigger(triggerJson),
            conditions = JsonConverters.jsonToConditions(conditionsJson),
            actions = JsonConverters.jsonToActions(actionsJson),
            dateCreation = dateCreation,
            derniereExecution = derniereExecution,
            nbExecutions = nbExecutions,
            nbEchecs = nbEchecs,
            estGenereeParIA = estGenereeParIA
        )
    }

    companion object {
        fun fromDomain(macro: Macro): MacroEntity {
            val triggerSummary = macro.trigger.label
            val triggerType = macro.trigger.id
            val actionsCount = macro.actions.size
            val actionsSummary = macro.actions.joinToString(", ") { it.label }

            return MacroEntity(
                id = macro.id,
                nom = macro.nom,
                description = macro.description,
                active = macro.active,
                triggerJson = JsonConverters.triggerToJson(macro.trigger),
                conditionsJson = JsonConverters.conditionsToJson(macro.conditions),
                actionsJson = JsonConverters.actionsToJson(macro.actions),
                dateCreation = macro.dateCreation,
                derniereExecution = macro.derniereExecution,
                nbExecutions = macro.nbExecutions,
                nbEchecs = macro.nbEchecs,
                estGenereeParIA = macro.estGenereeParIA,
                triggerType = triggerType,
                triggerSummary = triggerSummary,
                actionsCount = actionsCount,
                actionsSummary = actionsSummary
            )
        }
    }
}
