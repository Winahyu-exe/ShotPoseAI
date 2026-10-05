package com.shotposeai.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.PasswordHasher
import com.shotposeai.data.SessionStore
import com.shotposeai.databinding.ActivityLoginBinding
import com.shotposeai.ui.home.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Isi email guru terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Isi password terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO) { DatabaseProvider.get(this@LoginActivity).appDao().findUserByEmail(email) }
                binding.btnLogin.isEnabled = true
                if (user == null || !PasswordHasher.verify(password, user.passwordHash)) {
                    Toast.makeText(this@LoginActivity, "Email atau password tidak sesuai", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                SessionStore(this@LoginActivity).save(user, binding.cbRemember.isChecked)
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finishAffinity()
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
