package com.sorongos.sharelocationapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.kakao.sdk.common.util.Utility
import com.sorongos.sharelocationapp.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //callback 자체가 인터페이스이기 때문에, 오브젝트로 구현해줌
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            //New requested location info
            for (location in locationResult.locations){
                location.latitude
                location.longitude
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as
                SupportMapFragment
        mapFragment.getMapAsync(this)
        //callback이 구현된 mapActivity로 넘겨줌

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.requestLocationUpdates(locationReqeust, locationCallback, Looper)

        /**default debug keyHash*/
        var keyHash = Utility.getKeyHash(this)
        println(keyHash)
        Log.e("keyhash", keyHash.toString())

//        startActivity(Intent(this, LoginActivity::class.java))
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val sydney = LatLng(-34.0, 151.0)
        googleMap.addMarker(
            MarkerOptions()
                .position(sydney)
                .title("Marker in Sydney")
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}