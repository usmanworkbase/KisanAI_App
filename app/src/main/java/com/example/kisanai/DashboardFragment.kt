package com.example.kisanai

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvTemperature: TextView
    private lateinit var tvLocation: TextView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvRainProb: TextView
    private lateinit var tvUserName: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val apiKey = ""
    private var lastLat: Double = 31.5204
    private var lastLon: Double = 74.3587
    private var selectedCrop: String? = null
    private var currentWeatherData: WeatherResponse? = null
    private val claudeApiKey = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (isAdded) {
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                getLocationAndWeather()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
                // Default to Lahore if permission is denied
                fetchWeatherData(31.5204, 74.3587)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvLocation = view.findViewById(R.id.tvLocation)
        ivWeatherIcon = view.findViewById(R.id.ivWeatherIcon)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvWindSpeed = view.findViewById(R.id.tvWindSpeed)
        tvRainProb = view.findViewById(R.id.tvRainProb)
        tvUserName = view.findViewById(R.id.tvUserName)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        view.findViewById<View>(R.id.btnScan)?.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_scan
        }

        view.findViewById<View>(R.id.cardWeather)?.setOnClickListener {
            val intent = Intent(requireContext(), WeatherForecastActivity::class.java)
            intent.putExtra("lat", lastLat)
            intent.putExtra("lon", lastLon)
            startActivity(intent)
        }

        view.findViewById<View>(R.id.cardMarketPrices)?.setOnClickListener {
            startActivity(Intent(requireContext(), MarketPricesActivity::class.java))
        }

        view.findViewById<View>(R.id.irrigationbox)?.setOnClickListener {
            showCropSelectionDialog()
        }

        checkLocationPermissions()
        fetchUserName()

        return view
    }

    private fun showCropSelectionDialog() {
        val context = context ?: return
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_crop_selection)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val rgCrops = dialog.findViewById<RadioGroup>(R.id.rgCrops)
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btnConfirm)
        val isUrdu = LocaleHelper.getLanguage(context) == "ur"
        val alignment = if (isUrdu) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
        
        dialog.findViewById<TextView>(R.id.tvTitle).textDirection = alignment
        dialog.findViewById<TextView>(R.id.tvDesc).textDirection = alignment

        btnConfirm.setOnClickListener {
            val selectedId = rgCrops.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(requireContext(), getString(R.string.please_select_crop), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cropNameEn = when (selectedId) {
                R.id.rbWheat -> "Wheat"
                R.id.rbSugarcane -> "Sugarcane"
                R.id.rbPotato -> "Potato"
                R.id.rbMaize -> "Maize"
                R.id.rbRice -> "Rice"
                else -> ""
            }

            saveUserCrop(cropNameEn, dialog)
        }

        dialog.show()
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = lp
    }

    private fun saveUserCrop(cropName: String, dialog: Dialog) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        
        db.collection("users").document(user.uid)
            .update("selectedCrop", cropName)
            .addOnSuccessListener {
                dialog.dismiss()
                showIrrigationInputDialog(cropName)
            }
            .addOnFailureListener {
                val userMap = hashMapOf("selectedCrop" to cropName)
                db.collection("users").document(user.uid).set(userMap, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        dialog.dismiss()
                        showIrrigationInputDialog(cropName)
                    }
                    .addOnFailureListener { innerE ->
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Error: ${innerE.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun showIrrigationInputDialog(cropName: String) {
        val context = context ?: return
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_irrigation_input)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val tvDesc = dialog.findViewById<TextView>(R.id.tvDesc)
        val tvLastDate = dialog.findViewById<TextView>(R.id.tvLastDate)
        val btnSelectDate = dialog.findViewById<MaterialButton>(R.id.btnSelectDate)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btnSave)
        
        tvDesc.text = getString(R.string.last_irrigation_desc, cropName)
        
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        
        // Fetch last irrigation date for this crop
        db.collection("irrigation_history")
            .whereEqualTo("userId", user.uid)
            .whereEqualTo("cropName", cropName)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val lastDate = documents.documents[0].getString("date")
                    tvLastDate.text = getString(R.string.last_recorded_date, lastDate)
                    tvLastDate.visibility = View.VISIBLE
                }
            }

        var selectedDateString = ""
        
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    selectedDateString = String.format(Locale.US, "%02d/%02d/%d", dayOfMonth, month + 1, year)
                    btnSelectDate.text = selectedDateString
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
        
        btnSave.setOnClickListener {
            if (selectedDateString.isEmpty()) {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val data = hashMapOf(
                "userId" to user.uid,
                "cropName" to cropName,
                "date" to selectedDateString,
                "location" to (currentWeatherData?.name ?: ""),
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("irrigation_history")
                .add(data)
                .addOnSuccessListener { docRef ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), getString(R.string.irrigation_saved), Toast.LENGTH_SHORT).show()
                        predictNextIrrigation(cropName, selectedDateString, docRef.id)
                    }
                    dialog.dismiss()
                    activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_irrigation
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        
        dialog.show()
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = lp
    }

    private fun fetchUserName() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            // Use a listener for real-time updates (useful if name is set just after signup)
            db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, e ->
                    if (!isAdded) return@addSnapshotListener

                    if (e != null) {
                        Log.e("DashboardFragment", "Listen failed.", e)
                        tvUserName.text = "Kisan"
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val name = snapshot.getString("name")
                        selectedCrop = snapshot.getString("selectedCrop")
                        Log.d("DashboardFragment", "User data received: $name, Crop: $selectedCrop")
                        if (!name.isNullOrEmpty()) {
                            tvUserName.text = name
                        } else {
                            tvUserName.text = user.email?.split("@")?.get(0) ?: "Kisan"
                        }
                    } else {
                        Log.d("DashboardFragment", "No such document")
                        tvUserName.text = user.email?.split("@")?.get(0) ?: "Kisan"
                    }
                }
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLocationAndWeather()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getLocationAndWeather() {
        if (!isAdded) return
        
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastLat = location.latitude
                    lastLon = location.longitude
                    fetchWeatherData(lastLat, lastLon)
                } else {
                    // Try to get fresh location if last location is null (common on first run)
                    if (isAdded) {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            CancellationTokenSource().token
                        ).addOnSuccessListener { currentLocation ->
                            if (isAdded) {
                                if (currentLocation != null) {
                                    lastLat = currentLocation.latitude
                                    lastLon = currentLocation.longitude
                                }
                                // Fetch weather with either fresh location or Lahore fallback
                                fetchWeatherData(lastLat, lastLon)
                            }
                        }.addOnFailureListener {
                            if (isAdded) fetchWeatherData(lastLat, lastLon)
                        }
                    }
                }
            }.addOnFailureListener {
                if (isAdded) fetchWeatherData(lastLat, lastLon)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            if (isAdded) fetchWeatherData(lastLat, lastLon)
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherApiService::class.java)
        service.getCurrentWeather(lat, lon, apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    currentWeatherData = response.body()
                    currentWeatherData?.let {
                        val temp = it.main.temp.toInt()
                        val city = it.name
                        val weatherObj = it.weather.firstOrNull()
                        val description = weatherObj?.description ?: ""
                        val iconCode = weatherObj?.icon ?: ""
                        
                        tvTemperature.text = "${temp}°C"
                        tvLocation.text = "${city} • ${description.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
                        
                        tvHumidity.text = "${it.main.humidity}%"
                        tvWindSpeed.text = "${it.wind.speed} km/h"
                        tvRainProb.text = "${it.clouds.all}%"

                        if (iconCode.isNotEmpty()) {
                            val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
                            ivWeatherIcon.load(iconUrl)
                        }
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Weather data error", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun predictNextIrrigation(cropName: String, lastDate: String, docId: String) {
        val weather = currentWeatherData ?: run {
            Log.e("ClaudeAI", "Weather data is null, cannot predict")
            if (isAdded) {
                Toast.makeText(requireContext(), "Weather data not ready for AI prediction", Toast.LENGTH_SHORT).show()
                updateRecordWithAI(docId, "Error")
            }
            return
        }
        
        Log.d("ClaudeAI", "Starting prediction for $cropName at ${weather.name}")

        val todayDate = String.format(Locale.US, "%02d/%02d/%d", 
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            Calendar.getInstance().get(Calendar.MONTH) + 1,
            Calendar.getInstance().get(Calendar.YEAR))
        
        val prompt = """
            As an expert agricultural AI, predict the next irrigation date for a $cropName crop in ${weather.name}, Pakistan.
            Today's Date: $todayDate
            Current Weather in ${weather.name}:
            - Temperature: ${weather.main.temp}°C
            - Humidity: ${weather.main.humidity}%
            - Wind Speed: ${weather.wind.speed} km/h
            - Cloud Cover: ${weather.clouds.all}%
            - Last Irrigation Date: $lastDate
            
            Based on this specific weather and crop, when should the farmer next water this crop? 
            IMPORTANT: The predicted date MUST be in the FUTURE relative to Today's Date ($todayDate).
            Do NOT suggest any date before $todayDate.
            Respond ONLY with the date in dd/mm/yyyy format.
        """.trimIndent()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ClaudeApiService::class.java)
        val request = ClaudeRequest(messages = listOf(Message("user", prompt)))

        service.getCompletion(claudeApiKey, request).enqueue(object : Callback<ClaudeResponse> {
            override fun onResponse(call: Call<ClaudeResponse>, response: Response<ClaudeResponse>) {
                if (response.isSuccessful) {
                    val fullText = response.body()?.content?.firstOrNull()?.text ?: ""
                    Log.d("ClaudeAI", "Raw AI Response: $fullText")
                    
                    // Regex to find a date in dd/mm/yyyy format
                    val dateRegex = Regex("""\d{2}/\d{2}/\d{4}""")
                    val aiDate = dateRegex.find(fullText)?.value
                    
                    if (!aiDate.isNullOrEmpty()) {
                        Log.d("ClaudeAI", "Extracted Date: $aiDate")
                        updateRecordWithAI(docId, aiDate)
                        // Removed saveAIPrediction(cropName, aiDate) to stop creating duplicate entries
                    } else {
                        Log.e("ClaudeAI", "Could not extract date from response: $fullText")
                        // Optional: Update with a failure message so it's not stuck forever
                        updateRecordWithAI(docId, "Error")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ClaudeAI", "API Error: ${response.code()} - $errorBody")
                    updateRecordWithAI(docId, "Error")
                }
            }

            override fun onFailure(call: Call<ClaudeResponse>, t: Throwable) {
                Log.e("ClaudeAI", "Network Error: ${t.message}")
            }
        })
    }

    private fun updateRecordWithAI(docId: String, aiDate: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("irrigation_history").document(docId)
            .update("nextDate", aiDate)
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Failed to update AI date: ${e.message}")
            }
    }

    // Removed unused saveAIPrediction function
}
