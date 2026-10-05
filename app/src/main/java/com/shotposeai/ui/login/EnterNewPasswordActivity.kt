package com.shotposeai.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.PasswordHasher
import com.shotposeai.databinding.ActivityEnterNewPasswordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnterNewPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnterNewPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnterNewPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnContinue.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Konfirmasi password belum sama", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = intent.getStringExtra(ForgotPasswordActivity.EXTRA_EMAIL).orEmpty()
            lifecycleScope.launch {
                val updated = withContext(Dispatchers.IO) {
                    val dao = DatabaseProvider.get(this@EnterNewPasswordActivity).appDao()
                    val user = dao.findUserByEmail(email) ?: return@withContext false
                    user.passwordHash = PasswordHasher.hash(newPassword)
                    dao.updateUser(user) > 0
                }
                if (!updated) {
                    Toast.makeText(this@EnterNewPasswordActivity, "Akun tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Toast.makeText(this@EnterNewPasswordActivity, "Password berhasil diperbarui", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@EnterNewPasswordActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
            }
        }
    }
}
