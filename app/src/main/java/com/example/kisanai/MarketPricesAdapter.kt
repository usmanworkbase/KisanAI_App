package com.example.kisanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MarketPricesAdapter(private var prices: List<CropPrice>) :
    RecyclerView.Adapter<MarketPricesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCropName: TextView = view.findViewById(R.id.tvCropName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_market_price, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val price = prices[position]
        val context = holder.itemView.context
        val isUrdu = LocaleHelper.getLanguage(context) == "ur"
        
        holder.tvCropName.text = if (isUrdu) {
            price.commodityUr ?: price.commodityEn ?: "---"
        } else {
            price.commodityEn ?: price.commodityUr ?: "---"
        }
        
        val priceVal = price.avgPrice
        if (priceVal != null && priceVal > 0) {
            holder.tvPrice.text = context.getString(R.string.rs_unit, priceVal)
        } else {
            holder.tvPrice.text = if (isUrdu) "دستیاب نہیں" else "N/A"
        }
    }

    override fun getItemCount(): Int = prices.size

    fun updateList(newList: List<CropPrice>) {
        this.prices = newList
        notifyDataSetChanged()
    }
}
