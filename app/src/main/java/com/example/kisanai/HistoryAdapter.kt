package com.example.kisanai

import android.app.Dialog
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.Date

class HistoryAdapter(
    private var items: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit,
    private val onDeleteClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var diseaseList: List<Disease> = emptyList()

    fun setDiseases(diseases: List<Disease>) {
        this.diseaseList = diseases
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivHistoryImage)
        val pbLoader: View = view.findViewById(R.id.pbImageLoader)
        val tvDisease: TextView = view.findViewById(R.id.tvHistoryDisease)
        val tvCrop: TextView = view.findViewById(R.id.tvHistoryCrop)
        val tvConfidence: TextView = view.findViewById(R.id.tvHistoryConfidence)
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.pbLoader.visibility = View.VISIBLE
        
        if (item.imageUrl.startsWith("data:image")) {
            try {
                val base64String = item.imageUrl.substringAfter("base64,")
                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                holder.ivImage.load(imageBytes) {
                    crossfade(true)
                    listener(
                        onSuccess = { _, _ -> holder.pbLoader.visibility = View.GONE },
                        onError = { _, _ -> holder.pbLoader.visibility = View.GONE }
                    )
                    error(R.drawable.ic_scan)
                }
            } catch (e: Exception) {
                holder.pbLoader.visibility = View.GONE
                holder.ivImage.load(R.drawable.ic_scan)
            }
        } else {
            holder.ivImage.load(item.imageUrl) {
                crossfade(true)
                listener(
                    onSuccess = { _, _ -> holder.pbLoader.visibility = View.GONE },
                    onError = { _, _ -> holder.pbLoader.visibility = View.GONE }
                )
                error(R.drawable.ic_scan)
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.ivImage.setOnClickListener {
            showFullScreenImage(holder.itemView.context, item.imageUrl)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }

        val context = holder.itemView.context
        val language = LocaleHelper.getLanguage(context)
        val isUrdu = language == "ur"
        
        val foundDisease = diseaseList.find { it.class_name == item.rawLabel }
        
        if (isUrdu && foundDisease != null) {
            holder.tvDisease.text = foundDisease.display_name_ur
            holder.tvCrop.text = "فصل: ${foundDisease.crop_ur}"
            holder.tvConfidence.text = "${(item.confidence * 100).toInt()}% اعتماد"
        } else {
            holder.tvDisease.text = item.disease
            holder.tvCrop.text = context.getString(R.string.crop_label, item.crop)
            holder.tvConfidence.text = context.getString(R.string.confidence_label, (item.confidence * 100).toInt())
        }
        
        item.timestamp?.let {
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                it.toDate().time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            holder.tvTime.text = timeAgo
        }
    }

    private fun showFullScreenImage(context: android.content.Context, imageUrl: String) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_full_image)
        
        val imageView = dialog.findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnClose)
        
        if (imageUrl.startsWith("data:image")) {
            val base64String = imageUrl.substringAfter("base64,")
            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            imageView.load(imageBytes)
        } else {
            imageView.load(imageUrl)
        }
        
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<HistoryItem>) {
        items = newList
        notifyDataSetChanged()
    }
}
