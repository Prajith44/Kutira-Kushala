package com.example.kutira_kushala

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class OrderTrackingActivity : AppCompatActivity() {

    private lateinit var currentNotification: Notification
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_tracking)

        val notificationId = intent.getStringExtra("NOTIFICATION_ID") ?: ""
        if (notificationId.isNotEmpty()) {
            loadOrderData(notificationId)
        } else {
            finish()
        }

        findViewById<View>(R.id.btn_back_tracking).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_next_status).setOnClickListener {
            moveToNextStatus()
        }
    }

    private fun loadOrderData(id: String) {
        db.collection("notifications").document(id).get()
            .addOnSuccessListener { doc ->
                val notification = doc.toObject(Notification::class.java)
                if (notification != null) {
                    currentNotification = notification
                    updateUI()
                } else {
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load order data", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateUI() {
        findViewById<TextView>(R.id.tv_track_buyer).text = currentNotification.senderName
        findViewById<TextView>(R.id.tv_track_product).text = "${currentNotification.quantity} x ${currentNotification.productName}"
        
        val statusBadge = findViewById<TextView>(R.id.tv_current_status_badge)
        statusBadge.text = currentNotification.status.displayName.uppercase()
        updateBadgeColor(statusBadge, currentNotification.status)

        updateTimeline()
        updateActionButton()
    }

    private fun updateBadgeColor(view: TextView, status: OrderStatus) {
        val color = when (status) {
            OrderStatus.REQUESTED -> "#9CA3AF"
            OrderStatus.ACCEPTED -> "#3B82F6"
            OrderStatus.PROCESSING -> "#F59E0B"
            OrderStatus.READY -> "#10B981"
            OrderStatus.DELIVERED, OrderStatus.COMPLETED -> "#1F5D57"
        }
        view.setTextColor(Color.parseColor(color))
    }

    private fun updateTimeline() {
        val steps = listOf(
            Triple(R.id.step_requested, OrderStatus.REQUESTED, "Order placed by buyer"),
            Triple(R.id.step_accepted, OrderStatus.ACCEPTED, "Seller accepted the order"),
            Triple(R.id.step_processing, OrderStatus.PROCESSING, "Product is being manufactured"),
            Triple(R.id.step_ready, OrderStatus.READY, "Product is ready for pickup"),
            Triple(R.id.step_delivered, OrderStatus.DELIVERED, "Product delivered successfully")
        )

        steps.forEachIndexed { index, step ->
            val stepView = findViewById<View>(step.first)
            val stepStatus = step.second
            val isCompleted = currentNotification.status.ordinal >= stepStatus.ordinal
            val isCurrent = currentNotification.status == stepStatus

            val dot = stepView.findViewById<ImageView>(R.id.iv_status_dot)
            val title = stepView.findViewById<TextView>(R.id.tv_status_title)
            val desc = stepView.findViewById<TextView>(R.id.tv_status_desc)
            val lineTop = stepView.findViewById<View>(R.id.view_line_top)
            val lineBottom = stepView.findViewById<View>(R.id.view_line_bottom)

            title.text = stepStatus.displayName
            desc.text = step.third
            
            lineTop.visibility = if (index == 0) View.INVISIBLE else View.VISIBLE
            lineBottom.visibility = if (index == steps.size - 1) View.INVISIBLE else View.VISIBLE

            if (isCompleted) {
                dot.setImageResource(android.R.drawable.checkbox_on_background)
                dot.setColorFilter(Color.parseColor("#1F5D57"))
                title.setTextColor(if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) Color.WHITE else Color.parseColor("#1A1A1A"))
                desc.setTextColor(if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) Color.parseColor("#9CA3AF") else Color.parseColor("#4B5563"))
                if (index > 0) lineTop.setBackgroundColor(Color.parseColor("#1F5D57"))
                if (!isCurrent && index < steps.size - 1) lineBottom.setBackgroundColor(Color.parseColor("#1F5D57"))
            } else {
                dot.setImageResource(android.R.drawable.checkbox_off_background)
                dot.setColorFilter(Color.parseColor("#D1D5DB"))
                title.setTextColor(Color.parseColor("#9CA3AF"))
                desc.setTextColor(Color.parseColor("#9CA3AF"))
                if (index > 0) lineTop.setBackgroundColor(Color.parseColor("#D1D5DB"))
                if (index < steps.size - 1) lineBottom.setBackgroundColor(Color.parseColor("#D1D5DB"))
            }

            if (isCurrent) {
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            } else {
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            }
        }
    }

    private fun updateActionButton() {
        val btn = findViewById<Button>(R.id.btn_next_status)
        when (currentNotification.status) {
            OrderStatus.REQUESTED -> btn.text = "ACCEPT REQUEST"
            OrderStatus.ACCEPTED -> btn.text = "MOVE TO PROCESSING"
            OrderStatus.PROCESSING -> btn.text = "MARK AS READY"
            OrderStatus.READY -> btn.text = "MARK AS DELIVERED"
            OrderStatus.DELIVERED -> btn.text = "MARK AS COMPLETED"
            OrderStatus.COMPLETED -> {
                btn.visibility = View.GONE
            }
        }
    }

    private fun moveToNextStatus() {
        val nextStatus = when (currentNotification.status) {
            OrderStatus.REQUESTED -> OrderStatus.ACCEPTED
            OrderStatus.ACCEPTED -> OrderStatus.PROCESSING
            OrderStatus.PROCESSING -> OrderStatus.READY
            OrderStatus.READY -> OrderStatus.DELIVERED
            OrderStatus.DELIVERED -> OrderStatus.COMPLETED
            OrderStatus.COMPLETED -> OrderStatus.COMPLETED
        }

        val id = currentNotification.id
        val batch = db.batch()
        batch.update(db.collection("notifications").document(id), "status", nextStatus)
        batch.update(db.collection("bulk_orders").document(id), "status", nextStatus)
        
        batch.commit().addOnSuccessListener {
            currentNotification.status = nextStatus
            updateUI()
            Snackbar.make(findViewById(android.R.id.content), "Status updated to ${nextStatus.displayName}", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(Color.parseColor("#1F5D57"))
                .show()
        }
    }
}
