package com.vicentcode.viceweath

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.vicentcode.viceweath.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.IOException
import java.text.Normalizer
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var requestQueue: RequestQueue
    private lateinit var stringRequest: StringRequest
    private var weatherTexts = Array<String?>(4) { null }
    private var url = ""
    val apiKey = com.vicentcode.viceweath.BuildConfig.API_KEY

    private val PERMISSION_REQUEST_CODE = 123
    lateinit var locationManager: LocationManager
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestQueue = Volley.newRequestQueue(this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        permissionCheckAndLoad()

        showHideSplash(true)


        val currentDateTime = LocalDateTime.now()
        val currentHour = currentDateTime.hour

        val greeting = when {
            currentHour in 0..11 -> getString(R.string.good_morning)
            currentHour in 12..18 -> getString(R.string.good_afternoon)
            else -> getString(R.string.good_evening)
        }


        binding.tvWelcome.text = greeting

        loadWeatherData(url)

        binding.ubicationCard.setOnClickListener {
            getNewLocation(this) { newUrl ->
                loadWeatherData(newUrl)
            }
        }

        //ubicationCard long click
        binding.ubicationCard.setOnLongClickListener {
            showHideSplash(true)
            loadWeatherData(permissionCheckAndLoad())
            true
        }
    }

    private fun permissionCheckAndLoad(): String {
        if (checkLocationPermission()) {
            var locationData = requestLocation()
            val cityName = getCityName(locationData[0].toDouble(), locationData[1].toDouble())

            if (Locale.getDefault().language == "es") {
                url = "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=${removeAccents(cityName)}&days=7&lang=es"
            } else
                url = "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=${removeAccents(cityName)}&days=7&lang=en"

            Log.e("Location", "Latitud: ${locationData[0]} Longitud: ${locationData[1]}")
            Log.e("Location", "URL: $url")
        } else {
            requestPermissions()
        }
        return url
    }


    private fun getNewLocation(context: Context, callback: (String) -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.enter_a_location))

        val input = EditText(context)
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.accept)) { dialog, _ ->
            val newLocation = input.text.toString()
            showHideSplash(true)

            if (Locale.getDefault().language == "es") {
                url =
                    "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$newLocation&days=7&lang=es"
            } else
                url =
                    "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$newLocation&days=7&lang=en"



            val stringRequest = object : StringRequest(Method.GET, url,
                Response.Listener {
                    callback(url)
                },
                Response.ErrorListener {
                    showSnackbar(getString(R.string.invalid_location))
                    showHideSplash(false)
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

        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun loadWeatherData(newUrl: String) {
        stringRequest =
            @SuppressLint("SetTextI18n")
            object : StringRequest(Method.GET, newUrl,
                Response.Listener { response ->

                    val current = JSONObject(response).getJSONObject("current")

                    with(binding) {
                        tvWindData.text = "${current.getDouble("wind_kph")}km/h"
                        tvHumiData.text = "${current.getDouble("humidity")}%"
                        tvPrepData.text = "${current.getDouble("precip_mm")}mm"
                        tvTemp.text = "${current.getDouble("temp_c")}°c"
                        wDesc.text = current.getJSONObject("condition").getString("text")
                        wUbi.text = JSONObject(response).getJSONObject("location").getString("name")

                        Glide.with(this@MainActivity).load(
                            "https:${
                                current.getJSONObject("condition").getString
                                    ("icon").replace("64x64", "128x128")
                            }"
                        ).into(imWCondition)
                    }

                    val calendar = Calendar.getInstance()
                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                    val forecastDays = JSONObject(response).getJSONObject("forecast").getJSONArray("forecastday")

                    val currentDayForecast = forecastDays.getJSONObject(0)
                    val currentDayHours = currentDayForecast.getJSONArray("hour")

                    for (i in 0 until 4) {
                        val nextHourIndex = (currentHour + i + 1) % 24
                        val hour = currentDayHours.getJSONObject(nextHourIndex)

                        weatherTexts[i] = hour.getJSONObject("condition").getString("text")

                        with(binding) {
                            val tvTemp =
                                findViewById<TextView>(resources.getIdentifier("tvTimeTemp${i + 1}", "id", packageName))
                            val tvTime =
                                findViewById<TextView>(resources.getIdentifier("tvTime${i + 1}", "id", packageName))
                            val imgTime =
                                findViewById<ImageView>(resources.getIdentifier("imgTime${i + 1}", "id", packageName))

                            Glide.with(this@MainActivity).load(
                                "https:${
                                    hour.getJSONObject("condition").getString
                                        ("icon").replace("64x64", "128x128")
                                }"
                            ).into(imgTime)

                            tvTime.text = hour.getString("time").substring(11, 16)
                            tvTemp.text = "${hour.getDouble("temp_c")}°c"

                            wMaxMin.text = "${
                                currentDayForecast.getJSONObject("day").getDouble("maxtemp_c")
                            }°c / ${currentDayForecast.getJSONObject("day").getDouble("mintemp_c")}°c"
                        }

                    }
                    setDataWeatherClick()

                    showHideSplash(false)
                },
                Response.ErrorListener { error ->
                    Log.e("WeatherData", "Error: $error")
                    showAlertDialog(getString(R.string.errorTitle), getString(R.string.errorMessage), false)
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

    private fun showHideSplash(show: Boolean) {
        val handler = Handler()
        val timeoutMillis = 8000L // 8 seconds

        if (show) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, SplashFragment())
                .commit()
        } else {
            handler.removeCallbacksAndMessages(null)
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .remove(supportFragmentManager.findFragmentById(R.id.fragmentContainerView)!!)
                .commit()
        }

        handler.postDelayed({
            val splashFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as? SplashFragment
            if (splashFragment != null && splashFragment.isVisible) {

                showAlertDialog(getString(R.string.timeoutTitle), getString(R.string.timeOut), true)
            }
        }, timeoutMillis)
    }

    private fun setDataWeatherClick() {
        with(binding) {
            timeCard1.setOnClickListener { showSnackbar(weatherTexts[0]) }
            timeCard2.setOnClickListener { showSnackbar(weatherTexts[1]) }
            timeCard3.setOnClickListener { showSnackbar(weatherTexts[2]) }
            timeCard4.setOnClickListener { showSnackbar(weatherTexts[3]) }
        }
    }

    private fun showAlertDialog(title: String, message: String, exitApp: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(getString(R.string.accept)) { dialog, _ ->
            if (exitApp)
                finish()
            else
                dialog.cancel()
        }
        builder.show()
    }

    private fun showSnackbar(text: String?) {
        Snackbar.make(binding.root, text!!, Snackbar.LENGTH_SHORT).show()
    }

    private fun checkLocationPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun requestLocation(): List<String> {
        // Crear una lista mutable de cadenas
        val locationData = mutableListOf<String>()

        try {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude

                locationData.add(latitude.toString())
                locationData.add(longitude.toString())
            }
        } catch (ex: SecurityException) {
            ex.printStackTrace()
        }
        return locationData
    }


    private fun getCityName(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses!!.isNotEmpty()) {
                val cityName = addresses!![0].locality
                return cityName ?: "Ciudad no encontrada"
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "Ciudad no encontrada"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, obtener la ubicación
                requestLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun removeAccents(input: String): String {
        val normalizedString = Normalizer.normalize(input, Normalizer.Form.NFD)
        val pattern = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        return pattern.replace(normalizedString, "")
    }
}
