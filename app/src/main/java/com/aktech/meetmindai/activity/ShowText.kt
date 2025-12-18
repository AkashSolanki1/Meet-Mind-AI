package com.aktech.meetmindai.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aktech.meetmindai.R
import com.google.firebase.database.*

class ShowText : AppCompatActivity() {

    private lateinit var textDateDetail: TextView
    private lateinit var textSummaryDetail: TextView
    private lateinit var textEvaluationDetail: TextView
    private lateinit var textKeyPointsDetail: TextView
    private lateinit var textOriginalDetail: TextView

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_text)

        textDateDetail = findViewById(R.id.textDateDetail)
        textSummaryDetail = findViewById(R.id.textSummaryDetail)
        textEvaluationDetail = findViewById(R.id.textEvaluationDetail)
        textKeyPointsDetail = findViewById(R.id.textKeyPointsDetail)
        textOriginalDetail = findViewById(R.id.textOriginalDetail)

        dbRef = FirebaseDatabase.getInstance().getReference("Meetings/history")

        fetchLatestMeeting()
    }

    private fun fetchLatestMeeting() {
        dbRef.limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    showNoData()
                    return
                }

                val latest = snapshot.children.firstOrNull()
                if (latest == null) {
                    showNoData()
                    return
                }

                val timestamp = latest.child("timestamp").getValue(String::class.java) ?: "N/A"
                val summary = latest.child("summary").getValue(String::class.java) ?: "N/A"
                val evaluation = latest.child("evaluation").getValue(String::class.java) ?: "N/A"
                val originalText = latest.child("original_text").getValue(String::class.java) ?: "N/A"

                val keyPointsList = mutableListOf<String>()
                val kpSnapshot = latest.child("key_points")
                for (kp in kpSnapshot.children) {
                    kp.getValue(String::class.java)?.let { keyPointsList.add("• $it") }
                }

                val keyPoints = if (keyPointsList.isEmpty()) "No key points available"
                else keyPointsList.joinToString("\n")

                // ✅ Update UI
                textDateDetail.text = "📅 Date: $timestamp"
                textSummaryDetail.text = summary
                textEvaluationDetail.text = evaluation
                textKeyPointsDetail.text = keyPoints
                textOriginalDetail.text = originalText
            }

            override fun onCancelled(error: DatabaseError) {
                showError(error.message)
            }
        })
    }

    private fun showNoData() {
        textDateDetail.text = "No meeting data found"
        textSummaryDetail.text = "-"
        textEvaluationDetail.text = "-"
        textKeyPointsDetail.text = "-"
        textOriginalDetail.text = "-"
    }

    private fun showError(message: String) {
        textDateDetail.text = "Error loading data: $message"
        textSummaryDetail.text = "-"
        textEvaluationDetail.text = "-"
        textKeyPointsDetail.text = "-"
        textOriginalDetail.text = "-"
    }
}
