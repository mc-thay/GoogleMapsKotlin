package com.aguilar.googlemapskt

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import com.google.maps.android.PolyUtil
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: LatLng
    private var hasMarkedCurrentLocation = false
    private var hasMarkedSelectedPlace = false
    private lateinit var selectedPlace: LatLng
    private lateinit var btnSearch: Button
    private lateinit var btnRoute: Button
    private lateinit var btnMapType: Button
    private lateinit var btnCurrentLocation: Button
    private lateinit var editTextPlace: EditText
    private var currentMapType = GoogleMap.MAP_TYPE_NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Places SDK and FusedLocationProvider
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get map fragment and initialize it
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up buttons and EditText
        editTextPlace = findViewById(R.id.editTextPlace)
        btnSearch = findViewById(R.id.btnSearch)
        btnRoute = findViewById(R.id.btnRoute)
        btnMapType = findViewById(R.id.btnMapType)
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)

        btnSearch.setOnClickListener {
            searchPlace()
        }

        btnRoute.setOnClickListener {
            traceRoute()
        }

        btnMapType.setOnClickListener {
            changeMapType()
        }

        btnCurrentLocation.setOnClickListener {
            markCurrentLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()

        // Ubicación fija de Senati, Nuevo Chimbote
        val senatiLocation = LatLng(-9.116824481116799, -78.51622141673337)
        mMap.addMarker(MarkerOptions().position(senatiLocation).title("Senati Nuevo Chimbote"))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(senatiLocation, 15f))

        // Guardamos la ubicación fija como selectedPlace
        selectedPlace = senatiLocation
        hasMarkedSelectedPlace = true
    }

    private fun traceRoute() {
        if (hasMarkedCurrentLocation && hasMarkedSelectedPlace) {
            val context = GeoApiContext.Builder().apiKey(getString(R.string.google_maps_key)).build()

            thread {
                try {
                    val result: DirectionsResult = DirectionsApi.newRequest(context)
                        .origin(com.google.maps.model.LatLng(currentLocation.latitude, currentLocation.longitude))
                        .destination(com.google.maps.model.LatLng(selectedPlace.latitude, selectedPlace.longitude))
                        .mode(TravelMode.DRIVING)
                        .await()

                    runOnUiThread {
                        if (result.routes.isNotEmpty()) {
                            val decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.encodedPath)
                            mMap.addPolyline(com.google.android.gms.maps.model.PolylineOptions().addAll(decodedPath))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(this, "Ubicación actual o lugar no marcado", Toast.LENGTH_SHORT).show()
        }
    }


    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun markCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    if (!hasMarkedCurrentLocation) {
                        mMap.addMarker(MarkerOptions().position(currentLocation).title("Mi ubicación actual"))
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                        hasMarkedCurrentLocation = true
                    } else {
                        Toast.makeText(this, "Ubicación actual ya está marcada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun searchPlace() {
        val placeName = editTextPlace.text.toString()
        if (placeName.isNotEmpty()) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(placeName)
                .setTypeFilter(TypeFilter.ADDRESS) // o TypeFilter.GEOCODE
                .setLocationBias(
                    RectangularBounds.newInstance(
                        LatLng(-5.0, -81.0), // Limites aproximados de Perú
                        LatLng(-0.0, -68.0)
                    ))
                .build()

            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions
                if (predictions.isNotEmpty()) {
                    showPlaceSuggestions(predictions)
                } else {
                    Toast.makeText(this, "No se encontraron lugares", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Error en la búsqueda: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Por favor, ingrese un lugar para buscar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPlaceSuggestions(predictions: List<AutocompletePrediction>) {
        // Mostrar sugerencias en un AlertDialog o en un RecyclerView
        val placeNames = predictions.map { it.getPrimaryText(null).toString() }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sugerencias")
        builder.setItems(placeNames.toTypedArray()) { _, which ->
            val selectedPrediction = predictions[which]
            getPlaceDetails(selectedPrediction.placeId)
        }
        builder.show()
    }


    private fun getPlaceDetails(placeId: String) {
        val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG)).build()
        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            selectedPlace = place.latLng ?: return@addOnSuccessListener
            mMap.addMarker(MarkerOptions().position(selectedPlace).title(place.name))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace, 15f))
            hasMarkedSelectedPlace = true
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error al obtener detalles del lugar: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun changeMapType() {
        currentMapType = (currentMapType + 1) % 4
        when (currentMapType) {
            0 -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            1 -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            2 -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            3 -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        }
        Toast.makeText(this, "Tipo de mapa cambiado", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}
