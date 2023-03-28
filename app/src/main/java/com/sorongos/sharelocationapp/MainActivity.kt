package com.sorongos.sharelocationapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.kakao.sdk.common.util.Utility
import com.sorongos.sharelocationapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /**default debug keyHash*/
        var keyHash = Utility.getKeyHash(this)
        println(keyHash)
        Log.e("keyhash",keyHash.toString())

        startActivity(Intent(this, LoginActivity::class.java))
    }
}