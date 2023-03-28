package com.sorongos.sharelocationapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.kakao.sdk.common.util.Utility
import com.sorongos.sharelocationapp.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /**default debug keyHash*/
        var keyHash = Utility.getKeyHash(this)
        println(keyHash)
        Log.e("keyhash",keyHash.toString())

        startActivity(Intent(this, LoginActivity::class.java))
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }
}