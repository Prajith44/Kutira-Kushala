package com.example.kutira_kushala

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var ivMain: ImageView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var mainContent: View
    private var currentProduct: ProductItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        shimmerContainer = findViewById(R.id.shimmer_view_container)
        mainContent = findViewById(R.id.main_content)
        ivMain = findViewById(R.id.iv_product_detail)

        val productId = intent.getStringExtra("PRODUCT_ID")
        val productName = intent.getStringExtra("PRODUCT_NAME")

        if (productId != null) {
            loadProductFromFirestore(productId)
        } else if (productName != null) {
            val product = ProductRepository.getAllProducts().find { it.name == productName }
            if (product != null) {
                currentProduct = product
                setupUI(product)
                setupGallery(product)
                loadSellerInfo(product.sellerId)
                simulateLoading()
            } else finish()
        } else finish()

        findViewById<View>(R.id.btn_back_detail).setOnClickListener { finish() }

        findViewById<Button>(R.id.btn_call).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:9876543210"))
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_whatsapp).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=919876543210"))
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_bulk_request).setOnClickListener {
            showBulkOrderDialog()
        }
    }

    private fun loadProductFromFirestore(productId: String) {
        shimmerContainer.startShimmer()
        FirebaseFirestore.getInstance().collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(ProductItem::class.java)?.let { product ->
                    currentProduct = product
                    setupUI(product)
                    setupGallery(product)
                    loadSellerInfo(product.sellerId)
                    simulateLoading()
                } ?: finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load product details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadSellerInfo(sellerId: String) {
        if (sellerId.isEmpty()) return
        
        FirebaseFirestore.getInstance().collection("sellers").document(sellerId)
            .get()
            .addOnSuccessListener { doc ->
                val seller = doc.toObject(Seller::class.java)
                seller?.let { s ->
                    findViewById<TextView>(R.id.tv_detail_seller_name).text = s.businessName
                    findViewById<TextView>(R.id.tv_detail_seller_loc).text = s.location
                    findViewById<TextView>(R.id.tv_detail_seller_rating).text = "${s.rating} ★"
                    findViewById<TextView>(R.id.tv_detail_seller_products).text = "${s.productsCount} Products"
                    findViewById<TextView>(R.id.tv_detail_seller_about_preview).text = s.about.ifEmpty { "Rural artisan dedicated to quality craftsmanship." }
                    
                    findViewById<ImageView>(R.id.iv_detail_seller).load(s.profileImageUrl) {
                        placeholder(R.drawable.ic_person)
                        transformations(CircleCropTransformation())
                    }
                    
                    val openProfile = { openSellerProfile(s.sellerId) }
                    findViewById<View>(R.id.cv_seller_card).setOnClickListener { openProfile() }
                    findViewById<View>(R.id.btn_view_seller_profile).setOnClickListener { openProfile() }
                    
                    findViewById<View>(R.id.btn_chat_seller).setOnClickListener {
                        val intent = Intent(this@ProductDetailActivity, ChatActivity::class.java)
                        intent.putExtra("RECIPIENT_ID", s.sellerId)
                        intent.putExtra("PRODUCT_ID", currentProduct?.productId)
                        startActivity(intent)
                    }
                }
            }
    }

    private fun openSellerProfile(sellerId: String) {
        val intent = Intent(this, SellerProfileActivity::class.java)
        intent.putExtra("SELLER_ID", sellerId)
        startActivity(intent)
    }

    private fun simulateLoading() {
        Handler(Looper.getMainLooper()).postDelayed({
            shimmerContainer.stopShimmer()
            shimmerContainer.visibility = View.GONE
            mainContent.visibility = View.VISIBLE
            mainContent.alpha = 0f
            mainContent.animate().alpha(1f).setDuration(500).start()
        }, 600)
    }

    private fun setupUI(product: ProductItem) {
        findViewById<TextView>(R.id.tv_detail_name).text = product.name
        findViewById<TextView>(R.id.tv_detail_category).text = product.category
        findViewById<TextView>(R.id.tv_detail_retail).text = product.price
        
        val retailVal = product.price.replace("[^\\d]".toRegex(), "").toDoubleOrNull() ?: 0.0
        val calculatedWholesale = (retailVal * 0.75).toInt()
        findViewById<TextView>(R.id.tv_detail_wholesale).text = if (product.wholesalePrice.isNotEmpty()) product.wholesalePrice else "₹$calculatedWholesale"
        
        findViewById<TextView>(R.id.tv_detail_desc).text = product.description.ifEmpty { "Premium handmade ${product.name} from our village micro-factory." }
        
        val firstImg = (product.imageUrls + product.imageUris + product.additionalImages).firstOrNull() ?: R.drawable.ic_shield
        ivMain.load(firstImg) {
            crossfade(true)
            placeholder(R.drawable.ic_shield)
        }
    }

    private fun setupGallery(product: ProductItem) {
        val rvThumbnails = findViewById<RecyclerView>(R.id.rv_thumbnails)
        rvThumbnails.adapter = DetailThumbnailAdapter(product) { source ->
            ivMain.animate().alpha(0.8f).scaleX(0.98f).scaleY(0.98f).setDuration(100).withEndAction {
                ivMain.load(source) { crossfade(true); placeholder(R.drawable.ic_shield) }
                ivMain.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
        }
    }

    private fun showBulkOrderDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bulk_order, null)
        val dialog = AlertDialog.Builder(this, R.style.RoundedDialog).setView(dialogView).create()

        dialogView.findViewById<Button>(R.id.btn_send_request).setOnClickListener {
            val name = dialogView.findViewById<EditText>(R.id.et_bulk_buyer_name).text.toString().trim()
            val mobile = dialogView.findViewById<EditText>(R.id.et_bulk_buyer_mobile).text.toString().trim()
            val qty = dialogView.findViewById<EditText>(R.id.et_bulk_quantity).text.toString().trim()
            val deadline = dialogView.findViewById<EditText>(R.id.et_bulk_deadline).text.toString().trim()
            val address = dialogView.findViewById<EditText>(R.id.et_bulk_address).text.toString().trim()
            val message = dialogView.findViewById<EditText>(R.id.et_bulk_message).text.toString().trim()
            
            if (name.isNotEmpty() && mobile.length == 10 && qty.isNotEmpty()) {
                sendBulkOrderToFirestore(name, mobile, qty, deadline, address, message, dialog)
            } else {
                Toast.makeText(this, "Please fill required fields (Name, 10-digit Mobile, Quantity)", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.btn_cancel_request).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun sendBulkOrderToFirestore(name: String, mobile: String, qty: String, deadline: String, address: String, msg: String, dialog: AlertDialog) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val sellerId = currentProduct?.sellerId ?: ""
        val prodId = currentProduct?.productId ?: ""
        val prodName = currentProduct?.name ?: ""
        val prodImg = (currentProduct?.imageUrls?.firstOrNull() ?: "").toString()

        if (sellerId.isEmpty()) {
            Toast.makeText(this, "Seller information missing", Toast.LENGTH_SHORT).show()
            return
        }

        val orderId = db.collection("bulk_orders").document().id
        val timestamp = Timestamp.now()

        val notification = Notification(
            id = orderId,
            senderId = auth.currentUser?.uid ?: "anonymous",
            senderName = name,
            senderMobile = mobile,
            receiverId = sellerId,
            productId = prodId,
            productName = prodName,
            productImage = prodImg,
            buyerName = name,
            buyerLocation = address,
            quantity = qty,
            message = msg,
            deadline = deadline,
            timestamp = timestamp,
            status = OrderStatus.REQUESTED
        )

        // Save order and notification
        val batch = db.batch()
        batch.set(db.collection("bulk_orders").document(orderId), notification)
        batch.set(db.collection("notifications").document(orderId), notification)

        batch.commit().addOnSuccessListener {
            dialog.dismiss()
            Snackbar.make(findViewById(android.R.id.content), "Bulk order request sent successfully", Snackbar.LENGTH_LONG)
                .setBackgroundTint(resources.getColor(R.color.primary_green, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to send request: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
