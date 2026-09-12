package com.example.domain.repository

import com.example.domain.model.Variable
import kotlinx.coroutines.flow.Flow

interface VariableRepository {
    fun getVariables(): Flow<List<Variable>>
    suspend fun getVariable(nom: String): Variable?
    suspend fun saveVariable(variable: Variable)
    suspend fun deleteVariable(nom: String)
    suspend fun getValue(nom: String): String?
    suspend fun setValue(nom: String, valeur: String)
}
