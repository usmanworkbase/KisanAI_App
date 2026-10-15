package com.example.kisanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.kisanai.databinding.ItemFertilizerListBinding

class FertilizerAdapter(
    private var items: List<FertilizerDiseaseInfo>,
    private val isUrdu: Boolean,
    private val getImagePath: (String) -> String?,
    private val onItemClick: (FertilizerDiseaseInfo) -> Unit
) : RecyclerView.Adapter<FertilizerAdapter.ViewHolder>() {

    fun updateList(newList: List<FertilizerDiseaseInfo>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFertilizerListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemFertilizerListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FertilizerDiseaseInfo) {
            val alignment = if (isUrdu) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
            binding.tvDiseaseName.textDirection = alignment
            binding.tvCropName.textDirection = alignment
            
            binding.tvDiseaseName.text = if (isUrdu) item.disease_name?.ur else item.disease_name?.en
            binding.tvCropName.text = binding.root.context.getString(R.string.crop_label, item.crop)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
