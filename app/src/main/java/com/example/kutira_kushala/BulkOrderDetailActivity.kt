package com.example.kutira_kushala

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class BulkOrderDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bulk_order_detail)

        val notificationId = intent.getStringExtra("NOTIFICATION_ID") ?: ""
        
        if (notificationId.isNotEmpty()) {
            loadNotificationDetails(notificationId)
        } else {
            finish()
        }

        findViewById<View>(R.id.btn_back_bulk).setOnClickListener {
            finish()
        }
    }

    private fun loadNotificationDetails(id: String) {
        db.collection("notifications").document(id).get()
            .addOnSuccessListener { doc ->
                val notification = doc.toObject(Notification::class.java)
                if (notification != null) {
                    setupUI(notification)
                } else {
                    Toast.makeText(this, "Order details not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupUI(notification: Notification) {
        findViewById<TextView>(R.id.tv_buyer_name).text = notification.senderName
        findViewById<TextView>(R.id.tv_buyer_location).text = notification.buyerLocation.ifEmpty { "Location not specified" }
        findViewById<TextView>(R.id.tv_bulk_product).text = notification.productName
        findViewById<TextView>(R.id.tv_bulk_qty).text = "${notification.quantity} units"
        findViewById<TextView>(R.id.tv_bulk_deadline).text = notification.deadline.ifEmpty { "Flexible" }
        findViewById<TextView>(R.id.tv_bulk_message).text = notification.message.ifEmpty { "No special requirements." }
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        findViewById<TextView>(R.id.tv_bulk_timestamp)?.text = if (notification.timestamp != null) {
            dateFormat.format(notification.timestamp.toDate())
        } else {
            "Just now"
        }

        findViewById<Button>(R.id.btn_call_buyer).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${notification.senderMobile}")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_whatsapp_buyer).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?phone=91${notification.senderMobile}")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_accept).setOnClickListener {
            updateStatus(notification.id, OrderStatus.ACCEPTED)
        }

        findViewById<Button>(R.id.btn_reject).setOnClickListener {
            val snackbar = Snackbar.make(it, "Request rejected", Snackbar.LENGTH_LONG)
            snackbar.setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
            snackbar.show()
        }
    }

    private fun updateStatus(id: String, status: OrderStatus) {
        val batch = db.batch()
        batch.update(db.collection("notifications").document(id), "status", status)
        batch.update(db.collection("bulk_orders").document(id), "status", status)
        
        batch.commit().addOnSuccessListener {
            val intent = Intent(this, OrderTrackingActivity::class.java)
            intent.putExtra("NOTIFICATION_ID", id)
            startActivity(intent)
            finish()
        }
    }
}
