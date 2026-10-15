package com.example.kisanai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.kisanai.databinding.ActivityDiseaseDetailBinding
import java.util.Locale

class DiseaseDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityDiseaseDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiseaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val disease = intent.getParcelableExtra<Disease>("disease_data")
        val imageUri = intent.getStringExtra("image_uri")

        if (disease == null) {
            finish()
            return
        }

        setupToolbar()
        displayDiseaseDetails(disease, imageUri)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = getString(R.string.disease_details)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun displayDiseaseDetails(disease: Disease, imageUri: String?) {
        val language = LocaleHelper.getLanguage(this)
        val isUrdu = language == "ur"

        imageUri?.let { uri ->
            if (uri.startsWith("data:image")) {
                try {
                    val base64String = uri.substringAfter("base64,")
                    val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                    binding.ivDiseaseImage.load(imageBytes)
                } catch (e: Exception) {
                    binding.ivDiseaseImage.load(R.drawable.ic_scan)
                }
            } else {
                binding.ivDiseaseImage.load(uri)
            }
        }

        // Set Text Alignment based on language
        val alignment = if (isUrdu) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
        binding.tvDisplayName.textDirection = alignment
        binding.tvDescription.textDirection = alignment
        binding.tvSymptomsList.textDirection = alignment
        binding.tvPreventionList.textDirection = alignment

        binding.tvDisplayName.text = if (isUrdu) disease.display_name_ur else disease.display_name
        binding.tvCategory.text = if (isUrdu) disease.category_ur else disease.category
        
        val severityValue = if (isUrdu) disease.severity_ur else disease.severity
        binding.tvSeverity.text = "${getString(R.string.severity_label)}: $severityValue"
        
        val cropValue = if (isUrdu) disease.crop_ur else disease.crop
        binding.tvCrop.text = "${getString(R.string.crop_optional)}: $cropValue"
        
        binding.tvDescription.text = if (isUrdu) disease.description_ur else disease.description
        binding.tvRecoveryTime.text = if (isUrdu) disease.estimated_recovery_time_ur else disease.estimated_recovery_time

        val symptoms = (if (isUrdu) disease.symptoms_ur else disease.symptoms) ?: emptyList()
        binding.tvSymptomsList.text = symptoms.joinToString("\n") { "• $it" }

        val prevention = (if (isUrdu) disease.prevention_ur else disease.prevention) ?: emptyList()
        binding.tvPreventionList.text = prevention.joinToString("\n") { "• $it" }

        // Treatments
        binding.layoutTreatments.removeAllViews()
        val inflater = LayoutInflater.from(this)
        
        disease.treatment?.forEach { treatment ->
            val tView = inflater.inflate(android.R.layout.simple_list_item_2, binding.layoutTreatments, false)
            val text1 = tView.findViewById<TextView>(android.R.id.text1)
            val text2 = tView.findViewById<TextView>(android.R.id.text2)

            text1.textDirection = alignment
            text2.textDirection = alignment

            val name = if (isUrdu) treatment.name_ur ?: "" else treatment.name ?: ""
            val type = if (isUrdu) treatment.type_ur ?: "" else treatment.type ?: ""
            val usage = if (isUrdu) treatment.usage_ur ?: "" else treatment.usage ?: ""

            text1.text = if (type.isNotEmpty()) "$name ($type)" else name
            text2.text = usage
            
            text1.setTextColor(resources.getColor(R.color.black, null))
            text2.setTextColor(resources.getColor(R.color.dark_green, null))
            
            binding.layoutTreatments.addView(tView)
        }
    }
}
