package com.shotposeai.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.shotposeai.ui.common.AvatarStore
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.SessionStore
import com.shotposeai.databinding.ActivityProfileBinding
import com.shotposeai.ui.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            binding.ivPhoto.isEnabled = false
            binding.btnChangePhoto.isEnabled = false
            try {
                val session = SessionStore(this@ProfileActivity).current() ?: error("Sesi tidak tersedia")
                val photo = AvatarStore.import(this@ProfileActivity, uri)
                withContext(Dispatchers.IO) {
                    val dao = DatabaseProvider.get(this@ProfileActivity).appDao()
                    val user = dao.getUser(session.userId) ?: error("Profil tidak tersedia")
                    user.profilePhotoPath = photo
                    check(dao.updateUser(user) == 1)
                }
                AvatarStore.bind(binding.ivPhoto, photo, lifecycleScope)
                Toast.makeText(this@ProfileActivity, "Foto profil tersimpan", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Foto belum tersimpan. Silakan coba lagi.", Toast.LENGTH_LONG).show()
            } finally {
                binding.ivPhoto.isEnabled = true
                binding.btnChangePhoto.isEnabled = true
            }
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivPhoto.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        binding.btnChangePhoto.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, AccountSettingsActivity::class.java)) }
        binding.btnHelp.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Bantuan dan Dukungan")
                .setMessage("Analisis ShotPose AI bekerja offline. Preview teknik YouTube membutuhkan internet. Pastikan seluruh tubuh siswa terlihat, pencahayaan cukup, dan kamera stabil.\n\nVersi 1.0 • Data tersimpan hanya di perangkat.")
                .setPositiveButton("OK", null)
                .show()
        }
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Logout") { _, _ ->
                    lifecycleScope.launch {
                        SessionStore(this@ProfileActivity).clear()
                        startActivity(Intent(this@ProfileActivity, LoginActivity::class.java))
                        finishAffinity()
                    }
                }.show()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val session = SessionStore(this@ProfileActivity).current()
            val snapshot = withContext(Dispatchers.IO) {
                val dao = DatabaseProvider.get(this@ProfileActivity).appDao()
                Triple(session?.let { dao.findUserByEmail(it.email) }, dao.studentCount(), dao.classCount())
            }
            val user = snapshot.first
            binding.tvName.text = "Coach ${user?.name ?: session?.name ?: "Guru"}"
            binding.tvStudents.text = "♟\nTotal Siswa\n${snapshot.second}"
            binding.tvClasses.text = "▣\nKelas\n${snapshot.third}"
            AvatarStore.bind(binding.ivPhoto, user?.profilePhotoPath, lifecycleScope)
        }
    }
}
