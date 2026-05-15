package com.example.kutira_kushala

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Chat(
    val chatId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Timestamp? = null,
    val unreadCount: Map<String, Int> = emptyMap(), // userId -> count
    val participantIds: List<String> = emptyList()
)

@IgnoreExtraProperties
data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val seen: Boolean = false,
    val type: String = "text" // "text", "image"
)
