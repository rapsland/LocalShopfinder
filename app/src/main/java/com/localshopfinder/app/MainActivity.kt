package com.localshopfinder.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnGps: Button
    private lateinit var spinnerCity: Spinner
    private lateinit var seekBarRadius: SeekBar
    private lateinit var tvRadiusLabel: TextView
    private lateinit var btnScan: Button
    private lateinit var tvStatus: TextView
    private lateinit var listViewStores: ListView

    private var radiusKm: Float = 2.0f

    private val cities = listOf(
        CityLocation("📍 Echter GPS Standort", 0.0, 0.0),
        CityLocation("Paris (Frankreich)", 48.8566, 2.3522),
        CityLocation("Madrid (Spanien)", 40.4168, -3.7038),
        CityLocation("Berlin (Deutschland)", 52.5200, 13.4050),
        CityLocation("İzmir (Türkei)", 38.4192, 27.1287),
        CityLocation("Rom (Italien)", 41.9028, 12.4964),
        CityLocation("Çeşme (Türkei)", 38.3236, 26.3056),
        CityLocation("Chicago (USA)", 41.8781, -87.6298),
        CityLocation("Liverpool (UK)", 53.4084, -2.9916)
    )

    private var selectedLat: Double = 0.0
    private var selectedLon: Double = 0.0
    private var isGpsMode: Boolean = true

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnGps = findViewById(R.id.btnGps)
        spinnerCity = findViewById(R.id.spinnerCity)
        seekBarRadius = findViewById(R.id.seekBarRadius)
        tvRadiusLabel = findViewById(R.id.tvRadiusLabel)
        btnScan = findViewById(R.id.btnScan)
        tvStatus = findViewById(R.id.tvStatus)
        listViewStores = findViewById(R.id.listViewStores)

        seekBarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // progress 1 to 50 representing 0.1km to 5.0km
                radiusKm = (progress.coerceAtLeast(1)) / 10.0f
                tvRadiusLabel.text = "Suchkreis: %.1f km".format(radiusKm)
                btnScan.text = "Supermärkte im Umkreis (%.1f km) scannen".format(radiusKm)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cities.map { it.name })
        spinnerCity.adapter = adapter

        spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    isGpsMode = true
                    tvStatus.text = "Modus: Echter GPS Standort aktiv"
                    fetchGpsLocation()
                } else {
                    isGpsMode = false
                    val city = cities[position]
                    selectedLat = city.lat
                    selectedLon = city.lon
                    tvStatus.text = "Modus: Override aktiv (${city.name})"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnGps.setOnClickListener {
            spinnerCity.setSelection(0)
            isGpsMode = true
            fetchGpsLocation()
        }

        btnScan.setOnClickListener {
            scanNearbyStores()
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            fetchGpsLocation()
        }
    }

    private fun fetchGpsLocation() {
        if (!isGpsMode) return
        tvStatus.text = "Ermittle GPS Standort..."
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        selectedLat = loc.latitude
                        selectedLon = loc.longitude
                        tvStatus.text = "GPS OK: %.4f, %.4f".format(selectedLat, selectedLon)
                    } else {
                        tvStatus.text = "GPS Signal schwach, verwende Fallback."
                        selectedLat = 52.5200
                        selectedLon = 13.4050
                    }
                }
                .addOnFailureListener {
                    tvStatus.text = "GPS Fehler, verwende Fallback (Berlin)."
                    selectedLat = 52.5200
                    selectedLon = 13.4050
                }
        }
    }

    private fun scanNearbyStores() {
        val radiusMeters = (radiusKm * 1000).toInt()
        tvStatus.text = "Scanne Supermärkte im Umkreis von %.1f km...".format(radiusKm)
        scope.launch(Dispatchers.IO) {
            val latDelta = radiusMeters / 111320.0
            val lonDelta = radiusMeters / (111320.0 * kotlin.math.cos(Math.toRadians(selectedLat)).coerceAtLeast(0.01))
            val south = selectedLat - latDelta
            val north = selectedLat + latDelta
            val west = selectedLon - lonDelta
            val east = selectedLon + lonDelta
            val bbox = String.format(Locale.ROOT, "%.6f,%.6f,%.6f,%.6f", south, west, north, east)

            val retailerRegex = "REWE|Netto|Lidl|ALDI|Aldi|PENNY|Kaufland|EDEKA|Marktkauf|Norma|Combi|famila|Nahkauf|Tegut|Migros|Carrefour|BİM|Bim|Şok|A101|Dia|Eroski|Mercadona|Supercor|Consum|El Corte Inglés|Auchan|Intermarché|Leader Price|Monoprix|Franprix|Casino|Super U|U Express|Hyper U|Sainsbury's|Tesco|Asda|Morrisons|Waitrose|Co-op|Aldi UK|Lidl GB|Trader Joe's|Whole Foods|Kroger|Safeway|Publix|Wegmans|Target|Walmart|Migros Ticaret|A101 Yeni Mağazacılık|Şok Marketler|Hakmar|Tarım Kredi|Macrocenter|Şok|Migros Jet|Tansas|Tansaş|Supermarket|Supermercato|Supermercado|Grocery|Groceries|Epicerie|Epiceria|Bakkal|Market"
            val query = "[out:json][timeout:10];(" +
                    "nwr[\"shop\"~\"^(supermarket|discount_store|convenience|department_store|grocery|greengrocer|general)$\"]($bbox);" +
                    "nwr[\"brand\"~\"$retailerRegex\",i]($bbox);" +
                    "nwr[\"name\"~\"$retailerRegex\",i]($bbox);" +
                    "nwr[\"store\"~\"supermarket\",i]($bbox);" +
                    ");out center tags;"

            val body = "data=" + URLEncoder.encode(query, "UTF-8")
            val endpoints = listOf(
                "https://overpass-api.de/api/interpreter",
                "https://lz4.overpass-api.de/api/interpreter"
            )

            val deferredOverpass = endpoints.map { endpoint ->
                scope.async(Dispatchers.IO) {
                    val foundStores = mutableListOf<ShopItem>()
                    try {
                        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            doOutput = true
                            connectTimeout = 3000
                            readTimeout = 5000
                            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                            setRequestProperty("User-Agent", "LocalShopfinder/1.1 (Android)")
                        }
                        conn.outputStream.buffered().use { it.write(body.toByteArray()) }
                        if (conn.responseCode == 200) {
                            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                            val root = JSONObject(jsonStr)
                            val elements = root.optJSONArray("elements")
                            if (elements != null) {
                                for (i in 0 until elements.length()) {
                                    val el = elements.optJSONObject(i) ?: continue
                                    val tags = el.optJSONObject("tags") ?: continue
                                    val name = tags.optString("name").takeIf { it.isNotBlank() }
                                        ?: tags.optString("brand").takeIf { it.isNotBlank() }
                                        ?: tags.optString("operator").takeIf { it.isNotBlank() }
                                        ?: "Supermarket"
                                    val brand = tags.optString("brand").takeIf { it.isNotBlank() && it != "null" } ?: name
                                    val center = el.optJSONObject("center")
                                    val eLat = center?.optDouble("lat", 0.0) ?: el.optDouble("lat", 0.0)
                                    val eLon = center?.optDouble("lon", 0.0) ?: el.optDouble("lon", 0.0)
                                    if (eLat != 0.0 && eLon != 0.0) {
                                        val results = FloatArray(1)
                                        Location.distanceBetween(selectedLat, selectedLon, eLat, eLon, results)
                                        val dist = results[0]
                                        if (dist <= radiusMeters) {
                                            foundStores.add(ShopItem(name, brand, eLat, eLon, dist))
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                    foundStores
                }
            }

            val deferredPhoton = scope.async(Dispatchers.IO) {
                val foundStores = mutableListOf<ShopItem>()
                val genericTerms = listOf("supermarket", "grocery")
                for (term in genericTerms) {
                    try {
                        val url = URL("https://photon.komoot.io/api/?q=${URLEncoder.encode(term, "UTF-8")}&lat=$selectedLat&lon=$selectedLon&limit=15")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 2000
                            readTimeout = 3000
                            setRequestProperty("User-Agent", "LocalShopfinder/1.1")
                        }
                        if (conn.responseCode == 200) {
                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                            val features = JSONObject(text).optJSONArray("features")
                            if (features != null) {
                                for (i in 0 until features.length()) {
                                    val f = features.optJSONObject(i) ?: continue
                                    val props = f.optJSONObject("properties") ?: continue
                                    val geom = f.optJSONObject("geometry") ?: continue
                                    val coords = geom.optJSONArray("coordinates") ?: continue
                                    val sLon = coords.optDouble(0, 0.0)
                                    val sLat = coords.optDouble(1, 0.0)
                                    if (sLat != 0.0 && sLon != 0.0) {
                                        val results = FloatArray(1)
                                        Location.distanceBetween(selectedLat, selectedLon, sLat, sLon, results)
                                        val dist = results[0]
                                        if (dist <= radiusMeters) {
                                            val name = props.optString("name").takeIf { it.isNotBlank() } ?: "Supermarkt"
                                            foundStores.add(ShopItem(name, "Supermarkt", sLat, sLon, dist))
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                foundStores
            }

            val foundStores = (deferredOverpass.awaitAll().flatten() + deferredPhoton.await()).distinctBy { "${it.name}_${it.lat}_${it.lon}" }

            withContext(Dispatchers.Main) {
                if (sortedStores.isEmpty()) {
                    tvStatus.text = "Keine Supermärkte im %.1f km Umkreis gefunden.".format(radiusKm)
                    listViewStores.adapter = null
                } else {
                    tvStatus.text = "${sortedStores.size} Supermärkte im Umkreis von %.1f km gefunden:".format(radiusKm)
                    val displayList = sortedStores.map {
                        if (it.distanceMeters >= 1000) {
                            "${it.name}\nEntfernung: %.2f km".format(it.distanceMeters / 1000.0)
                        } else {
                            "${it.name}\nEntfernung: %.0f Meter".format(it.distanceMeters)
                        }
                    }
                    val listAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, displayList)
                    listViewStores.adapter = listAdapter
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
