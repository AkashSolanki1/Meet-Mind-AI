package com.aktech.meetmindai.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aktech.meetmindai.MeetingAdapter
import com.aktech.meetmindai.R
import com.aktech.meetmindai.activity.MeetingDetailActivity
import com.google.firebase.database.*

data class MeetingItem(
    val id: String = "",
    val date: String = "",
    val summary: String = "",
    val evaluation: String = "",
    val key_points: List<String> = listOf(),
    val original_text: String = ""
)

class MeetingHistoryFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private val meetingList = mutableListOf<MeetingItem>()
    private lateinit var adapter: MeetingAdapter
    private val dbRef = FirebaseDatabase.getInstance().getReference("Meetings").child("history")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.recyclerMeetings)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = MeetingAdapter(meetingList) { meeting ->
            val intent = Intent(requireContext(), MeetingDetailActivity::class.java).apply {
                putExtra("summary", meeting.summary)
                putExtra("evaluation", meeting.evaluation)
                putExtra("date", meeting.date)
                putExtra("original_text", meeting.original_text)
                putStringArrayListExtra("key_points", ArrayList(meeting.key_points))
            }
            startActivity(intent)
        }
        recycler.adapter = adapter

        fetchMeetings()
    }

    override fun onResume() {
        super.onResume()
        fetchMeetings() // refresh if new entry added
    }

    private fun fetchMeetings() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                meetingList.clear()
                for (snap in snapshot.children) {
                    val map = snap.value as? Map<*, *> ?: continue

                    // Try to extract date
                    var extractedDate = map["timestamp"]?.toString() ?: ""
                    val datesMap = map["dates"] as? Map<*, *>
                    if (!datesMap.isNullOrEmpty()) {
                        extractedDate = datesMap.keys.first().toString()
                    }

                    val meeting = MeetingItem(
                        id = snap.key ?: "",
                        date = extractedDate,
                        summary = map["summary"]?.toString() ?: "",
                        evaluation = map["evaluation"]?.toString() ?: "",
                        original_text = map["original_text"]?.toString() ?: "",
                        key_points = (map["key_points"] as? List<*>)?.map { it.toString() } ?: listOf()
                    )
                    meetingList.add(meeting)
                }
                meetingList.reverse()
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // optionally log error
            }
        })
    }
}
