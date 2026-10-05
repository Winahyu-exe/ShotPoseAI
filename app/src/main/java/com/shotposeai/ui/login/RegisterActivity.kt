package com.shotposeai.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.PasswordHasher
import com.shotposeai.data.SessionStore
import com.shotposeai.data.UserEntity
import com.shotposeai.databinding.ActivityRegisterBinding
import com.shotposeai.ui.home.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Lengkapi nama, email, dan password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnRegister.isEnabled = false
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val user = UserEntity(name, email, PasswordHasher.hash(password), "TEACHER", "", "", System.currentTimeMillis())
                        user.id = DatabaseProvider.get(this@RegisterActivity).appDao().insertUser(user)
                        user
                    }
                }
                binding.btnRegister.isEnabled = true
                result.onSuccess { user ->
                    SessionStore(this@RegisterActivity).save(user, true)
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finishAffinity()
                }.onFailure {
                    Toast.makeText(this@RegisterActivity, "Email sudah digunakan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvGoLogin.setOnClickListener {
            finish()
        }
    }
}
