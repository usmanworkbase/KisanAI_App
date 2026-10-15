package com.example.kisanai

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.FileProvider

import com.google.gson.Gson
import com.example.kisanai.databinding.FragmentScanBinding

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private var classifier: Classifier? = null
    private var originalBitmap: Bitmap? = null
    private var photoUri: Uri? = null
    private var diseases: List<Disease> = emptyList()

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(requireContext().contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                }
                originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                binding.ivSelectedImage.setImageBitmap(originalBitmap)
                showSelectedImage()
            } catch (e: Exception) {
                Log.e("ScanFragment", "Gallery error: ${e.message}")
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                    }
                    originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    binding.ivSelectedImage.setImageBitmap(originalBitmap)
                    showSelectedImage()
                } catch (e: Exception) {
                    Log.e("ScanFragment", "Camera error: ${e.message}")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)

        binding.btnUploadArea.setOnClickListener {
            showImagePickerDialog()
        }

        classifier = Classifier(requireContext())
        loadDiseases()

        binding.btnScanLeaf.setOnClickListener {
            if (binding.ivSelectedImage.visibility == View.VISIBLE) {
                // Lock the inputs
                binding.btnScanLeaf.isEnabled = false
                binding.btnUploadArea.isClickable = false
                runInference()
            } else {
                Toast.makeText(requireContext(), "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun runInference() {
        val bitmap = originalBitmap
        if (bitmap != null && classifier != null) {
            binding.btnScanLeaf.isEnabled = false
            lifecycleScope.launch(Dispatchers.Default) {
                try {
                    val result = classifier!!.recognizeImage(bitmap)
                    withContext(Dispatchers.Main) {
                        displayResult(result)
                        when {
                            result.isError -> {
                                binding.btnScanLeaf.isEnabled = true
                                binding.btnUploadArea.isClickable = true
                            }
                            result.isLeaf -> {
                                saveScanToHistory(bitmap, result)
                                // Stays locked until new image is picked
                            }
                            else -> {
                                // Not a leaf
                                Toast.makeText(requireContext(), "Leaf not detected!", Toast.LENGTH_LONG).show()
                                binding.btnScanLeaf.isEnabled = true
                                binding.btnUploadArea.isClickable = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Inference failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.btnScanLeaf.isEnabled = true
                    }
                }
            }
        }
    }

    private fun saveScanToHistory(bitmap: Bitmap, recognition: Classifier.Recognition) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        binding.pbSaving.visibility = View.VISIBLE
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert bitmap to Base64 string instead of uploading to Firebase Storage
                // Resize if too large to keep Firestore document size under 1MB
                val maxDimension = 640
                val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                    val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                    Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                } else {
                    bitmap
                }

                val baos = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                val bytes = baos.toByteArray()
                val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                val imageUrl = "data:image/jpeg;base64,$base64Image"

                val historyItem = HistoryItem(
                    userId = user.uid,
                    imageUrl = imageUrl,
                    crop = recognition.cropName,
                    disease = recognition.disease,
                    confidence = recognition.confidence,
                    timestamp = com.google.firebase.Timestamp.now(),
                    rawLabel = recognition.rawLabel
                )

                FirebaseFirestore.getInstance().collection("history")
                    .add(historyItem)
                    .await()
                
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        binding.pbSaving.visibility = View.GONE
                        Log.d("ScanFragment", "Scan saved to Firestore history")
                        Toast.makeText(requireContext(), "Saved to history", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        binding.pbSaving.visibility = View.GONE
                        Log.e("ScanFragment", "Save Error: ${e.message}", e)
                        Toast.makeText(requireContext(), "Sync failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun loadDiseases() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonString = requireContext().assets.open("disease.json").bufferedReader().use { it.readText() }
                val response = Gson().fromJson(jsonString, DiseaseResponse::class.java)
                diseases = response?.diseases ?: emptyList()
                Log.d("ScanFragment", "Loaded ${diseases.size} diseases")
            } catch (e: Exception) {
                Log.e("ScanFragment", "Error loading diseases: ${e.message}")
            }
        }
    }

    private fun displayResult(result: Classifier.Recognition) {
        if (result.isError) {
            binding.layoutResults.visibility = View.VISIBLE
            binding.tvResultCrop.text = "Error"
            binding.tvResultDisease.text = result.disease
            binding.tvResultConfidence.text = ""
            binding.tvResultDisease.setTextColor(resources.getColor(R.color.error_red, null))
            binding.btnViewDetails.visibility = View.GONE
        } else if (result.isLeaf) {
            // Show the detected crop in the text area
            binding.tvDetectedCrop.text = result.cropName
            
            // Show result layout instead of navigating automatically
            binding.layoutResults.visibility = View.VISIBLE
            binding.tvResultCrop.text = "Crop: ${result.cropName}"
            binding.tvResultDisease.text = "Disease: ${result.disease}"
            binding.tvResultConfidence.text = "Confidence: ${(result.confidence * 100).toInt()}%"
            binding.tvResultDisease.setTextColor(resources.getColor(R.color.error_red, null))
            
            val foundDisease = diseases.find { it.class_name == result.rawLabel }
            if (foundDisease != null && foundDisease.severity != "None") {
                binding.btnViewDetails.visibility = View.VISIBLE
                binding.btnViewDetails.setOnClickListener {
                    navigateToDetails(foundDisease)
                }
            } else {
                binding.btnViewDetails.visibility = View.GONE
            }
        } else {
            binding.layoutResults.visibility = View.VISIBLE
            binding.tvResultCrop.text = "Detection Failed"
            binding.tvResultDisease.text = "Not a Leaf"
            binding.tvResultConfidence.text = "Confidence: ${(result.confidence * 100).toInt()}%"
            binding.tvResultDisease.setTextColor(resources.getColor(R.color.black, null))
            binding.btnViewDetails.visibility = View.GONE
        }
    }

    private fun navigateToDetails(disease: Disease) {
        val bitmap = originalBitmap ?: return
        
        // Save bitmap to temporary file to pass between activities
        val cachePath = File(requireContext().cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "selected_leaf.jpg")
        try {
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.close()
            
            val intent = Intent(requireContext(), DiseaseDetailActivity::class.java)
            intent.putExtra("disease_data", disease)
            intent.putExtra("image_uri", file.absolutePath)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ScanFragment", "Error saving temp image: ${e.message}")
            // Fallback: just open without image
            val intent = Intent(requireContext(), DiseaseDetailActivity::class.java)
            intent.putExtra("disease_data", disease)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        classifier?.close()
        _binding = null
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(getString(R.string.camera), getString(R.string.gallery))
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.select_image))
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> takePhoto()
                1 -> pickFromGallery()
            }
        }
        builder.show()
    }

    private fun takePhoto() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: Exception) {
            null
        }

        photoFile?.also {
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.kisanai.fileprovider",
                it
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(intent)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun pickFromGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showSelectedImage() {
        binding.layoutPlaceholder.visibility = View.GONE
        binding.ivSelectedImage.visibility = View.VISIBLE
        
        // Reset locks and results for new image
        binding.tvDetectedCrop.text = "—"
        binding.btnUploadArea.isClickable = true
        binding.btnScanLeaf.isEnabled = true
        binding.layoutResults.visibility = View.GONE
    }
}