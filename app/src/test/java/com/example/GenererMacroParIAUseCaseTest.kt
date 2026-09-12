package com.example

import com.example.domain.model.ActionMacro
import com.example.domain.model.Trigger
import com.example.domain.usecase.GenererMacroParIAUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GenererMacroParIAUseCaseTest {

    private val useCase = GenererMacroParIAUseCase()

    @Test
    fun testGenererMacroBatterie() = runBlocking {
        val prompt = "Alerte et vibre si la batterie descend sous 15%"
        val result = useCase.genererMacro(prompt)
        assertTrue(result.isSuccess)

        val macro = result.getOrNull()
        assertNotNull(macro)
        assertTrue(macro!!.trigger is Trigger.NiveauBatterie)
        assertEquals(15, (macro.trigger as Trigger.NiveauBatterie).seuil)
        assertTrue(macro.actions.any { it is ActionMacro.Vibrer })
        assertTrue(macro.estGenereeParIA)
    }

    @Test
    fun testGenererMacroHeure() = runBlocking {
        val prompt = "Coupe le Bluetooth et baisse le volume à 23h00"
        val result = useCase.genererMacro(prompt)
        assertTrue(result.isSuccess)

        val macro = result.getOrNull()
        assertNotNull(macro)
        assertTrue(macro!!.trigger is Trigger.HeureFixe)
        assertEquals(23, (macro.trigger as Trigger.HeureFixe).heure)
        assertEquals(0, (macro.trigger as Trigger.HeureFixe).minute)
        assertTrue(macro.actions.any { it is ActionMacro.BasculerBluetooth })
        assertTrue(macro.actions.any { it is ActionMacro.ChangerVolume })
    }

    @Test
    fun testGenererMacroSecousseEtTorche() = runBlocking {
        val prompt = "Quand je secoue le téléphone, allume la lampe torche"
        val result = useCase.genererMacro(prompt)
        assertTrue(result.isSuccess)

        val macro = result.getOrNull()
        assertNotNull(macro)
        assertTrue(macro!!.trigger is Trigger.Secousse)
        assertTrue(macro.actions.any { it is ActionMacro.LampeTorche })
    }
}
