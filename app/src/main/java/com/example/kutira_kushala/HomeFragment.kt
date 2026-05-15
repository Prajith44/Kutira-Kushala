package com.example.kutira_kushala

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var productAdapter: ProductAdapter
    private lateinit var orderAdapter: ActiveOrderAdapter
    private lateinit var tvBusinessName: TextView
    private lateinit var capacityProgress: CircularProgressIndicator
    private lateinit var tvCapacityPercentage: TextView
    private lateinit var tvAvailableCapacity: TextView
    
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var mainContent: View
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        shimmerContainer = view.findViewById(R.id.shimmer_view_container)
        mainContent = view.findViewById(R.id.main_content)

        tvBusinessName = view.findViewById(R.id.tv_business_name)
        capacityProgress = view.findViewById(R.id.capacity_progress)
        tvCapacityPercentage = view.findViewById(R.id.tv_capacity_percentage)
        tvAvailableCapacity = view.findViewById(R.id.tv_available_capacity)

        val ivProfile = view.findViewById<View>(R.id.iv_profile)
        ivProfile.setOnClickListener {
            val mainActivity = activity as? MainActivity
            mainActivity?.loadFragmentWithSharedElement(ProfileFragment(), it, "profile_image", R.id.nav_profile)
        }

        val rvOrders = view.findViewById<RecyclerView>(R.id.rv_active_orders)
        rvOrders.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        val rvProducts = view.findViewById<RecyclerView>(R.id.rv_products)
        rvProducts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        productAdapter = ProductAdapter { product ->
            val intent = Intent(context, ProductDetailActivity::class.java)
            if (product.productId.isNotEmpty()) {
                intent.putExtra("PRODUCT_ID", product.productId)
            } else {
                intent.putExtra("PRODUCT_NAME", product.name)
            }
            startActivity(intent)
        }
        rvProducts.adapter = productAdapter

        simulateLoading()

        return view
    }

    private fun simulateLoading() {
        shimmerContainer.startShimmer()
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                shimmerContainer.stopShimmer()
                shimmerContainer.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
                
                updateProfileData()
                updateOrdersAndCapacity()
                updateProducts()
            }
        }, 1500)
    }

    override fun onResume() {
        super.onResume()
        if (mainContent.visibility == View.VISIBLE) {
            updateProfileData()
            updateOrdersAndCapacity()
            updateProducts()
        }
    }

    private fun updateProfileData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("sellers").document(uid).get().addOnSuccessListener { doc ->
            tvBusinessName.text = doc.getString("businessName") ?: "Artisan shop"
            view?.findViewById<TextView>(R.id.tv_business_location)?.text = doc.getString("location") ?: "Rural India"
            val skills = doc.get("skills") as? List<*>
            view?.findViewById<TextView>(R.id.tv_business_info)?.text = "${skills?.firstOrNull() ?: "Craftsman"} | 5 Artisans"
        }
    }

    private fun updateOrdersAndCapacity() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("notifications")
            .whereEqualTo("receiverId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val notifications = snapshot.toObjects(Notification::class.java)
                    val activeOrders = notifications.filter { 
                        it.status != OrderStatus.REQUESTED && 
                        it.status != OrderStatus.DELIVERED && 
                        it.status != OrderStatus.COMPLETED 
                    }
                    
                    orderAdapter = ActiveOrderAdapter(activeOrders) { order ->
                        val intent = Intent(context, OrderTrackingActivity::class.java)
                        intent.putExtra("NOTIFICATION_ID", order.id)
                        startActivity(intent)
                    }
                    view?.findViewById<RecyclerView>(R.id.rv_active_orders)?.adapter = orderAdapter

                    calculateAndAnimateCapacity(notifications)
                }
            }
    }

    private fun calculateAndAnimateCapacity(allNotifications: List<Notification>) {
        val totalCapacity = 200
        var usedCapacity = 0
        
        allNotifications.forEach { order ->
            if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.COMPLETED) {
                usedCapacity += order.quantity.toIntOrNull() ?: 0
            }
        }

        val percentage = (usedCapacity.toFloat() / totalCapacity.toFloat() * 100).toInt().coerceIn(0, 100)
        val available = (totalCapacity - usedCapacity).coerceAtLeast(0)

        val animator = ValueAnimator.ofInt(0, percentage)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            capacityProgress.progress = value
            tvCapacityPercentage.text = "$value%"
            
            val color = when {
                value < 50 -> "#1F5D57"
                value < 80 -> "#F59E0B"
                else -> "#D32F2F"
            }
            capacityProgress.setIndicatorColor(Color.parseColor(color))
        }
        animator.start()

        tvAvailableCapacity.text = "$available units available"
    }

    private fun updateProducts() {
        db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val products = snapshot.toObjects(ProductItem::class.java)
                productAdapter.submitList(products)
            }
    }
}
