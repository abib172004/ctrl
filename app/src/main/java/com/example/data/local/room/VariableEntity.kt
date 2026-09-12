package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.TypeVariable
import com.example.domain.model.Variable

@Entity(tableName = "variables_globales")
data class VariableEntity(
    @PrimaryKey val nom: String,
    val valeur: String,
    val type: String,
    val dateModification: Long = System.currentTimeMillis()
) {
    fun toDomain(): Variable {
        val typeEnum = try {
            TypeVariable.valueOf(type)
        } catch (_: Exception) {
            TypeVariable.TEXTE
        }
        return Variable(
            nom = nom,
            valeur = valeur,
            type = typeEnum,
            dateModification = dateModification
        )
    }

    companion object {
        fun fromDomain(v: Variable): VariableEntity {
            return VariableEntity(
                nom = v.nom,
                valeur = v.valeur,
                type = v.type.name,
                dateModification = v.dateModification
            )
        }
    }
}
