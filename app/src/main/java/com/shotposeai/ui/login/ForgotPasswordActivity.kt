package com.shotposeai.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Isi email dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val exists = withContext(Dispatchers.IO) { DatabaseProvider.get(this@ForgotPasswordActivity).appDao().findUserByEmail(email) != null }
                if (!exists) {
                    Toast.makeText(this@ForgotPasswordActivity, "Akun guru tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Toast.makeText(this@ForgotPasswordActivity, "Mode lokal: kode OTP 2468", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@ForgotPasswordActivity, OtpVerificationActivity::class.java).apply { putExtra(EXTRA_EMAIL, email) })
            }
        }
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
}
