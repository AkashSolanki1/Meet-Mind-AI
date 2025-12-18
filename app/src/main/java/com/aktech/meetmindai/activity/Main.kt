package com.aktech.meetmindai.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aktech.meetmindai.R
import com.aktech.meetmindai.fragment.MeetingHistoryFragment
import com.aktech.meetmindai.fragment.Settings
import com.aktech.meetmindai.fragment.Recorder
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupBottomNavigationView()

        // Load default fragment (Recorder)
        frag(Recorder(), false)
    }

    private fun setupBottomNavigationView() {
        navigation = findViewById(R.id.bottomNavigation)
        navigation.setItemIconTintList(null)

        navigation.setOnItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem)
        }
    }

    private fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_home -> frag(Recorder(), false)
            R.id.nav_history -> frag(MeetingHistoryFragment(), false)
            R.id.nav_settings -> frag(Settings(), false)
        }
        return true
    }

    private fun frag(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
        if (addToBackStack) {
            transaction.add(R.id.framelayoutfl, fragment)
        } else {
            transaction.replace(R.id.framelayoutfl, fragment)
        }
        transaction.commit()
    }
}
