package com.example.kisanai

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import java.io.InputStreamReader
import java.util.Locale

class MarketPricesActivity : BaseActivity() {

    private lateinit var searchView: SearchView
    private lateinit var citySelector: MaterialCardView
    private lateinit var tvSelectedCity: TextView
    private lateinit var rvPrices: RecyclerView
    private lateinit var adapter: MarketPricesAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var allCities: List<City> = emptyList()
    private var selectedCity: City? = null
    private var currentPrices: List<CropPrice> = emptyList()
    private var currentSearchQuery: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getUserLocationAndMatchCity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_market_prices)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        loadMarketData()
        checkLocationPermissions()
        setupListeners()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getUserLocationAndMatchCity()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getUserLocationAndMatchCity() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    findNearestCity(location.latitude, location.longitude)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun findNearestCity(lat: Double, lon: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val userCityName = addresses[0].locality ?: addresses[0].subAdminArea
                Log.d("MarketPrices", "Detected user city: $userCityName")
                
                if (userCityName != null) {
                    val match = allCities.find { 
                        it.nameEn?.equals(userCityName, ignoreCase = true) == true ||
                        userCityName.contains(it.nameEn ?: "", ignoreCase = true)
                    }
                    
                    if (match != null) {
                        selectedCity = match
                        updateCityDisplay(LocaleHelper.getLanguage(this) == "ur")
                        updatePricesList()
                    } else {
                        // If no exact match, try to find a city in the same province
                        val province = addresses[0].adminArea // e.g., "Punjab"
                        Log.d("MarketPrices", "Trying fallback for province: $province")
                        val provinceMatch = allCities.find { 
                            province?.contains(it.nameEn ?: "", ignoreCase = true) == true ||
                            (it.nameEn?.equals("Lahore", true) == true && province?.contains("Punjab", true) == true) ||
                            (it.nameEn?.equals("Karachi", true) == true && province?.contains("Sindh", true) == true) ||
                            (it.nameEn?.equals("Peshawar", true) == true && province?.contains("Khyber", true) == true) ||
                            (it.nameEn?.equals("Quetta", true) == true && province?.contains("Balochistan", true) == true)
                        }
                        
                        if (provinceMatch != null) {
                            selectedCity = provinceMatch
                            updateCityDisplay(LocaleHelper.getLanguage(this) == "ur")
                            updatePricesList()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        searchView = findViewById(R.id.searchView)
        citySelector = findViewById(R.id.citySelector)
        tvSelectedCity = findViewById(R.id.tvSelectedCity)
        rvPrices = findViewById(R.id.rvPrices)
        
        rvPrices.layoutManager = LinearLayoutManager(this)
        adapter = MarketPricesAdapter(emptyList())
        rvPrices.adapter = adapter
    }

    private fun loadMarketData() {
        try {
            val inputStream = assets.open("pakistan_crop_prices_full.json")
            val reader = InputStreamReader(inputStream)
            val marketData = Gson().fromJson(reader, MarketPriceData::class.java)
            
            if (marketData != null) {
                allCities = marketData.cities
                Log.d("MarketPrices", "Loaded ${allCities.size} cities")
                
                val isUrdu = LocaleHelper.getLanguage(this) == "ur"
                // Try to default to Lahore, or first available city
                selectedCity = allCities.find { it.nameEn?.equals("Lahore", ignoreCase = true) == true } ?: allCities.firstOrNull()
                
                updateCityDisplay(isUrdu)
                updatePricesList()
            } else {
                Log.e("MarketPrices", "Market data or cities list is null/empty")
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e("MarketPrices", "Error loading JSON", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateCityDisplay(isUrdu: Boolean) {
        selectedCity?.let {
            tvSelectedCity.text = if (isUrdu) it.nameUr ?: it.nameEn else it.nameEn ?: it.nameUr
        }
    }

    private fun updatePricesList() {
        val city = selectedCity ?: return
        val isUrdu = LocaleHelper.getLanguage(this) == "ur"
        
        // Show all crops in the prices map, even if avgPrice is null (the adapter handles nulls)
        currentPrices = city.prices.values.toList()
        filterAndDisplay(currentSearchQuery, isUrdu)
    }

    private fun filterAndDisplay(query: String, isUrdu: Boolean) {
        val filtered = if (query.isEmpty()) {
            currentPrices
        } else {
            currentPrices.filter {
                val name = if (isUrdu) it.commodityUr else it.commodityEn
                name?.contains(query, ignoreCase = true) == true
            }
        }
        
        adapter.updateList(filtered)
        findViewById<View>(R.id.tvEmpty).visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        citySelector.setOnClickListener {
            showCitySelectionDialog()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                filterAndDisplay(
                    currentSearchQuery,
                    LocaleHelper.getLanguage(this@MarketPricesActivity) == "ur"
                )
                return true
            }
        })
    }

    private fun showCitySelectionDialog() {
        if (allCities.isEmpty()) {
            Toast.makeText(this, "Cities not loaded yet", Toast.LENGTH_SHORT).show()
            loadMarketData() // Try to reload if empty
            if (allCities.isEmpty()) return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_city_selection, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            
        // Remove default dialog background to allow custom background/rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val rvCities = dialogView.findViewById<RecyclerView>(R.id.rvCities)
        val dialogSearchView = dialogView.findViewById<SearchView>(R.id.dialogSearchView)
        val isUrdu = LocaleHelper.getLanguage(this) == "ur"

        if (rvCities == null) {
            Log.e("MarketPrices", "RecyclerView not found in dialog layout")
            return
        }

        rvCities.layoutManager = LinearLayoutManager(this)
        // Ensure we pass the latest allCities
        val cityAdapter = CityAdapter(allCities) { city ->
            selectedCity = city
            updateCityDisplay(isUrdu)
            updatePricesList()
            dialog.dismiss()
        }
        rvCities.adapter = cityAdapter
        
        Log.d("MarketPrices", "Opening dialog with ${allCities.size} cities")

        dialogSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                cityAdapter.filter(newText ?: "", isUrdu)
                return true
            }
        })

        dialog.show()
        // Make the dialog full screen or near full screen
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }
}
