package com.example.kutira_kushala

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Fade-in animation
        val splashRoot = findViewById<android.view.View>(R.id.splash_root)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        splashRoot.startAnimation(fadeIn)

        // Firebase Session Check
        val auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            val nextActivity = if (auth.currentUser != null) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            
            val intent = Intent(this, nextActivity)
            startActivity(intent)
            finish()
        }, 2000)
    }
}
