package com.example.kisanai

import com.google.firebase.Timestamp

data class IrrigationItem(
    val id: String = "",
    val userId: String = "",
    val cropName: String = "",
    val date: String = "",
    val nextDate: String = "",
    val location: String = "",
    val timestamp: Timestamp? = null,
    val isAI: Boolean = false
)
