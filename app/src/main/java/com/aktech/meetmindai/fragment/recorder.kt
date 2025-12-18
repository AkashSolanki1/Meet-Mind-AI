package com.aktech.meetmindai.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.aktech.meetmindai.AIProcessor
import com.aktech.meetmindai.R
import com.aktech.meetmindai.activity.ShowText
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class Recorder : Fragment() {

    private lateinit var micAnim: LottieAnimationView
    private lateinit var recordText: TextView
    private lateinit var summaryBtn: MaterialButton

    private val firebaseRef = FirebaseDatabase.getInstance().getReference("Meetings")
    private val VOICE_REQ_CODE = 500

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recorder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        micAnim = view.findViewById(R.id.micAnimation)
        recordText = view.findViewById(R.id.recordText)
        summaryBtn = view.findViewById(R.id.btnSummary)

        micAnim.enableMergePathsForKitKatAndAbove(true)
        micAnim.pauseAnimation()
        micAnim.progress = 0f

        micAnim.setOnClickListener { startGoogleVoicePopup() }

        summaryBtn.setOnClickListener {
            startActivity(Intent(requireContext(), ShowText::class.java))
        }
    }

    private fun startGoogleVoicePopup() {
        if (!hasPermission()) {
            requestPermission()
            return
        }

        micAnim.playAnimation()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }

        try {
            startActivityForResult(intent, VOICE_REQ_CODE)
        } catch (e: Exception) {
            micAnim.pauseAnimation()
            micAnim.progress = 0f
            safeToast("Google Voice Typing not available")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != VOICE_REQ_CODE) return

        micAnim.pauseAnimation()
        micAnim.progress = 0f

        if (resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = result?.get(0)?.trim() ?: ""

            recordText.text = text
            firebaseRef.child("live_transcription").setValue(text)

            analyzeAndStore(text)
        }
    }

    private fun analyzeAndStore(text: String) {
        if (text.isBlank()) {
            val fallback = generateFallbackAI(text)
            saveToFirebaseHistory(text, fallback)
            return
        }

        val aiProcessor = AIProcessor()
        aiProcessor.analyzeSpeech(text, object : AIProcessor.AIResponseCallback {
            override fun onSuccess(aiJson: JSONObject) {
                val validated = validateAIJsonOrFallback(aiJson, text)
                if (!isAdded) {
                    // fragment detached, still save to Firebase
                    saveToFirebaseHistory(text, validated)
                    return
                }
                requireActivity().runOnUiThread {
                    saveToFirebaseHistory(text, validated)
                    safeToast("Saved (AI)")
                }
            }

            override fun onFailure(error: String) {
                val fallback = generateFallbackAI(text)
                if (!isAdded) {
                    saveToFirebaseHistory(text, fallback)
                    return
                }
                requireActivity().runOnUiThread {
                    saveToFirebaseHistory(text, fallback)
                    safeToast("Saved (fallback) — AI error: $error")
                }
            }
        })
    }

    private fun validateAIJsonOrFallback(aiJson: JSONObject, originalText: String): JSONObject {
        try {
            val hasSummary = aiJson.has("summary") && aiJson.optString("summary").isNotBlank()
            val hasKeyPoints = aiJson.has("key_points") && (aiJson.opt("key_points") is JSONArray)

            if (!hasSummary || !hasKeyPoints) return generateFallbackAI(originalText)

            if (aiJson.has("dates") && aiJson.opt("dates") !is JSONObject)
                return generateFallbackAI(originalText)

            return aiJson
        } catch (_: Exception) {
            return generateFallbackAI(originalText)
        }
    }

    private fun generateFallbackAI(text: String): JSONObject {
        val summary = makeRoughSummary(text)
        val keyPoints = makeKeyPoints(text)
        val dates = makeDatesMap(text)
        val evaluation = makeEvaluation(text)
        val aiJson = JSONObject()
        aiJson.put("summary", summary)
        aiJson.put("key_points", JSONArray(keyPoints))
        aiJson.put("dates", JSONObject(dates))
        aiJson.put("evaluation", evaluation)
        return aiJson
    }

    private fun makeRoughSummary(text: String): String {
        if (text.isBlank()) return ""
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        val keep = Math.max(1, Math.ceil(sentences.size * 0.10).toInt())
        return sentences.take(keep).joinToString(" ").trim()
    }

    private fun makeKeyPoints(text: String): List<String> {
        if (text.isBlank()) return listOf("No content")
        val clauses = text.split(Regex(",| and | then |;|\\.|\\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val points = mutableListOf<String>()
        for (c in clauses) {
            if (points.size >= 6) break
            if (c.length > 3 && !points.contains(c)) points.add(c)
        }
        while (points.size < 5) {
            points.add("Additional point not detected")
        }
        return points
    }

    private fun makeDatesMap(text: String): Map<String, List<String>> {
        val datePattern = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
        val found = datePattern.findAll(text).map { it.groupValues[1] }.toList()
        return if (found.isNotEmpty()) {
            found.associateWith { List(3) { "Important item related to $it" } }
        } else {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            mapOf(today to listOf("General discussion / no explicit date detected"))
        }
    }

    private fun makeEvaluation(text: String): String {
        val lower = text.lowercase(Locale.getDefault())
        return when {
            "thank" in lower || "great" in lower || "good" in lower -> "Positive / constructive"
            "problem" in lower || "issue" in lower || "delay" in lower -> "Problem-focused / needs attention"
            "angry" in lower || "not happy" in lower || "disappointed" in lower -> "Negative / concerning"
            else -> "Neutral / informational"
        }
    }

    private fun saveToFirebaseHistory(originalText: String, aiData: JSONObject) {
        val id = firebaseRef.child("history").push().key ?: return
        val historyData = HashMap<String, Any>()

        historyData["original_text"] = originalText
        historyData["timestamp"] =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        historyData["summary"] = aiData.optString("summary", "")

        val kp = mutableListOf<String>()
        val arr = aiData.optJSONArray("key_points")
        if (arr != null) {
            for (i in 0 until arr.length()) kp.add(arr.optString(i))
        }
        if (kp.isEmpty()) kp.add("No key points detected")
        historyData["key_points"] = kp

        val datesMap = HashMap<String, Any>()
        val datesObj = aiData.optJSONObject("dates")
        if (datesObj != null) {
            val keys = datesObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val list = mutableListOf<String>()
                val arr2 = datesObj.optJSONArray(k)
                if (arr2 != null) {
                    for (i in 0 until arr2.length()) list.add(arr2.optString(i))
                } else {
                    val v = datesObj.optString(k)
                    if (v.isNotBlank()) list.add(v)
                }
                datesMap[k] = list
            }
        }
        if (datesMap.isEmpty()) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            datesMap[today] = listOf("General discussion")
        }
        historyData["dates"] = datesMap

        historyData["evaluation"] = aiData.optString("evaluation", "No evaluation")

        firebaseRef.child("history").child(id).setValue(historyData)
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            101
        )
    }

    private fun safeToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } else {
            println("Fragment not attached, toast skipped: $message")
        }
    }
}
