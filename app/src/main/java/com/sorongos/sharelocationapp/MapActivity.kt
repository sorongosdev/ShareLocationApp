package com.sorongos.sharelocationapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.RoundedCorner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.sorongos.sharelocationapp.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var trackingPersonId: String = ""

    private val markerMap = hashMapOf<String, Marker>()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                //fine location권한이 있음
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                //coarse location권한이 있음
                getCurrentLocation()
            }
            else -> {
                //TODO 설정으로 보내기, 교육용 팝업 띄워 다시 권한 요청
            }
        }
    }

    //callback 자체가 인터페이스이기 때문에, 오브젝트로 구현해줌
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            //New requested location info
            for (location in locationResult.locations) {
                Log.e(
                    "MapAcitivity", "onLocationResult : ${location.latitude} ${location.longitude}"
                )

                val uid = Firebase.auth.currentUser?.uid.orEmpty()

                val locationMap = mutableMapOf<String, Any>()
                locationMap["latitude"] = location.latitude
                locationMap["longitude"] = location.longitude
                Firebase.database.reference.child("Person").child(uid).updateChildren(locationMap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //callback이 구현된 mapActivity로 넘겨줌

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        requestLocationPermission()
        setUpEmojiAnimationView()
        setUpCurrentLocationView()
        setupFirebaseDatabase()

//        /**default debug keyHash*/
//        var keyHash = Utility.getKeyHash(this)
//        println(keyHash)
//        Log.e("keyhash", keyHash.toString())

    }

    private fun setUpCurrentLocationView() {
        //floating button을 누르면 마지막 위치가 업데이트
        binding.currentLocationButton.setOnClickListener {
            //내 위치로 갈 때는 트래킹 모드 중지
            trackingPersonId = ""
            moveLastLocation()
        }
    }

    /**리액션 이모지 셋업*/
    private fun setUpEmojiAnimationView() {
        binding.emojiLottieAnimationView.setOnClickListener {
            if (trackingPersonId != "") { // 아무도 없으면 실행 x
                val lastEmoji = mutableMapOf<String, Any>()
                lastEmoji["type"] = "star"
                lastEmoji["lastModifier"] = System.currentTimeMillis()
                Firebase.database.reference.child("Emoji").child(trackingPersonId)
                    .updateChildren(lastEmoji)
            }

            binding.emojiLottieAnimationView.playAnimation()
            binding.dummyLottieAnimationView.animate()
                .scaleX(3f)
                .scaleY(3f)
                .alpha(0f)
                .withStartAction {
                    binding.dummyLottieAnimationView.apply {
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                    }
                }.withEndAction {
                    binding.dummyLottieAnimationView.apply {
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                    }
                }.start()
        }

        binding.emojiLottieAnimationView.speed = 3f
        binding.centerLottieAnimationView.speed = 3f
    }

    /**앱 실행 중일 때 위치를 게속해서 받아옴*/
    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }

    /**Background로 나가게 되면 위치를 업데이트하지 않음, 작동 중지*/
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun getCurrentLocation() {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 1000).build()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        //권한 있음
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )

        moveLastLocation()
    }

    private fun moveLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener {
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16.0f)
            )
        }
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun setupFirebaseDatabase() {
        Firebase.database.reference.child("Person")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val person = snapshot.getValue(Person::class.java) ?: return
                    val uid = person.uid ?: return

                    //해당 uid의 마커가 null일 때만 마커맵에 마커를 추가
                    if (markerMap[uid] == null) {
                        markerMap[uid] = makeNewMarker(person, uid) ?: return // if null
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val person = snapshot.getValue(Person::class.java) ?: return
                    val uid = person.uid ?: return

                    //해당 uid의 마커가 null일 때만 마커맵에 마커를 추가
                    if (markerMap[uid] == null) {
                        markerMap[uid] = makeNewMarker(person, uid) ?: return // if null
                    } else {
                        markerMap[uid]?.position =
                            LatLng(person.latitude ?: 0.0, person.longitude ?: 0.0)
                    }

                    /**업데이트 되고 있는 아이디와 추적하고자 하는 아이디 */
                    if (uid == trackingPersonId) {
                        googleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(LatLng(person.latitude ?: 0.0, person.longitude ?: 0.0))
                                    .zoom(16.0f)
                                    .build()
                            )
                        )
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    TODO("Not yet implemented")
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    TODO("Not yet implemented")
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        Firebase.database.reference.child("Emoji").child(Firebase.auth.currentUser?.uid ?: "")
            .addChildEventListener(object : ChildEventListener { //하나가 바뀌면
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    //누를 때마다, 파이어베이스의 Emoji child가 변화 -> 애니메이션 재생
                    binding.centerLottieAnimationView.playAnimation()
                    binding.centerLottieAnimationView.animate()
                        .scaleX(7f)
                        .scaleY(7f)
                        .alpha(0.3f)
                        .setDuration(binding.centerLottieAnimationView.duration / 3)
                        .withEndAction {
                            binding.centerLottieAnimationView.scaleX = 0f
                            binding.centerLottieAnimationView.scaleY = 0f
                            binding.centerLottieAnimationView.alpha = 1f
                        }
                }

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }

    private fun makeNewMarker(person: Person, uid: String): Marker? {
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(person.latitude ?: 0.0, person.longitude ?: 0.0))
                .title(person.name.orEmpty())
        ) ?: return null

        marker.tag = uid

        Glide.with(this).asBitmap()
            .load(person.profilePhoto)
            //이미지 모서리 깎기
            .transform(RoundedCorners(60))
            .override(200)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    //true -> taget에서 호출 안됨
                    //false -> target에서 호출됨
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.let {
                        runOnUiThread {
                            marker.setIcon(
                                BitmapDescriptorFactory.fromBitmap(
                                    resource
                                )
                            )
                        }
                    }
                    return true
                }
            }).submit()

        //마커에 그림
//        marker.setIcon()

        return marker
    }

    override fun onMapReady(map: GoogleMap) {
        //map init
        googleMap = map
        //zoom 했을 때 최대 줌 레벨
        googleMap.setMaxZoomPreference(20.0f)
        googleMap.setMinZoomPreference(10.0f)

        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener {
            trackingPersonId = "" // tracking하다가 map을 클릭했을 때 트래킹을 멈춤
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {

        //marker 누르면 uid 업데이트
        trackingPersonId = marker.tag as? String ?: ""

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.emojiBottomSheetLayout)
        //마커를 누르면 바텀시트가 올라옴
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        return false
    }
}