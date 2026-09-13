package com.example.domain.usecase

import com.example.data.local.room.JsonConverters
import com.example.data.remote.api.GeminiApiClient
import com.example.domain.model.Macro
import org.json.JSONObject
import java.util.UUID

/**
 * Génération de macro par IA — appelle réellement l'API Gemini (contrairement à l'ancien
 * [GenererMacroParIAUseCase], un moteur de mots-clés local qui ne faisait aucune requête
 * réseau malgré le libellé "IA"). En cas d'échec (pas de clé API, pas de réseau, réponse
 * invalide), replie explicitement sur le moteur local — et le signale dans le résultat
 * pour rester honnête envers l'utilisateur plutôt que de faire semblant.
 */
class GenererMacroAvecGeminiUseCase(
    private val geminiClient: GeminiApiClient,
    private val fallbackUseCase: GenererMacroParIAUseCase = GenererMacroParIAUseCase()
) {

    suspend fun genererMacro(prompt: String): Result<MacroGenereeResult> {
        val texte = prompt.trim()
        if (texte.length < 3) {
            return Result.failure(IllegalArgumentException("Veuillez décrire plus précisément votre automatisation."))
        }

        val reponseIA = geminiClient.genererContenu(
            prompt = texte,
            systemInstruction = AiPromptTemplates.SYSTEM_INSTRUCTION_MACRO
        )

        reponseIA.onSuccess { brut ->
            parserReponseJson(brut, texte)?.let { macro ->
                return Result.success(MacroGenereeResult(macro, viaGeminiReel = true))
            }
        }

        // Repli transparent : pas de clé API configurée, pas de réseau, ou JSON invalide.
        val fallback = fallbackUseCase.genererMacro(texte)
        return fallback.map { macro -> MacroGenereeResult(macro, viaGeminiReel = false) }
    }

    private fun parserReponseJson(brut: String, promptOriginal: String): Macro? {
        return try {
            val nettoye = brut.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()
            val racine = JSONObject(nettoye)

            val trigger = racine.optJSONObject("trigger")?.toString()
                ?.let { JsonConverters.jsonToTrigger(it) }
                ?: return null

            val conditions = racine.optJSONArray("conditions")?.toString()
                ?.let { JsonConverters.jsonToConditions(it) }
                ?: emptyList()

            val actions = racine.optJSONArray("actions")?.toString()
                ?.let { JsonConverters.jsonToActions(it) }
                ?: emptyList()

            if (actions.isEmpty()) return null

            Macro(
                id = "ai_" + UUID.randomUUID().toString().take(8),
                nom = racine.optString("nom", "Macro IA").take(60),
                description = racine.optString("description", "Générée par Gemini d'après : \"$promptOriginal\""),
                active = true,
                trigger = trigger,
                conditions = conditions,
                actions = actions,
                dateCreation = System.currentTimeMillis(),
                estGenereeParIA = true
            )
        } catch (_: Exception) {
            null
        }
    }
}

data class MacroGenereeResult(
    val macro: Macro,
    val viaGeminiReel: Boolean
)
