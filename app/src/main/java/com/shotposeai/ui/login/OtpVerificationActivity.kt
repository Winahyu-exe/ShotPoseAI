package com.shotposeai.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shotposeai.databinding.ActivityOtpVerificationBinding

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOtpAutoMove()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnContinue.setOnClickListener {
            val code = listOf(binding.otp1, binding.otp2, binding.otp3, binding.otp4)
                .joinToString(separator = "") { it.text.toString().trim() }

            if (code != "2468") {
                Toast.makeText(this, "Kode OTP tidak sesuai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, EnterNewPasswordActivity::class.java).apply {
                putExtra(ForgotPasswordActivity.EXTRA_EMAIL, intent.getStringExtra(ForgotPasswordActivity.EXTRA_EMAIL))
            })
        }
    }

    private fun setupOtpAutoMove() {
        val fields = listOf(binding.otp1, binding.otp2, binding.otp3, binding.otp4)
        fields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < fields.lastIndex) {
                        fields[index + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        fields.first().requestFocus()
    }
}
