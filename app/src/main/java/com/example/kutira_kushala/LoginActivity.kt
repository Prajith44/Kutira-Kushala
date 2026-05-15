package com.example.kutira_kushala

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme preference before super.onCreate
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        
        // Firebase Initialization
        auth = FirebaseAuth.getInstance()
        
        // Session Management: Skip login if already logged in
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etMobile = findViewById<EditText>(R.id.et_mobile)
        val etPasscode = findViewById<EditText>(R.id.et_passcode)
        val btnAuthenticate = findViewById<Button>(R.id.btn_authenticate)
        val tvRegister = findViewById<TextView>(R.id.tv_register)

        btnAuthenticate.setOnClickListener {
            val mobile = etMobile.text.toString().trim()
            val passcode = etPasscode.text.toString().trim()

            if (validate(mobile, passcode)) {
                loginUser(mobile, passcode)
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private fun validate(mobile: String, passcode: String): Boolean {
        if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            findViewById<EditText>(R.id.et_mobile).error = "Invalid mobile number"
            return false
        }
        if (passcode.isEmpty()) {
            findViewById<EditText>(R.id.et_passcode).error = "Passcode required"
            return false
        }
        return true
    }

    private fun loginUser(mobile: String, passcode: String) {
        val tempEmail = "$mobile@kutira.com"
        val btnAuthenticate = findViewById<Button>(R.id.btn_authenticate)
        
        btnAuthenticate.isEnabled = false
        
        auth.signInWithEmailAndPassword(tempEmail, passcode)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                } else {
                    btnAuthenticate.isEnabled = true
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthInvalidUserException -> "Account not found"
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect passcode"
                        else -> exception?.localizedMessage ?: "Authentication failed"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }
}
