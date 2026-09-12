package com.example.data.remote.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client natif Gemini API utilisant le modèle 'gemini-2.5-flash'
 * Conforme au skill gemini-api et à la section 9.2 du cahier des charges Ctrl.
 */
class GeminiApiClient(private val context: Context? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private fun getEffectiveApiKey(): String {
        // 1. Vérifier si l'utilisateur a saisi sa clé directement dans l'application
        if (context != null) {
            val prefs = context.getSharedPreferences("ctrl_prefs", Context.MODE_PRIVATE)
            val customKey = prefs.getString("gemini_api_key", "") ?: ""
            if (customKey.isNotBlank()) {
                return customKey.trim()
            }
        }
        // 2. Repli sur la clé injectée via les Secrets / .env
        val buildKey = BuildConfig.GEMINI_API_KEY
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey.trim() else ""
    }

    /**
     * Exécute une requête de génération de contenu auprès de Gemini API
     */
    suspend fun genererContenu(prompt: String, systemInstruction: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getEffectiveApiKey()
            if (apiKey.isBlank()) {
                Log.w("GeminiApiClient", "Clé API Gemini non configurée")
                return@withContext Result.failure(
                    IllegalStateException("Clé API Gemini manquante. Renseignez-la dans Réglages de l'app ou dans le panneau Secrets d'AI Studio.")
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject()

            // Instructions système éventuelles
            if (!systemInstruction.isNullOrBlank()) {
                val sysParts = JSONArray().put(JSONObject().put("text", systemInstruction))
                jsonBody.put("systemInstruction", JSONObject().put("parts", sysParts))
            }

            // Contenu du prompt utilisateur
            val contentsArray = JSONArray()
            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }
            contentsArray.put(userContent)
            jsonBody.put("contents", contentsArray)

            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erreur Gemini API ${response.code}: $responseString"))
            }

            val respJson = JSONObject(responseString)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val texte = parts.getJSONObject(0).optString("text")
                    return@withContext Result.success(texte)
                }
            }

            Result.failure(Exception("Réponse vide de Gemini"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

