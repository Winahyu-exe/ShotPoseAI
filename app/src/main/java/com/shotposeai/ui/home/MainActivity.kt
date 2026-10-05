package com.shotposeai.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.shotposeai.R
import com.shotposeai.databinding.ActivityMainBinding
import com.shotposeai.ui.management.ManagementFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_classes -> ManagementFragment.newInstance(ManagementFragment.MODE_CLASSES)
                R.id.nav_students -> ManagementFragment.newInstance(ManagementFragment.MODE_STUDENTS)
                R.id.nav_history -> ManagementFragment.newInstance(ManagementFragment.MODE_REPORTS)
                else -> null
            }
            fragment?.let(::replaceFragment) != null
        }
        if (savedInstanceState == null) binding.bottomNavigation.selectedItemId = R.id.nav_home
    }
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}
