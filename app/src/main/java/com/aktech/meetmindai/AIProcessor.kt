package com.aktech.meetmindai

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.math.min

class AIProcessor {

    private val client = OkHttpClient()

    private val API_KEY = "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"
    private val API_URL = "https://openrouter.ai/api/v1/chat/completions"

    interface AIResponseCallback {
        fun onSuccess(aiJson: JSONObject)
        fun onFailure(error: String)
    }

    fun analyzeSpeech(text: String, callback: AIResponseCallback) {
        if (text.isBlank()) {
            callback.onFailure("Text cannot be empty.")
            return
        }

        val wordCount = text.trim().split("\\s+".toRegex()).size
        val compressionHint = when {
            wordCount < 40 -> "Summarize in 1-2 short sentences."
            wordCount < 120 -> "Summarize in 2-3 concise sentences."
            else -> "Summarize in about 10% of the original length."
        }

        val prompt = """
            You are an expert meeting summarizer and insight generator.
            Text to analyze:
            "$text"

            Instructions:
            - $compressionHint
            - Paraphrase, don't copy.
            - Identify all key events, decisions, and important dates.
            - Create 5-6 concise actionable key points.
            - Detect dates (YYYY-MM-DD) and associate up to 10 related events.
            - Add one short 'evaluation' describing the tone or importance.
            - Return STRICTLY valid JSON. No markdown, no explanations.
            
            Format:
            {
              "summary": "...",
              "key_points": ["...", "..."],
              "dates": {"YYYY-MM-DD": ["...", "..."]},
              "evaluation": "..."
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("model", "mistralai/mistral-7b-instruct")
            put("temperature", 0.4)
            put("max_tokens", 400)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You output valid JSON only — no explanations.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/aktech/meetmindai") // or your site/app name
            .addHeader("X-Title", "MeetMindAI")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Network Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string() ?: ""

                    if (!it.isSuccessful) {
                        Log.e("AIProcessor", "API failed with code ${it.code}: $responseBody")
                        callback.onFailure("API Error ${it.code}: $responseBody")
                        return
                    }

                    try {
                        val obj = JSONObject(responseBody)
                        val content = obj
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        val jsonStart = content.indexOf('{')
                        val jsonEnd = content.lastIndexOf('}')
                        if (jsonStart == -1 || jsonEnd == -1) {
                            callback.onFailure("Response not valid JSON.")
                            return
                        }

                        val jsonPart = content.substring(jsonStart, jsonEnd + 1)
                        val aiJson = JSONObject(jsonPart)

                        val original = text.trim()
                        var summary = aiJson.optString("summary", "").trim()
                        if (summary.equals(original, ignoreCase = true) ||
                            summary.length > original.length * 0.8
                        ) {
                            summary = generateLocalSummary(original)
                            aiJson.put("summary", summary)
                        }

                        val keyPoints = aiJson.optJSONArray("key_points") ?: JSONArray()
                        if (keyPoints.length() < 3) {
                            val fallbackPoints = extractKeyPointsLocally(original)
                            aiJson.put("key_points", JSONArray(fallbackPoints))
                        }

                        callback.onSuccess(aiJson)

                    } catch (e: Exception) {
                        Log.e("AIProcessor", "Parse error: ${e.message}")
                        callback.onFailure("Parsing failed: ${e.message}")
                    }
                }
            }
        })
    }

    private fun generateLocalSummary(text: String): String {
        val sentences = text.split('.', '!', '?').map { it.trim() }.filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return text
        val take = min((sentences.size * 0.3).toInt(), 3)
        return sentences.take(take).joinToString(". ") + "."
    }

    private fun extractKeyPointsLocally(text: String): List<String> {
        val sentences = text.split('.', '!', '?').map { it.trim() }.filter { it.isNotEmpty() }
        val keywords = listOf("meeting", "event", "project", "update", "announcement", "CEO", "schedule", "plan", "director")
        val scored = sentences.map { s ->
            val score = keywords.count { s.contains(it, ignoreCase = true) }
            s to score
        }.sortedByDescending { it.second }

        return scored.take(5).map { it.first.ifEmpty { "Additional point not detected" } }
    }
}
