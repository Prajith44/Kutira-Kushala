package com.example.kutira_kushala

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

enum class OrderStatus(val displayName: String) {
    REQUESTED("Requested"),
    ACCEPTED("Accepted"),
    PROCESSING("Processing"),
    READY("Ready"),
    DELIVERED("Delivered"),
    COMPLETED("Completed")
}

@IgnoreExtraProperties
data class Notification(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderMobile: String = "",
    val receiverId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val buyerName: String = "", // Keep for compatibility if needed
    val buyerLocation: String = "",
    val quantity: String = "",
    val message: String = "",
    val deadline: String = "",
    val timestamp: Timestamp? = null,
    val notificationType: String = "BULK_ORDER",
    var isRead: Boolean = false,
    var status: OrderStatus = OrderStatus.REQUESTED
)
