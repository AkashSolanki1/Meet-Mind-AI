package com.aktech.meetmindai.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aktech.meetmindai.R
import com.aktech.meetmindai.activity.Login
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

class Settings : Fragment() {

    private lateinit var logoutBtn: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        logoutBtn = view.findViewById(R.id.logoutbtn)

        logoutBtn.setOnClickListener {
            logout()
        }

        return view
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
