package com.shotposeai.ui.profile

import com.shotposeai.ui.common.AvatarStore
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.SessionStore
import com.shotposeai.data.UserEntity
import com.shotposeai.databinding.ActivityAccountSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountSettingsBinding
    private var user: UserEntity? = null
    private var photoUri = ""
    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            binding.btnSave.isEnabled=false
            try {
                photoUri=AvatarStore.import(this@AccountSettingsActivity,uri)
                AvatarStore.bind(binding.ivPhoto,photoUri,lifecycleScope)
                binding.btnSave.isEnabled=true
                binding.ivPhoto.isEnabled=true
                binding.btnChangePhoto.isEnabled=true
            } catch(e:Exception){Toast.makeText(this@AccountSettingsActivity,"Foto tidak dapat dibaca",Toast.LENGTH_SHORT).show()}
            finally {binding.btnSave.isEnabled=true}
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityAccountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        photoUri=state?.getString("selected_photo").orEmpty()
        binding.btnSave.isEnabled=false
        binding.ivPhoto.isEnabled=false
        binding.btnChangePhoto.isEnabled=false
        binding.btnChangePhoto.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        binding.ivPhoto.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        lifecycleScope.launch {
            val session = SessionStore(this@AccountSettingsActivity).current() ?: return@launch
            user = withContext(Dispatchers.IO) {
                DatabaseProvider.get(this@AccountSettingsActivity).appDao().findUserByEmail(session.email)
            }
            user?.let {
                if(state==null) binding.etName.setText(it.name)
                if(state==null) binding.etNip.setText(it.employeeNumber)
                if(state==null) photoUri = it.profilePhotoPath.orEmpty()
                AvatarStore.bind(binding.ivPhoto,photoUri,lifecycleScope)
                binding.btnSave.isEnabled=true
                binding.ivPhoto.isEnabled=true
                binding.btnChangePhoto.isEnabled=true
            }
        }
        binding.btnSave.setOnClickListener { save() }
    }

    override fun onSaveInstanceState(outState:Bundle){outState.putString("selected_photo",photoUri);super.onSaveInstanceState(outState)}
    private fun save() {
        val current = user ?: return
        val name = binding.etName.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(this, "Nama wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnSave.isEnabled=false
        lifecycleScope.launch {
            current.name = name
            current.employeeNumber = binding.etNip.text.toString().trim()
            current.profilePhotoPath = photoUri
            withContext(Dispatchers.IO) {
                DatabaseProvider.get(this@AccountSettingsActivity).appDao().updateUser(current)
            }
            SessionStore(this@AccountSettingsActivity).let { store -> store.save(current, store.current()?.remember ?: false) }
            Toast.makeText(this@AccountSettingsActivity, "Profil tersimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
