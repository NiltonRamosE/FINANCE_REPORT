package com.nramos.finance_report.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import com.google.android.material.navigation.NavigationView
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.ActivityMainBinding
import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.domain.usecase.auth.GetCurrentUserUseCase
import com.nramos.finance_report.domain.usecase.auth.LogoutUseCase
import com.nramos.finance_report.ui.auth.login.LoginActivity
import com.nramos.finance_report.ui.profile.ProfileActivity
import com.nramos.finance_report.utils.NetworkResult
import com.nramos.finance_report.utils.ProfileUpdateEvent
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout

    @Inject
    lateinit var logoutUseCase: LogoutUseCase

    @Inject
    lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @Inject
    lateinit var profileUpdateEvent: ProfileUpdateEvent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        setupNavigation()
        setupUserInfo()
        setupLogout()
        setupProfileClick()
        observeProfileUpdates()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        // También refrescar cuando se vuelve de ProfileActivity
        refreshUserInfo()
    }

    private fun setupNavigation() {
        drawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_dashboard, R.id.nav_reports, R.id.nav_movements),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun setupUserInfo() {
        refreshUserInfo()
    }

    private fun refreshUserInfo() {
        lifecycleScope.launch {
            val user = getCurrentUserUseCase()
            user?.let {
                updateNavHeader(it)
            }
        }
    }

    private fun updateNavHeader(user: UserProfile) {
        val navView = binding.navView
        val headerView = navView.getHeaderView(0)

        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)
        val ivAvatar = headerView.findViewById<CircleImageView>(R.id.ivAvatar)

        val fullName = buildString {
            append(user.name)
            user.paternalSurname?.takeIf { it.isNotEmpty() }?.let { append(" $it") }
            user.maternalSurname?.takeIf { it.isNotEmpty() }?.let { append(" $it") }
        }

        tvUserName.text = fullName.ifEmpty { user.name }
        tvUserEmail.text = user.email

        if (!user.avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_user_avatar)
                .error(R.drawable.ic_user_avatar)
                .skipMemoryCache(true)
                .into(ivAvatar)
        } else {
            ivAvatar.setImageResource(R.drawable.ic_user_avatar)
        }
    }

    private fun setupProfileClick() {
        val navView = binding.navView
        val headerView = navView.getHeaderView(0)
        val btnViewProfile = headerView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewProfile)

        btnViewProfile.setOnClickListener {
            drawerLayout.closeDrawers()
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeProfileUpdates() {
        lifecycleScope.launch {
            profileUpdateEvent.updateFlow.collect {
                refreshUserInfo()
            }
        }
    }

    private fun setupLogout() {
        val navView = binding.navView
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    drawerLayout.closeDrawers()
                    performLogout()
                    true
                }
                else -> {
                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val navController = navHostFragment.navController
                    val handled = try {
                        navController.navigate(
                            menuItem.itemId,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(navController.graph.startDestinationId, false)
                                .build()
                        )
                        true
                    } catch (e: Exception) {
                        false
                    }
                    if (handled) {
                        drawerLayout.closeDrawers()
                    }
                    handled
                }
            }
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            logoutUseCase().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        showToast("Cerrando sesión...")
                    }
                    is NetworkResult.Success -> {
                        showToast("Sesión cerrada correctamente")
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    is NetworkResult.Error -> {
                        showToast(result.message ?: "Error al cerrar sesión")
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}