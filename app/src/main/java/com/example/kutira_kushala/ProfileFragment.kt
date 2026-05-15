package com.example.kutira_kushala

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {

    private lateinit var ivProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var etMobile: EditText
    private lateinit var etBusiness: EditText
    private lateinit var etLocation: EditText
    private lateinit var etSkill: EditText
    private lateinit var etAbout: EditText
    private lateinit var btnSave: Button
    private lateinit var btnEdit: TextView
    private lateinit var btnLogout: Button
    private lateinit var switchDarkMode: MaterialSwitch
    
    private var selectedImageUri: Uri? = null
    private var currentSeller: Seller? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            ivProfile.load(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.shared_image)
        sharedElementEnterTransition = transition
        sharedElementReturnTransition = transition
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        ivProfile = view.findViewById(R.id.iv_profile_large)
        etName = view.findViewById(R.id.et_profile_name)
        etMobile = view.findViewById(R.id.et_profile_mobile)
        etBusiness = view.findViewById(R.id.et_profile_business)
        etLocation = view.findViewById(R.id.et_profile_location)
        etSkill = view.findViewById(R.id.et_profile_skill)
        etAbout = view.findViewById(R.id.et_profile_about)
        btnSave = view.findViewById(R.id.btn_save_profile)
        btnEdit = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        switchDarkMode = view.findViewById(R.id.switch_dark_mode)

        loadProfileData()
        setupDarkModeSwitch()

        btnEdit.setOnClickListener { toggleEditing(true) }
        btnSave.setOnClickListener { saveProfileData() }
        btnLogout.setOnClickListener { performLogout() }
        
        view.findViewById<View>(R.id.tv_change_photo).setOnClickListener {
            if (btnSave.visibility == View.VISIBLE) pickImage.launch("image/*")
        }

        return view
    }

    private fun loadProfileData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("sellers").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val seller = doc.toObject(Seller::class.java)
                seller?.let {
                    currentSeller = it
                    etName.setText(it.sellerName)
                    etMobile.setText(it.mobileNumber)
                    etBusiness.setText(it.businessName)
                    etLocation.setText(it.location)
                    etSkill.setText(it.skills.firstOrNull() ?: "")
                    etAbout.setText(it.about)
                    ivProfile.load(it.profileImageUrl) {
                        placeholder(R.drawable.ic_person)
                        error(R.drawable.ic_person)
                    }
                }
            }
    }

    private fun setupDarkModeSwitch() {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        switchDarkMode.isChecked = isDarkMode
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
            requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE).edit { putBoolean("dark_mode", isChecked) }
        }
    }

    private fun toggleEditing(enabled: Boolean) {
        listOf(etName, etMobile, etBusiness, etLocation, etSkill, etAbout).forEach { it.isEnabled = enabled }
        btnSave.visibility = if (enabled) View.VISIBLE else View.GONE
        btnEdit.visibility = if (enabled) View.GONE else View.VISIBLE
        if (enabled) etName.requestFocus()
    }

    private fun saveProfileData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val mobile = etMobile.text.toString().trim()
        
        if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            etMobile.error = "Enter valid 10-digit number"
            return
        }

        btnSave.isEnabled = false
        Toast.makeText(context, "Updating profile...", Toast.LENGTH_SHORT).show()
        
        if (selectedImageUri != null) {
            uploadImageAndSave(uid)
        } else {
            updateFirestore(uid, currentSeller?.profileImageUrl ?: "")
        }
    }

    private fun uploadImageAndSave(uid: String) {
        val ref = FirebaseStorage.getInstance().reference.child("profiles/$uid.jpg")
        ref.putFile(selectedImageUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                ref.downloadUrl
            }
            .addOnSuccessListener { url ->
                updateFirestore(uid, url.toString())
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFirestore(uid: String, imageUrl: String) {
        val updatedSeller = hashMapOf(
            "sellerId" to uid,
            "sellerName" to etName.text.toString().trim(),
            "businessName" to etBusiness.text.toString().trim(),
            "mobileNumber" to etMobile.text.toString().trim(),
            "location" to etLocation.text.toString().trim(),
            "skills" to listOf(etSkill.text.toString().trim()),
            "about" to etAbout.text.toString().trim(),
            "profileImageUrl" to imageUrl
        )

        FirebaseFirestore.getInstance().collection("sellers").document(uid)
            .set(updatedSeller, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                btnSave.isEnabled = true
                toggleEditing(false)
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performLogout() {
        MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialog)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Logout") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                requireActivity().finish()
            }.show()
    }
}
