package com.example.kutira_kushala

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddProductActivity : AppCompatActivity() {

    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var rvPreviews: RecyclerView
    private lateinit var llPrompt: LinearLayout
    private lateinit var etCategory: EditText
    private lateinit var imageAdapter: ImagePreviewAdapter
    
    private var tempImageUri: Uri? = null
    
    private val categories = arrayOf("Crafts", "Food", "Textiles")

    // Image Pickers
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.forEach { addImage(it) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { addImage(it) }
        }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission required for photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme setting
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        rvPreviews = findViewById(R.id.rv_image_previews)
        llPrompt = findViewById(R.id.ll_upload_prompt)
        etCategory = findViewById(R.id.et_category)
        
        imageAdapter = ImagePreviewAdapter(selectedImageUris) { position ->
            selectedImageUris.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            updateImageVisibility()
        }
        rvPreviews.adapter = imageAdapter
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btn_back).setOnClickListener { handleBackNavigation() }
        findViewById<View>(R.id.cl_upload).setOnClickListener { showImageOptions() }
        etCategory.setOnClickListener { showCategoryDialog() }
        
        findViewById<Button>(R.id.btn_submit).setOnClickListener { attemptSubmit() }
        findViewById<Button>(R.id.btn_cancel).setOnClickListener { handleBackNavigation() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackNavigation() }
        })
    }

    private fun addImage(uri: Uri) {
        if (selectedImageUris.size < 5) {
            selectedImageUris.add(uri)
            imageAdapter.notifyItemInserted(selectedImageUris.size - 1)
            updateImageVisibility()
        } else {
            Toast.makeText(this, "Maximum 5 images allowed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImageVisibility() {
        val hasImages = selectedImageUris.isNotEmpty()
        rvPreviews.visibility = if (hasImages) View.VISIBLE else View.GONE
        llPrompt.visibility = if (hasImages) View.GONE else View.VISIBLE
    }

    private fun showImageOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Upload Image")
            .setItems(options) { _, which ->
                if (which == 0) checkCameraPermission() else pickImages.launch("image/*")
            }.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Product Photo")
            put(MediaStore.Images.Media.DESCRIPTION, "From Kutira Kushala Camera")
        }
        tempImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        tempImageUri?.let { takePhoto.launch(it) }
    }

    private fun showCategoryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Category")
            .setItems(categories) { _, which -> etCategory.setText(categories[which]) }
            .show()
    }

    private fun attemptSubmit() {
        val name = findViewById<EditText>(R.id.et_product_name).text.toString().trim()
        val category = etCategory.text.toString()
        val retailPrice = findViewById<EditText>(R.id.et_retail_price).text.toString().trim()
        val wholesalePrice = findViewById<EditText>(R.id.et_wholesale_price).text.toString().trim()
        val description = findViewById<EditText>(R.id.et_description).text.toString().trim()

        if (validateInputs(name, category, retailPrice, wholesalePrice)) {
            uploadProcess(name, category, retailPrice, wholesalePrice, description)
        }
    }

    private fun validateInputs(name: String, cat: String, retail: String, wholesale: String): Boolean {
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Upload at least one image", Toast.LENGTH_SHORT).show()
            return false
        }
        if (name.isEmpty() || cat.isEmpty() || retail.isEmpty() || wholesale.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun uploadProcess(name: String, cat: String, retail: String, wholesale: String, desc: String) {
        val btn = findViewById<Button>(R.id.btn_submit)
        btn.isEnabled = false
        Toast.makeText(this, "Uploading images...", Toast.LENGTH_LONG).show()

        val storage = FirebaseStorage.getInstance()
        val urls = mutableListOf<String>()
        val uploadTasks = selectedImageUris.map { uri ->
            val ref = storage.reference.child("products/${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                ref.downloadUrl
            }
        }

        Tasks.whenAllComplete(uploadTasks).addOnCompleteListener { 
            uploadTasks.forEach { task ->
                if (task.isSuccessful) urls.add(task.result.toString())
            }

            if (urls.isNotEmpty()) {
                saveProductToFirestore(name, cat, retail, wholesale, desc, urls)
            } else {
                btn.isEnabled = true
                Toast.makeText(this, "Image upload failed. Check connection.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveProductToFirestore(name: String, cat: String, ret: String, who: String, desc: String, urls: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val productId = db.collection("products").document().id
        
        val product = ProductItem(
            name = name,
            category = cat,
            price = "₹$ret",
            wholesalePrice = "₹$who",
            description = desc,
            imageUrls = urls,
            productId = productId,
            sellerId = auth.currentUser?.uid ?: "unknown",
            createdAt = Timestamp.now()
        )

        db.collection("products").document(productId)
            .set(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Product listed successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                findViewById<Button>(R.id.btn_submit).isEnabled = true
                Toast.makeText(this, "Error saving to database", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleBackNavigation() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
