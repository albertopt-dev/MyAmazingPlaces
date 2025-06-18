package edu.albertoperez.myamazingplaces1

import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import edu.albertoperez.myamazingplaces1.databinding.ActivityMaps2Binding

/** Esta clase muestra un mapa con la ubicacion predeterminada o la del lugar que se introduzca */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var mMap: GoogleMap // Referencia al mapa
    private lateinit var binding: ActivityMaps2Binding // Enlace con el layout
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient // Cliente de ubicación de Google
    private var lastlocation: Location? = null  //Ultima ubicacion conocida

    // Configuración del mapa y gestion de permisos y ubicación
    private fun configMap(latitud: Double, longitud: Double, nombreLugar: String?) {
        // Verifica si el permiso de ubicación está concedido
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no está concedido, lo solicita al usuario
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Habilitamos la ubicación en el mapa
        mMap.isMyLocationEnabled = true

        // Si hay coordenadas válidas, se muestra el lugar en el mapa
        if (latitud != 0.0 && longitud != 0.0) {
            val lugarLatLng = LatLng(latitud, longitud)
            mMap.addMarker(MarkerOptions().position(lugarLatLng).title(nombreLugar))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lugarLatLng, 15f))
        } else {
            // Obtenemos la última ubicación conocida si no se envían coordenadas
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    lastlocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                } else {
                    // Ubicación por defecto (Alcoy) si no hay última ubicación ni coordenadas válidas
                    val alcoy = LatLng(38.705521, -0.474348)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(alcoy, 10f))
                    Toast.makeText(
                        this,
                        "No se pudieron obtener las coordenadas, mostrando Alcoy",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMaps2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtenemos el fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializamos el cliente de ubicación
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configuraciones del mapa
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabled = true
        mMap.uiSettings.isRotateGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true

        // Recupera las coordenadas del Intent
        val intent = intent
        val latitud = intent.getDoubleExtra("latitud", 0.0)
        val longitud = intent.getDoubleExtra("longitud", 0.0)
        val nombreLugar = intent.getStringExtra("nombre") ?: "Ubicación"

        // Configuramos el mapa y mostramos el lugar especificado
        configMap(latitud, longitud, nombreLugar)
    }
}
