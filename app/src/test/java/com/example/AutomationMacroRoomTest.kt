package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.room.CtrlDao
import com.example.data.local.room.CtrlDatabase
import com.example.data.local.room.MacroEntity
import com.example.domain.model.ActionMacro
import com.example.domain.model.Condition
import com.example.domain.model.Macro
import com.example.domain.model.Trigger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AutomationMacroRoomTest {

    private lateinit var database: CtrlDatabase
    private lateinit var dao: CtrlDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CtrlDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.ctrlDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun testStoreAndRetrieveUserDefinedMacroWithTriggerAndActions() = runBlocking {
        // 1. Créer une macro utilisateur complète avec déclencheur et actions
        val userMacro = Macro(
            id = "macro_custom_test_1",
            nom = "Alerte Batterie Faible",
            description = "Vibre et envoie une alerte dès que la batterie descend sous 20%",
            active = true,
            trigger = Trigger.NiveauBatterie(seuil = 20, inferieur = true),
            conditions = listOf(Condition.EnCharge(doitEtreEnCharge = false)),
            actions = listOf(
                ActionMacro.Vibrer(dureeMs = 500),
                ActionMacro.EnvoyerNotification(
                    titre = "Batterie faible",
                    message = "Pensez à brancher le chargeur"
                )
            )
        )

        // 2. Stocker la macro dans la base Room via l'Entity
        val entity = MacroEntity.fromDomain(userMacro)
        dao.insertOrUpdateMacro(entity)

        // 3. Vérifier les colonnes du schéma
        assertEquals("trigger_battery_lvl", entity.triggerType)
        assertEquals(2, entity.actionsCount)
        assertTrue(entity.triggerSummary.contains("20%"))

        // 4. Récupérer la macro depuis Room
        val loadedEntity = dao.getMacroById(userMacro.id)
        assertNotNull(loadedEntity)
        assertEquals("Alerte Batterie Faible", loadedEntity!!.nom)

        // 5. Convertir en modèle domaine et vérifier l'intégrité du déclencheur et des actions
        val domainMacro = loadedEntity.toDomain()
        assertTrue(domainMacro.trigger is Trigger.NiveauBatterie)
        assertEquals(20, (domainMacro.trigger as Trigger.NiveauBatterie).seuil)
        assertEquals(2, domainMacro.actions.size)
        assertTrue(domainMacro.actions[0] is ActionMacro.Vibrer)
        assertTrue(domainMacro.actions[1] is ActionMacro.EnvoyerNotification)
    }

    @Test
    fun testUpdateActiveStateAndSearchInRoom() = runBlocking {
        val macro = Macro(
            id = "macro_wifi_1",
            nom = "Silence Wi-Fi Travail",
            description = "Baisse le volume quand connecté au bureau",
            active = true,
            trigger = Trigger.WifiState(connecte = true, ssid = "Bureau_Wifi"),
            actions = listOf(
                ActionMacro.ChangerVolume(niveauPourcentage = 10, canal = "Média"),
                ActionMacro.AfficherToast(message = "Profil travail activé")
            )
        )

        dao.insertOrUpdateMacro(MacroEntity.fromDomain(macro))

        // Bascule de l'état actif
        dao.updateMacroActive(macro.id, false)
        val updated = dao.getMacroById(macro.id)
        assertNotNull(updated)
        assertFalse(updated!!.active)

        // Recherche par mot-clé
        val searchResults = dao.searchMacros("Travail").first()
        assertEquals(1, searchResults.size)
        assertEquals(macro.id, searchResults[0].id)
    }

    @Test
    fun testDeleteMacroFromRoom() = runBlocking {
        val macro = Macro(
            id = "macro_to_delete",
            nom = "Macro Temporaire",
            trigger = Trigger.Manuel(),
            actions = listOf(ActionMacro.AfficherToast("Coucou"))
        )

        dao.insertOrUpdateMacro(MacroEntity.fromDomain(macro))
        assertNotNull(dao.getMacroById(macro.id))

        dao.deleteMacroById(macro.id)
        assertNull(dao.getMacroById(macro.id))
    }
}
