package com.sorongos.sharelocationapp

import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.sorongos.sharelocationapp.databinding.ActivityEmailLoginBinding

class EmailLoginActivity: AppCompatActivity() {
    private lateinit var binding: ActivityEmailLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkId()

        binding.doneButton.setOnClickListener {

        }

    }

    private fun checkId() {
        binding.emailEditText.addTextChangedListener {
            val email = binding.emailEditText.text.toString()
            val pattern = Patterns.EMAIL_ADDRESS
            binding.emailTextInputLayout.error =
                if (pattern.matcher(email).matches()) null
                else if(binding.emailEditText.text?.length == 0) "8자 이상 입력해주세요"
                else "이메일 주소 형식을 입력해주세요."
        }
    }
}
