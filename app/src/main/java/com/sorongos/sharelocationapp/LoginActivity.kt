package com.sorongos.sharelocationapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.sorongos.sharelocationapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            //login failed
        } else if (token != null) {
            //login success
            Log.e("loginActivity", "login in with kakao acount token == $token")
        }
    }

    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                        Log.e("loginActivity", "token == $token")
                    }
                }

            } else {
                //kakaAcount login
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)

            }

        }
    }
}