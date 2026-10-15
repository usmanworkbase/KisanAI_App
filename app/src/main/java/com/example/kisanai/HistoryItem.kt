package com.example.kisanai

import com.google.firebase.Timestamp

data class HistoryItem(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val crop: String = "",
    val disease: String = "",
    val confidence: Float = 0f,
    val timestamp: Timestamp? = null,
    val rawLabel: String = ""
)
