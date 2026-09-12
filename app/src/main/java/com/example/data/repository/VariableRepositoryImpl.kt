package com.example.data.repository

import com.example.data.local.room.CtrlDao
import com.example.data.local.room.VariableEntity
import com.example.domain.model.TypeVariable
import com.example.domain.model.Variable
import com.example.domain.repository.VariableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VariableRepositoryImpl(
    private val dao: CtrlDao
) : VariableRepository {

    override fun getVariables(): Flow<List<Variable>> {
        return dao.getAllVariables().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getVariable(nom: String): Variable? {
        return dao.getVariableByName(nom)?.toDomain()
    }

    override suspend fun saveVariable(variable: Variable) {
        dao.insertOrUpdateVariable(VariableEntity.fromDomain(variable))
    }

    override suspend fun deleteVariable(nom: String) {
        dao.deleteVariableByName(nom)
    }

    override suspend fun getValue(nom: String): String? {
        return dao.getVariableByName(nom)?.valeur
    }

    override suspend fun setValue(nom: String, valeur: String) {
        val existing = dao.getVariableByName(nom)
        val type = existing?.type ?: TypeVariable.TEXTE.name
        dao.insertOrUpdateVariable(
            VariableEntity(
                nom = nom,
                valeur = valeur,
                type = type,
                dateModification = System.currentTimeMillis()
            )
        )
    }
}
