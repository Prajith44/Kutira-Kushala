package com.example.kutira_kushala

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ProductItem(
    val name: String = "",
    val category: String = "",
    val price: String = "",
    val wholesalePrice: String = "",
    val description: String = "",
    
    @get:Exclude
    val imageUris: List<Uri> = emptyList(), // Local Uris for uploading
    
    val imageUrls: List<String> = emptyList(), // Remote URLs from Storage
    
    @get:Exclude
    val additionalImages: List<Int> = emptyList(), // For demo res IDs

    val isAvailable: Boolean = true,
    val productId: String = "",
    val sellerId: String = "",
    val createdAt: Timestamp? = null
)
