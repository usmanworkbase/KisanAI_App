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

class DailyForecastAdapter(private val items: List<ForecastItem>) :
    RecyclerView.Adapter<DailyForecastAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val ivIcon: ImageView = view.findViewById(R.id.ivDailyIcon)
        val tvCondition: TextView = view.findViewById(R.id.tvDailyCondition)
        val tvTemp: TextView = view.findViewById(R.id.tvDailyTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // Format day from dt
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val date = Date(item.dt * 1000)
        holder.tvDay.text = if (position == 0) "Today" else sdf.format(date)
        
        val weatherObj = item.weather.firstOrNull()
        holder.tvCondition.text = weatherObj?.description?.replaceFirstChar { it.titlecase() } ?: ""
        holder.tvTemp.text = "${item.main.temp.toInt()}°C"
        
        val iconCode = weatherObj?.icon ?: ""
        if (iconCode.isNotEmpty()) {
            val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
            holder.ivIcon.load(iconUrl)
        }
    }

    override fun getItemCount() = items.size
}
