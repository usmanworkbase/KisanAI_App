package com.example.kisanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CityAdapter(
    private val cities: List<City>,
    private val onCitySelected: (City) -> Unit
) : RecyclerView.Adapter<CityAdapter.ViewHolder>() {

    private var filteredCities: List<City> = ArrayList(cities)

    init {
        android.util.Log.d("CityAdapter", "Initializing with ${cities.size} cities")
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCityName: TextView = view.findViewById(R.id.tvCityName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val city = filteredCities[position]
        val isUrdu = LocaleHelper.getLanguage(holder.itemView.context) == "ur"
        holder.tvCityName.text = if (isUrdu) city.nameUr ?: city.nameEn else city.nameEn ?: city.nameUr
        
        holder.itemView.setOnClickListener {
            onCitySelected(city)
        }
    }

    override fun getItemCount() = filteredCities.size

    fun filter(query: String, isUrdu: Boolean) {
        filteredCities = if (query.isEmpty()) {
            cities
        } else {
            cities.filter {
                val name = if (isUrdu) it.nameUr ?: it.nameEn else it.nameEn ?: it.nameUr
                name?.contains(query, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }
}
