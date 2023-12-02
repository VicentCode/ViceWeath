package com.vicentcode.viceweath

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.BuildConfig
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.vicentcode.viceweath.databinding.ActivityMainBinding
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var requestQueue: RequestQueue
    private lateinit var stringRequest: StringRequest
    private var weatherTexts = Array<String?>(4) { null }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestQueue = Volley.newRequestQueue(this)

        val apiKey= com.vicentcode.viceweath.BuildConfig.API_KEY
        val url = "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=24.446693,-104.122912&days=7&lang=en"

        stringRequest = object : StringRequest(Method.GET, url,
            Response.Listener {
                val current = JSONObject(it).getJSONObject("current")

                with(binding) {
                    tvWindData.text = "${current.getDouble("wind_kph")}km/h"
                    tvHumiData.text = "${current.getDouble("humidity")}%"
                    tvPrepData.text = "${current.getDouble("precip_mm")}mm"
                    tvTemp.text = "${current.getDouble("temp_c")}°c"
                    wDesc.text = current.getJSONObject("condition").getString("text")
                    wUbi.text = JSONObject(it).getJSONObject("location").getString("name")


                    Glide.with(this@MainActivity).load("https:${current.getJSONObject("condition").getString
                        ("icon").replace("64x64","128x128")}").into(imWCondition)

                }

                val currentDateTime = LocalDateTime.now()
                val currentHour = currentDateTime.hour

                val forecastDays = JSONObject(it).getJSONObject("forecast").getJSONArray("forecastday")
                var currentIndex = -1

                for (i in 0 until forecastDays.length()) {
                    val date = forecastDays.getJSONObject(i).getString("date")
                    val forecastDate = LocalDate.parse(date)
                    if (currentDateTime.toLocalDate() == forecastDate) {
                        currentIndex = i
                        break
                    }
                }

                if (currentIndex != -1) {
                    val currentDayForecast = forecastDays.getJSONObject(currentIndex)
                    val currentDayHours = currentDayForecast.getJSONArray("hour")

                    for (i in 0 until 4) {
                        val nextHourIndex = (currentHour + i+1) % 24
                        val hour = currentDayHours.getJSONObject(nextHourIndex)

                        weatherTexts[i] = hour.getJSONObject("condition").getString("text")

                        with(binding) {
                            val tvTemp = findViewById<TextView>(resources.getIdentifier("tvTimeTemp${i + 1}", "id", packageName))
                            val tvTime = findViewById<TextView>(resources.getIdentifier("tvTime${i + 1}", "id", packageName))
                            val imgTime = findViewById<ImageView>(resources.getIdentifier("imgTime${i + 1}", "id", packageName))

                           Picasso.get().load("https:${hour.getJSONObject("condition").getString("icon").replace
                               ("64x64", "128x128")}").into(imgTime)


                            tvTime.text = hour.getString("time").substring(11, 16)
                            tvTemp.text = "${hour.getDouble("temp_c")}°c"
                        }
                    }
                }

                setDataWeatherClick()
            }, Response.ErrorListener {

            }) {
            override fun getParams(): HashMap<String, String>? = null

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "Mozilla/5.0"
                return headers
            }
        }

        requestQueue.add(stringRequest)
    }

    private fun setDataWeatherClick() {
        with(binding) {
            timeCard1.setOnClickListener { showSnackbar(weatherTexts[0]) }
            timeCard2.setOnClickListener { showSnackbar(weatherTexts[1]) }
            timeCard3.setOnClickListener { showSnackbar(weatherTexts[2]) }
            timeCard4.setOnClickListener { showSnackbar(weatherTexts[3]) }
        }
    }

    private fun showSnackbar(text: String?) {
        text?.let {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
        }
    }
}
