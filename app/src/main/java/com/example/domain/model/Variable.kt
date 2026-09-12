package com.example.domain.model

data class Variable(
    val nom: String,
    val valeur: String,
    val type: TypeVariable = TypeVariable.TEXTE,
    val dateModification: Long = System.currentTimeMillis()
)

enum class TypeVariable(val label: String) {
    TEXTE("Texte"),
    NOMBRE("Nombre"),
    BOOLEEN("Booléen")
}
