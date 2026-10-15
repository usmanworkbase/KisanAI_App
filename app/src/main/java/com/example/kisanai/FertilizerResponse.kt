package com.example.kisanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FertilizerResponse(
    val fertilizers: List<FertilizerDiseaseInfo>? = null
) : Parcelable

@Parcelize
data class FertilizerDiseaseInfo(
    val id: Int,
    val disease_name: MultiLanguageText? = null,
    val crop: String? = null,
    val class_name: String? = null,
    val recommended_fertilizers: List<RecommendedFertilizer>? = null,
    val best_recommendation: BestRecommendation? = null,
    val farmer_notes: List<MultiLanguageText>? = null
) : Parcelable

@Parcelize
data class MultiLanguageText(
    val en: String? = null,
    val ur: String? = null
) : Parcelable

@Parcelize
data class RecommendedFertilizer(
    val priority: Int,
    val name: MultiLanguageText? = null,
    val brand_examples_pakistan: List<String>? = null,
    val type: MultiLanguageText? = null,
    val description: MultiLanguageText? = null,
    val purpose: MultiLanguageText? = null,
    val impact_on_crop: MultiLanguageText? = null,
    val application_method: MultiLanguageText? = null,
    val dosage: MultiLanguageText? = null,
    val first_application: MultiLanguageText? = null,
    val repeat_after_days: MultiLanguageText? = null,
    val best_weather_conditions: MultiLanguageText? = null,
    val avoid_using_when: MultiLanguageText? = null,
    val compatibility_with_fungicide: MultiLanguageText? = null,
    val availability_in_pakistan: Boolean = false
) : Parcelable

@Parcelize
data class BestRecommendation(
    val fertilizer_name: MultiLanguageText? = null,
    val reason: MultiLanguageText? = null
) : Parcelable
