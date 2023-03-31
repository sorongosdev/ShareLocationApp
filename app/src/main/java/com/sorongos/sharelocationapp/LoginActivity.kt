package com.sorongos.sharelocationapp

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import com.sorongos.sharelocationapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var emailLoginResult: ActivityResultLauncher<Intent>
    private lateinit var pendingUser: User // 잠시 저장
    private val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            //login failed
            showErrorToast()
            error.printStackTrace()

        } else if (token != null) {
            //login success
            getKakaoAccountInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        KakaoSdk.init(this, "ccd5a4eacd32c16e3eb23d1d177ebfec")

        if(AuthApiClient.instance.hasToken()){
            //토큰 있는지 확인하고 자동 로그인
            UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
                if(error == null){
                    getKakaoAccountInfo() //
                }
            }
        }

        emailLoginResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val email = it.data?.getStringExtra("email")

                    if (email == null) {
                        showErrorToast()
                        return@registerForActivityResult
                    } else {
                        signInFirebase(pendingUser, email)
                    }
                }
            }

        binding.kakaoTalkLoginButton.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                //kakaotalk login
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    //login failed
                    if (error != null) {
                        //의도적 실패
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    } else if (token != null) {
                        //로그인이 됐다면 파이어베이스에 로그인 되어 있는지 다시 한번 확인
                        if (Firebase.auth.currentUser == null) {
                            //카카오톡에서 정보를 가져와 파이어베이스로 로그인
                            getKakaoAccountInfo()
                        } else {
                            //Firebase 정보가 있다면
                            navigateToMapActivity()
                        }
                        Log.e("loginActivity", "token == $token")
                    }
                }

            } else {
                //kakaAcount login, 카카오톡 로그인 불가, 카카오계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)

            }

        }
    }

    /**카카오 계정 정보를 가져옴*/
    private fun getKakaoAccountInfo() {
        UserApiClient.instance.me { user, error ->
            //Exception
            if (error != null) {
                showErrorToast()
            } else if (user != null) {
                // 사용자 정보 요청 성공
                Log.e(
                    "LoginActivity",
                    "회원번호 ${user.id} / email ${user.kakaoAccount?.email} " +
                            "/ nickname ${user.kakaoAccount?.profile?.nickname} " +
                            "/ profile photo ${user.kakaoAccount?.profile?.thumbnailImageUrl}"
                )

                checkKakaoUserData(user)
            }
        }
    }

    private fun showErrorToast() {
        Toast.makeText(this, "사용자 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
    }

    /**Check Again*/
    private fun checkKakaoUserData(user: User) {
        val kakaoEmail = user.kakaoAccount?.email.orEmpty()
        if (kakaoEmail.isEmpty()) {
            //추가 이메일을 받아야함
            pendingUser = user
            emailLoginResult.launch(Intent(this,EmailLoginActivity::class.java))
            return
        }
        //User data를 넘김
        signInFirebase(user, kakaoEmail)
    }

    private fun signInFirebase(user: User, email: String) {
        val uid = user.id.toString()
        Firebase.auth.createUserWithEmailAndPassword(email, uid)
            .addOnCompleteListener {
                //로그인 성공
                if (it.isSuccessful) {
                    //Next process
                    updateFirebaseDatabase(user)
                }
            }
            .addOnFailureListener {
                //이미 가입된 계정
                if (it is FirebaseAuthUserCollisionException) {
                    Firebase.auth.signInWithEmailAndPassword(email, uid).addOnCompleteListener {
                        if (it.isSuccessful) {
                            //Next Process
                            updateFirebaseDatabase(user)
                        } else {
                            showErrorToast()
                        }
                    }.addOnFailureListener { error ->
                        error.printStackTrace()
                        showErrorToast()
                    }
                } else {
                    showErrorToast()
                }
            }
    }

    private fun updateFirebaseDatabase(user: User) {
        val uid = Firebase.auth.currentUser?.uid.orEmpty()
        val personMap = mutableMapOf<String, Any>()
        personMap["uid"] = uid
        personMap["name"] = user.kakaoAccount?.profile?.nickname.orEmpty()
        personMap["profilePhoto"] = user.kakaoAccount?.profile?.thumbnailImageUrl.orEmpty()

        Firebase.database.reference.child("Person").child(uid).updateChildren(personMap)

        navigateToMapActivity()
    }

    private fun navigateToMapActivity() {
        startActivity(Intent(this, MapActivity::class.java))
    }
}