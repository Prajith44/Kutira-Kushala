package com.example.kutira_kushala

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Seller(
    val sellerId: String = "",
    val businessName: String = "",
    val sellerName: String = "",
    val location: String = "",
    val profileImageUrl: String = "",
    val skills: List<String> = emptyList(),
    val experienceYears: Int = 0,
    val about: String = "",
    val rating: Float = 0f,
    val reviewsCount: Int = 0,
    val productsCount: Int = 0,
    val mobileNumber: String = ""
)
