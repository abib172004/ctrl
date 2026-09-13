package com.example.data.repository

import android.content.Context
import com.example.data.local.room.CtrlDao
import com.example.data.local.room.JsonConverters
import com.example.data.local.room.MacroEntity
import com.example.data.triggers.scheduler.TriggerScheduler
import com.example.domain.model.Macro
import com.example.domain.repository.MacroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class MacroRepositoryImpl(
    private val dao: CtrlDao,
    private val context: Context? = null
) : MacroRepository {

    override fun getMacros(): Flow<List<Macro>> {
        return dao.getAllMacros().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMacroById(id: String): Macro? {
        return dao.getMacroById(id)?.toDomain()
    }

    override suspend fun insertOrUpdate(macro: Macro) {
        dao.insertOrUpdateMacro(MacroEntity.fromDomain(macro))
        context?.let { TriggerScheduler.schedule(it, macro) }
    }

    override suspend fun delete(id: String) {
        dao.deleteMacroById(id)
        context?.let { TriggerScheduler.cancel(it, id) }
    }

    override suspend fun setMacroActive(id: String, active: Boolean) {
        dao.updateMacroActive(id, active)
        context?.let { ctx ->
            if (active) {
                getMacroById(id)?.let { TriggerScheduler.schedule(ctx, it) }
            } else {
                TriggerScheduler.cancel(ctx, id)
            }
        }
    }

    override suspend fun recordExecution(id: String, succes: Boolean) {
        val echecIncrement = if (succes) 0 else 1
        dao.recordMacroExecution(id, echecIncrement, System.currentTimeMillis())
    }

    override suspend fun exportMacrosJson(): String {
        val allEntities = dao.getAllMacrosSync()
        val root = JSONObject()
        root.put("version", "3.0")
        root.put("app", "Ctrl")
        root.put("exportedAt", System.currentTimeMillis())

        val array = JSONArray()
        for (entity in allEntities) {
            val mJson = JSONObject()
            mJson.put("id", entity.id)
            mJson.put("nom", entity.nom)
            mJson.put("description", entity.description)
            mJson.put("active", entity.active)
            mJson.put("trigger", entity.triggerJson)
            mJson.put("conditions", entity.conditionsJson)
            mJson.put("actions", entity.actionsJson)
            mJson.put("dateCreation", entity.dateCreation)
            array.put(mJson)
        }
        root.put("macros", array)
        return root.toString(2)
    }

    override suspend fun importMacrosJson(json: String): Int {
        return try {
            val root = JSONObject(json)
            val array = root.optJSONArray("macros") ?: return 0
            var count = 0
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val entity = MacroEntity(
                    id = item.optString("id", java.util.UUID.randomUUID().toString()),
                    nom = item.optString("nom", "Macro Importée"),
                    description = item.optString("description", ""),
                    active = item.optBoolean("active", true),
                    triggerJson = item.optString("trigger"),
                    conditionsJson = item.optString("conditions"),
                    actionsJson = item.optString("actions"),
                    dateCreation = item.optLong("dateCreation", System.currentTimeMillis()),
                    derniereExecution = null
                )
                dao.insertOrUpdateMacro(entity)
                count++
            }
            count
        } catch (_: Exception) {
            0
        }
    }
}
