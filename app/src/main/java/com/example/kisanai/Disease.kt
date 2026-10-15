package com.example.kisanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Disease(
    val id: Int,
    val class_name: String? = "",
    val crop: String? = "",
    val crop_ur: String? = "",
    val display_name: String? = "",
    val display_name_ur: String? = "",
    val category: String? = "",
    val category_ur: String? = "",
    val description: String? = "",
    val description_ur: String? = "",
    val symptoms: List<String>? = emptyList(),
    val symptoms_ur: List<String>? = emptyList(),
    val prevention: List<String>? = emptyList(),
    val prevention_ur: List<String>? = emptyList(),
    val treatment: List<Treatment>? = emptyList(),
    val estimated_recovery_time: String? = "",
    val estimated_recovery_time_ur: String? = "",
    val severity: String? = "",
    val severity_ur: String? = ""
) : Parcelable

@Parcelize
data class Treatment(
    val name: String? = "",
    val name_ur: String? = "",
    val type: String? = "",
    val type_ur: String? = "",
    val usage: String? = "",
    val usage_ur: String? = ""
) : Parcelable

data class DiseaseResponse(
    val diseases: List<Disease>? = emptyList()
)
