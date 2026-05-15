package com.example.kutira_kushala

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SellerProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var tvBusinessName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvExperience: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvProductsCount: TextView
    private lateinit var tvAbout: TextView
    private lateinit var cgSkills: ChipGroup
    private lateinit var rvProducts: RecyclerView
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_profile)

        val sellerId = intent.getStringExtra("SELLER_ID") ?: ""
        if (sellerId.isEmpty()) {
            finish()
            return
        }

        initViews(sellerId)
        loadSellerData(sellerId)
        loadSellerProducts(sellerId)
    }

    private fun initViews(sellerId: String) {
        ivProfile = findViewById(R.id.iv_seller_profile)
        tvBusinessName = findViewById(R.id.tv_seller_business_name)
        tvLocation = findViewById(R.id.tv_seller_location)
        tvExperience = findViewById(R.id.tv_experience)
        tvRating = findViewById(R.id.tv_rating)
        tvProductsCount = findViewById(R.id.tv_products_count)
        tvAbout = findViewById(R.id.tv_seller_about)
        cgSkills = findViewById(R.id.cg_skills)
        rvProducts = findViewById(R.id.rv_seller_products)

        findViewById<View>(R.id.btn_back_seller).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_contact_seller).setOnClickListener {
            val sellerId = intent.getStringExtra("SELLER_ID")
            FirebaseFirestore.getInstance().collection("sellers").document(sellerId!!).get()
                .addOnSuccessListener { doc ->
                    val mobile = doc.getString("mobileNumber") ?: "9876543210"
                    val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$mobile"))
                    startActivity(intent)
                }
        }

        findViewById<View>(R.id.btn_message_seller).setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("RECIPIENT_ID", sellerId)
            startActivity(intent)
        }

        adapter = ProductAdapter { product ->
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("PRODUCT_ID", product.productId)
            startActivity(intent)
        }
        rvProducts.layoutManager = GridLayoutManager(this, 2)
        rvProducts.adapter = adapter
        rvProducts.isNestedScrollingEnabled = false // Optimization for NestedScrollView
    }

    private fun loadSellerData(sellerId: String) {
        FirebaseFirestore.getInstance().collection("sellers").document(sellerId)
            .get()
            .addOnSuccessListener { doc ->
                val seller = doc.toObject(Seller::class.java)
                seller?.let { populateUI(it) }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateUI(seller: Seller) {
        tvBusinessName.text = seller.businessName
        tvLocation.text = seller.location
        tvExperience.text = "${seller.experienceYears} yrs"
        tvRating.text = "${seller.rating} ★"
        tvProductsCount.text = seller.productsCount.toString()
        tvAbout.text = seller.about.ifEmpty { "Rural artisan micro-factory dedicated to authentic craftsmanship and community growth." }
        
        ivProfile.load(seller.profileImageUrl) {
            placeholder(R.drawable.ic_person)
            error(R.drawable.ic_person)
            transformations(CircleCropTransformation())
        }

        cgSkills.removeAllViews()
        seller.skills.forEach { skill ->
            val chip = Chip(this)
            chip.text = skill
            chip.chipBackgroundColor = getColorStateList(R.color.primary_green)
            chip.setTextColor(android.graphics.Color.WHITE)
            cgSkills.addView(chip)
        }
    }

    private fun loadSellerProducts(sellerId: String) {
        FirebaseFirestore.getInstance().collection("products")
            .whereEqualTo("sellerId", sellerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val products = it.toObjects(ProductItem::class.java)
                    adapter.submitList(products)
                    tvProductsCount.text = products.size.toString()
                    
                    // Update seller product count in firestore if needed
                    FirebaseFirestore.getInstance().collection("sellers").document(sellerId)
                        .update("productsCount", products.size)
                }
            }
    }
}
