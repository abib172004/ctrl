package com.example.data.ocr

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Moteur d'OCR on-device natif (ML Kit Text Recognition) et analyse de hiérarchie UI
 * Conforme aux sections 4.6 et 9.1 du cahier des charges Ctrl.
 */
class OcrEngine {

    private val recognizer by lazy {
        try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Analyse une image Bitmap (capture d'écran) et extrait le texte structuré via ML Kit
     */
    suspend fun reconnaitreTexte(bitmap: Bitmap): ResultatOcr = withContext(Dispatchers.Default) {
        val client = recognizer
        if (client == null) {
            return@withContext ResultatOcr(
                texteComplet = "",
                lignes = emptyList(),
                nbBlocs = 0,
                erreur = "Module OCR indisponible"
            )
        }

        val deferred = CompletableDeferred<ResultatOcr>()
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            client.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val lignes = mutableListOf<String>()
                    for (bloc in visionText.textBlocks) {
                        for (ligne in bloc.lines) {
                            lignes.add(ligne.text)
                        }
                    }
                    deferred.complete(
                        ResultatOcr(
                            texteComplet = visionText.text,
                            lignes = lignes,
                            nbBlocs = visionText.textBlocks.size
                        )
                    )
                }
                .addOnFailureListener { e ->
                    deferred.complete(
                        ResultatOcr(
                            texteComplet = "",
                            lignes = emptyList(),
                            nbBlocs = 0,
                            erreur = e.message
                        )
                    )
                }
        } catch (e: Exception) {
            deferred.complete(
                ResultatOcr(
                    texteComplet = "",
                    lignes = emptyList(),
                    nbBlocs = 0,
                    erreur = e.message
                )
            )
        }

        deferred.await()
    }

    /**
     * Lecture d'écran structurée depuis l'arbre AccessibilityNodeInfo
     * Extrait récursivement tous les textes et descriptions visibles sans capture graphique
     */
    fun extraireTextesNode(racine: AccessibilityNodeInfo?): List<String> {
        if (racine == null) return emptyList()
        val resultat = mutableListOf<String>()
        val file = ArrayDeque<AccessibilityNodeInfo>()
        file.add(racine)

        while (file.isNotEmpty()) {
            val noeud = file.removeFirst()
            noeud.text?.toString()?.takeIf { it.isNotBlank() }?.let { resultat.add(it) }
            noeud.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { resultat.add(it) }

            for (i in 0 until noeud.childCount) {
                noeud.getChild(i)?.let { file.add(it) }
            }
        }
        return resultat
    }

    /**
     * Vérifie si un texte ou motif regex est présent à l'écran
     */
    fun verifierPresenceTexte(textes: List<String>, motifRegex: String): Boolean {
        val regex = try {
            Regex(motifRegex, RegexOption.IGNORE_CASE)
        } catch (_: Exception) {
            Regex(Regex.escape(motifRegex), RegexOption.IGNORE_CASE)
        }
        return textes.any { regex.containsMatchIn(it) }
    }
}

data class ResultatOcr(
    val texteComplet: String,
    val lignes: List<String>,
    val nbBlocs: Int,
    val erreur: String? = null
)
