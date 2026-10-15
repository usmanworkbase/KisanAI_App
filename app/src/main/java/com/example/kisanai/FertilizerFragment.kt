package com.example.kisanai

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.kisanai.databinding.DialogFertilizerDetailsBinding
import com.example.kisanai.databinding.FragmentFertilizerBinding
import com.example.kisanai.databinding.ItemFertilizerDetailsBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FertilizerFragment : Fragment() {

    private var _binding: FragmentFertilizerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FertilizerAdapter
    private var isUrdu: Boolean = false
    private var diseaseImages: Map<String, String> = emptyMap()
    private var allFertilizers: List<FertilizerDiseaseInfo> = emptyList()
    private var historyListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFertilizerBinding.inflate(inflater, container, false)
        
        isUrdu = LocaleHelper.getLanguage(requireContext()) == "ur"
        setupRecyclerView()
        loadFertilizerData()

        return binding.root
    }

    private fun listenToHistory() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        historyListener = FirebaseFirestore.getInstance().collection("history")
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FertilizerFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val imageMap = mutableMapOf<String, String>()
                    val detectedClasses = mutableSetOf<String>()
                    
                    snapshot.documents.forEach { doc ->
                        val className = doc.getString("rawLabel")
                        val imageUrl = doc.getString("imageUrl")
                        if (className != null && imageUrl != null) {
                            imageMap[className] = imageUrl
                            detectedClasses.add(className)
                        }
                    }
                    
                    diseaseImages = imageMap
                    filterAndDisplayFertilizers(detectedClasses)
                }
            }
    }

    private fun filterAndDisplayFertilizers(detectedClasses: Set<String>) {
        val filteredList = allFertilizers.filter { 
            detectedClasses.contains(it.class_name) 
        }
        
        if (_binding != null) {
            adapter.updateList(filteredList)
            if (filteredList.isEmpty()) {
                binding.layoutNoFertilizer.visibility = View.VISIBLE
                binding.rvFertilizers.visibility = View.GONE
            } else {
                binding.layoutNoFertilizer.visibility = View.GONE
                binding.rvFertilizers.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvFertilizers.layoutManager = LinearLayoutManager(requireContext())
        adapter = FertilizerAdapter(
            emptyList(),
            isUrdu,
            { className: String -> diseaseImages[className] },
            { item: FertilizerDiseaseInfo -> showFertilizerDialog(item) }
        )
        binding.rvFertilizers.adapter = adapter
    }

    private fun loadFertilizerData() {
        val context = context ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("fertilizer.json").bufferedReader().use { it.readText() }
                val response = Gson().fromJson(jsonString, FertilizerResponse::class.java)
                allFertilizers = response.fertilizers ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        listenToHistory()
                    }
                }
            } catch (e: Exception) {
                Log.e("FertilizerFragment", "Error loading fertilizer.json", e)
            }
        }
    }

    private fun showFertilizerDialog(data: FertilizerDiseaseInfo) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogFertilizerDetailsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val alignment = if (isUrdu) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR

        dialogBinding.tvDialogTitle.textDirection = alignment
        dialogBinding.tvDiseaseName.textDirection = alignment
        dialogBinding.tvCropName.textDirection = alignment
        dialogBinding.tvBestReason.textDirection = alignment
        dialogBinding.tvFarmerNotes.textDirection = alignment

        dialogBinding.tvDiseaseName.text = if (isUrdu) data.disease_name?.ur else data.disease_name?.en
        dialogBinding.tvCropName.text = getString(R.string.crop_label, data.crop)

        // Set disease image in dialog
        val imageUrl = data.class_name?.let { diseaseImages[it] }
        if (imageUrl != null) {
            dialogBinding.cvDiseaseImage.visibility = View.VISIBLE
            if (imageUrl.startsWith("data:image")) {
                try {
                    val base64String = imageUrl.substringAfter("base64,")
                    val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                    dialogBinding.ivDiseaseImage.load(imageBytes)
                } catch (e: Exception) {
                    dialogBinding.cvDiseaseImage.visibility = View.GONE
                }
            } else {
                dialogBinding.ivDiseaseImage.load(imageUrl)
            }
        } else {
            dialogBinding.cvDiseaseImage.visibility = View.GONE
        }

        data.recommended_fertilizers?.find { it.priority == 1 }?.let { fert ->
            setupFertilizerItem(dialogBinding.layoutPriority1, fert, isUrdu, alignment)
        }

        data.recommended_fertilizers?.find { it.priority == 2 }?.let { fert ->
            setupFertilizerItem(dialogBinding.layoutPriority2, fert, isUrdu, alignment)
        }

        dialogBinding.tvBestReason.text = if (isUrdu) data.best_recommendation?.reason?.ur else data.best_recommendation?.reason?.en
        
        val notes = data.farmer_notes?.map { if (isUrdu) it.ur else it.en }?.joinToString("\n") { "• $it" }
        dialogBinding.tvFarmerNotes.text = notes

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = lp
    }

    private fun setupFertilizerItem(
        itemBinding: ItemFertilizerDetailsBinding,
        fert: RecommendedFertilizer,
        isUrdu: Boolean,
        alignment: Int
    ) {
        itemBinding.tvFertilizerName.textDirection = alignment
        itemBinding.tvType.textDirection = alignment
        itemBinding.tvDescription.textDirection = alignment
        itemBinding.tvImpact.textDirection = alignment
        itemBinding.tvMethod.textDirection = alignment
        itemBinding.tvDosage.textDirection = alignment
        itemBinding.tvFirstApp.textDirection = alignment
        itemBinding.tvRepeat.textDirection = alignment
        itemBinding.tvWeather.textDirection = alignment
        itemBinding.tvAvoid.textDirection = alignment
        itemBinding.tvBrands.textDirection = alignment
        
        itemBinding.tvImpactLabel.textDirection = alignment
        itemBinding.tvMethodLabel.textDirection = alignment
        itemBinding.tvDosageLabel.textDirection = alignment
        itemBinding.tvFirstAppLabel.textDirection = alignment
        itemBinding.tvRepeatLabel.textDirection = alignment
        itemBinding.tvWeatherLabel.textDirection = alignment
        itemBinding.tvAvoidLabel.textDirection = alignment
        itemBinding.tvBrandsLabel.textDirection = alignment

        itemBinding.tvFertilizerName.text = if (isUrdu) fert.name?.ur else fert.name?.en
        itemBinding.tvType.text = if (isUrdu) fert.type?.ur else fert.type?.en
        itemBinding.tvDescription.text = if (isUrdu) fert.description?.ur else fert.description?.en
        itemBinding.tvImpact.text = if (isUrdu) fert.impact_on_crop?.ur else fert.impact_on_crop?.en
        itemBinding.tvMethod.text = if (isUrdu) fert.application_method?.ur else fert.application_method?.en
        itemBinding.tvDosage.text = if (isUrdu) fert.dosage?.ur else fert.dosage?.en
        itemBinding.tvFirstApp.text = if (isUrdu) fert.first_application?.ur else fert.first_application?.en
        itemBinding.tvRepeat.text = if (isUrdu) fert.repeat_after_days?.ur else fert.repeat_after_days?.en
        itemBinding.tvWeather.text = if (isUrdu) fert.best_weather_conditions?.ur else fert.best_weather_conditions?.en
        itemBinding.tvAvoid.text = if (isUrdu) fert.avoid_using_when?.ur else fert.avoid_using_when?.en
        itemBinding.tvBrands.text = fert.brand_examples_pakistan?.joinToString(", ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historyListener?.remove()
        _binding = null
    }
}
