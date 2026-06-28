package com.aliadas.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.aliadas.R
import com.aliadas.contacts.AlertManager
import com.aliadas.databinding.FragmentHomeBinding
import com.aliadas.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: MapView
    private var locationOverlay: MyLocationNewOverlay? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inicializar OSMDroid con contexto (requerido)
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = "AliadadApp/1.0"

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val name = SessionManager.getUserName(requireContext()) ?: "amiga"
        binding.tvWelcome.text = "Hola, $name 💜"

        setupMap()

        binding.btnPanic.setOnClickListener { showPanicConfirm() }
    }

    private fun setupMap() {
        map = binding.osmMap

        // Configuración básica del mapa
        map.setTileSource(TileSourceFactory.MAPNIK) // tiles de OpenStreetMap
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        // Centro inicial en Ciudad de México como fallback
        map.controller.setCenter(GeoPoint(19.4326, -99.1332))

        // Overlay de ubicación actual (punto azul)
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(requireContext()), map
            ).apply {
                enableMyLocation()
                enableFollowLocation()
                runOnFirstFix {
                    requireActivity().runOnUiThread {
                        map.controller.setCenter(myLocation)
                        map.controller.setZoom(16.0)
                    }
                }
            }
            map.overlays.add(locationOverlay)
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupMap()
        }
    }

    private fun showPanicConfirm() {
        AlertDialog.Builder(requireContext())
            .setTitle("🚨 Botón de pánico")
            .setMessage("¿Confirmas que necesitas ayuda? Se enviará tu ubicación a tus contactos de confianza.")
            .setPositiveButton("Sí, necesito ayuda") { _, _ -> triggerPanic() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun triggerPanic() {
        Toast.makeText(requireContext(), "🚨 Alerta enviada a tus contactos", Toast.LENGTH_LONG).show()
        AlertManager.sendPanicAlert(requireContext())
    }

    // Ciclo de vida del mapa OSMDroid (obligatorio)
    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
        locationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        if (::map.isInitialized) map.onPause()
        locationOverlay?.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationOverlay?.disableMyLocation()
        _binding = null
    }
}
