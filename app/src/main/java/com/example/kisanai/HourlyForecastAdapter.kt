package com.example.kisanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.text.SimpleDateFormat
import java.util.*

class HourlyForecastAdapter(private val items: List<ForecastItem>) :
    RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHour: TextView = view.findViewById(R.id.tvHour)
        val ivIcon: ImageView = view.findViewById(R.id.ivHourlyIcon)
        val tvTemp: TextView = view.findViewById(R.id.tvHourlyTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // Format time from dt (unix timestamp)
        val sdf = SimpleDateFormat("h a", Locale.getDefault())
        val date = Date(item.dt * 1000)
        holder.tvHour.text = sdf.format(date)
        
        holder.tvTemp.text = "${item.main.temp.toInt()}°C"
        
        val iconCode = item.weather.firstOrNull()?.icon ?: ""
        if (iconCode.isNotEmpty()) {
            val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
            holder.ivIcon.load(iconUrl)
        }
    }

    override fun getItemCount() = items.size
}
