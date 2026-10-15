package com.example.kisanai

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherForecastActivity : AppCompatActivity() {

    private lateinit var rvHourly: RecyclerView
    private lateinit var rvDaily: RecyclerView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var scrollView: androidx.core.widget.NestedScrollView
    private val apiKey = "f765705edfb49cc17e94a9ef26ef865a"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_weather_forecast)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        rvHourly = findViewById(R.id.rvHourlyForecast)
        rvDaily = findViewById(R.id.rvDailyForecast)
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)

        val lat = intent.getDoubleExtra("lat", 31.5204)
        val lon = intent.getDoubleExtra("lon", 74.3587)

        fetchForecastData(lat, lon)
    }

    private fun fetchForecastData(lat: Double, lon: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherApiService::class.java)

        // 1. Fetch Current Weather for the Featured Card
        service.getCurrentWeather(lat, lon, apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val current = response.body()
                    current?.let {
                        findViewById<android.widget.TextView>(R.id.tvMainTemp).text = "${it.main.temp.toInt()}°"
                        findViewById<android.widget.TextView>(R.id.tvMainCondition).text = it.weather.firstOrNull()?.description?.replaceFirstChar { it.titlecase() } ?: ""
                        findViewById<android.widget.TextView>(R.id.tvHumidityVal).text = "${it.main.humidity}%"
                        findViewById<android.widget.TextView>(R.id.tvRainVal).text = "${it.clouds.all}%"
                        findViewById<android.widget.TextView>(R.id.tvWindVal).text = "${it.wind.speed} km/h"

                        val iconCode = it.weather.firstOrNull()?.icon ?: ""
                        if (iconCode.isNotEmpty()) {
                            val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@4x.png"
                            findViewById<android.widget.ImageView>(R.id.ivMainIcon).load(iconUrl)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {}
        })

        // 2. Fetch Forecast for the Lists
        service.getForecast(lat, lon, apiKey).enqueue(object : Callback<ForecastResponse> {
            override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                progressBar.visibility = android.view.View.GONE
                if (response.isSuccessful) {
                    scrollView.visibility = android.view.View.VISIBLE
                    val forecastData = response.body()
                    forecastData?.let {
                        // The free OpenWeatherMap API provides data every 3 hours for 5 days.
                        // To show 24 hours, we need 8 items (8 * 3 = 24).
                        val hourlyItems = it.list.take(8) 
                        rvHourly.adapter = HourlyForecastAdapter(hourlyItems)

                        // To show all 5 available days (Today + 4 days), we take one snapshot per day.
                        val dailyItems = it.list.filterIndexed { index, _ -> index % 8 == 0 }
                        rvDaily.adapter = DailyForecastAdapter(dailyItems)
                    }
                } else {
                    Toast.makeText(this@WeatherForecastActivity, "Error fetching forecast", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@WeatherForecastActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
