package com.example.kutira_kushala

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etFullName: EditText
    private lateinit var etMobile: EditText
    private lateinit var etPasscode: EditText
    private lateinit var etConfirmPasscode: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etFullName = findViewById(R.id.et_full_name)
        etMobile = findViewById(R.id.et_mobile)
        etPasscode = findViewById(R.id.et_passcode)
        etConfirmPasscode = findViewById(R.id.et_confirm_passcode)
        btnRegister = findViewById(R.id.btn_register)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateFields()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etFullName.addTextChangedListener(watcher)
        etMobile.addTextChangedListener(watcher)
        etPasscode.addTextChangedListener(watcher)
        etConfirmPasscode.addTextChangedListener(watcher)

        btnRegister.setOnClickListener {
            if (performFinalValidation()) {
                registerUser()
            }
        }

        tvLogin.setOnClickListener { handleBackNavigation() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackNavigation() }
        })
    }

    private fun validateFields() {
        val name = etFullName.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val passcode = etPasscode.text.toString()
        val confirmPasscode = etConfirmPasscode.text.toString()

        btnRegister.isEnabled = name.isNotEmpty() && 
                                mobile.length == 10 && 
                                passcode.length >= 6 && 
                                confirmPasscode == passcode
    }

    private fun performFinalValidation(): Boolean {
        val mobile = etMobile.text.toString().trim()
        val passcode = etPasscode.text.toString()
        val confirmPasscode = etConfirmPasscode.text.toString()

        var isValid = true

        if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            etMobile.error = "Invalid mobile number"
            isValid = false
        }

        if (passcode.length < 6) {
            etPasscode.error = "Passcode must be at least 6 characters"
            isValid = false
        }

        if (passcode != confirmPasscode) {
            etConfirmPasscode.error = "Passcodes do not match"
            isValid = false
        }

        return isValid
    }

    private fun registerUser() {
        val name = etFullName.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val passcode = etPasscode.text.toString()
        
        val tempEmail = "$mobile@kutira.com"

        btnRegister.isEnabled = false
        
        auth.createUserWithEmailAndPassword(tempEmail, passcode)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        saveUserAndSeller(uid, name, mobile)
                    }
                } else {
                    btnRegister.isEnabled = true
                    val message = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Mobile number already registered"
                        is FirebaseAuthWeakPasswordException -> "Passcode is too weak"
                        else -> task.exception?.localizedMessage ?: "Registration failed"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserAndSeller(uid: String, name: String, mobile: String) {
        val batch = firestore.batch()
        
        val userRef = firestore.collection("users").document(uid)
        val userMap = hashMapOf(
            "uid" to uid,
            "fullName" to name,
            "mobileNumber" to mobile,
            "createdAt" to FieldValue.serverTimestamp()
        )
        batch.set(userRef, userMap)

        val sellerRef = firestore.collection("sellers").document(uid)
        val sellerMap = hashMapOf(
            "sellerId" to uid,
            "sellerName" to name,
            "mobileNumber" to mobile,
            "businessName" to "$name's Micro-Factory",
            "location" to "Pending",
            "experienceYears" to 0,
            "rating" to 5.0f,
            "productsCount" to 0,
            "createdAt" to FieldValue.serverTimestamp()
        )
        batch.set(sellerRef, sellerMap)

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Welcome to Kutira Kushala!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                btnRegister.isEnabled = true
                Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleBackNavigation() {
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
