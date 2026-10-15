package com.example.kisanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IrrigationAdapter(
    private var items: List<IrrigationItem>,
    private val onDeleteClick: (IrrigationItem) -> Unit
) : RecyclerView.Adapter<IrrigationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCropName: TextView = view.findViewById(R.id.tvCropName)
        val tvIrrigationDate: TextView = view.findViewById(R.id.tvIrrigationDate)
        val tvNextDate: TextView = view.findViewById(R.id.tvNextDate)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val ivIcon: android.widget.ImageView = view.findViewById(R.id.ivIrrigationIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_irrigation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCropName.text = if (item.isAI) "${item.cropName} (AI Prediction)" else item.cropName
        
        if (item.isAI) {
            holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#2196F3")) // Blue for AI
            holder.ivIcon.setBackgroundResource(R.drawable.bg_blue_light)
        } else {
            holder.ivIcon.setColorFilter(holder.itemView.context.getColor(R.color.primary_green))
            holder.ivIcon.setBackgroundResource(R.drawable.bg_circle_green_light)
        }

        val dateString = if (item.isAI) {
            holder.itemView.context.getString(R.string.ai_prediction_label, item.date)
        } else {
            holder.itemView.context.getString(R.string.irrigation_date_display, item.date)
        }
        holder.tvIrrigationDate.text = dateString

        if (!item.isAI) {
            holder.tvNextDate.visibility = View.VISIBLE
            if (item.nextDate.isNotEmpty()) {
                if (item.nextDate == "Error") {
                    holder.tvNextDate.text = holder.itemView.context.getString(R.string.ai_failed)
                    holder.tvNextDate.setTextColor(android.graphics.Color.RED)
                } else {
                    holder.tvNextDate.text = holder.itemView.context.getString(R.string.next_irrigation_date, item.nextDate)
                    holder.tvNextDate.setTextColor(holder.itemView.context.getColor(R.color.primary_green))
                }
            } else {
                holder.tvNextDate.text = holder.itemView.context.getString(R.string.ai_calculating)
                holder.tvNextDate.setTextColor(android.graphics.Color.GRAY)
            }
        } else {
            holder.tvNextDate.visibility = View.GONE
        }
        
        if (item.location.isNotEmpty()) {
            holder.tvLocation.text = item.location
            holder.tvLocation.visibility = View.VISIBLE
        } else {
            holder.tvLocation.visibility = View.GONE
        }
        
        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<IrrigationItem>) {
        items = newList
        notifyDataSetChanged()
    }
}
