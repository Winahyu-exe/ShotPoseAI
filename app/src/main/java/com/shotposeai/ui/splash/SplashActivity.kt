package com.shotposeai.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.shotposeai.R
import androidx.lifecycle.lifecycleScope
import com.shotposeai.databinding.ActivitySplashBinding
import com.shotposeai.data.SessionStore
import com.shotposeai.ui.home.MainActivity
import com.shotposeai.ui.login.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        styleSplashSystemBars()

        lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            val session = SessionStore(this@SplashActivity).current()
            val destination = if (session != null) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this@SplashActivity, destination))
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun styleSplashSystemBars() {
        val background = ContextCompat.getColor(this, R.color.primary_orange_soft)
        window.statusBarColor = background
        window.navigationBarColor = background
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 3000L
    }
}
