package com.example.kisanai

import com.google.gson.annotations.SerializedName

data class MarketPriceData(
    @SerializedName("meta") val meta: Meta,
    @SerializedName("country") val country: CountrySummary,
    @SerializedName("cities") val cities: List<City>
)

data class Meta(
    @SerializedName("unit") val unit: PriceUnit
)

data class PriceUnit(
    @SerializedName("unit_en") val unitEn: String,
    @SerializedName("unit_ur") val unitUr: String
)

data class CountrySummary(
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("name_ur") val nameUr: String
)

data class City(
    @SerializedName("name_en") val nameEn: String?,
    @SerializedName("name_ur") val nameUr: String?,
    @SerializedName("prices") val prices: Map<String, CropPrice>
)

data class CropPrice(
    @SerializedName("commodity_en") val commodityEn: String?,
    @SerializedName("commodity_ur") val commodityUr: String?,
    @SerializedName("avg_price") val avgPrice: Double?,
    @SerializedName("is_real") val isReal: Boolean?
)
