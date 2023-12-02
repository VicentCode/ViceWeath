package com.vicentcode.viceweath

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.vicentcode.viceweath.databinding.ActivityMainBinding
import org.json.JSONObject
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestQueue = Volley.newRequestQueue(this)

        url = "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=24.446693,-104.122912&days=7&lang=en"

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
    }


    private fun getNewLocation(context: Context, callback: (String) -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.enter_a_location))

        val input = EditText(context)
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.accept)) { dialog, _ ->
            val newLocation = input.text.toString()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, SplashFragment())
                .commit()
            val url =
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
                                ("icon")
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

                        Picasso.get().load("https:${hour.getJSONObject("condition").getString("icon")}").into(imgTime)

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
}
