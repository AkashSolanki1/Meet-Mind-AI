package com.aktech.meetmindai.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aktech.meetmindai.R

class MeetingDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting_detail)

        val summary = intent.getStringExtra("summary") ?: ""
        val evaluation = intent.getStringExtra("evaluation") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val original = intent.getStringExtra("original_text") ?: ""
        val keyPoints = intent.getStringArrayListExtra("key_points") ?: arrayListOf()

        findViewById<TextView>(R.id.textDateDetail).text = date
        findViewById<TextView>(R.id.textSummaryDetail).text = summary
        findViewById<TextView>(R.id.textEvaluationDetail).text = evaluation
        findViewById<TextView>(R.id.textOriginalDetail).text = original
        findViewById<TextView>(R.id.textKeyPointsDetail).text = keyPoints.joinToString("\n• ")
    }
}
