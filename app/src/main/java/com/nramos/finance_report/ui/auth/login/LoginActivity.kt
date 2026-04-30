package com.nramos.finance_report.ui.auth.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.databinding.ActivityLoginBinding
import com.nramos.finance_report.ui.main.MainActivity
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.nramos.finance_report.data.auth.GoogleSignInManager
import com.nramos.finance_report.domain.usecase.auth.IsLoggedInUseCase
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    @Inject
    lateinit var googleSignInManager: GoogleSignInManager

    @Inject
    lateinit var isLoggedInUseCase: IsLoggedInUseCase

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        when (result.resultCode) {
            RESULT_OK -> {
                lifecycleScope.launch {
                    val success = viewModel.handleGoogleSignInResult(result.data)
                }
            }
            RESULT_CANCELED -> {
                viewModel.resetGoogleRequest()
                showToast("Inicio de sesión con Google cancelado")
            }
            else -> {
                viewModel.resetGoogleRequest()
                showToast("Error al iniciar sesión con Google (código: ${result.resultCode})")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (isLoggedInUseCase()) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
                return@launch
            }
            setupUI()
        }
        requestNotificationPermission()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvVersion.text = "Versión ${BuildConfig.VERSION_NAME}"
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.btnGoogleLogin.isEnabled = !state.isLoading
                binding.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

                state.error?.let { error ->
                    showToast(error)
                }

                if (state.isSuccess) {
                    showToast("Bienvenido ${state.user?.name}")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                    viewModel.resetSuccess()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnGoogleLogin.setOnClickListener {
                viewModel.onEvent(LoginEvent.OnGoogleLoginClick)
                val signInIntent = googleSignInManager.getSignInIntentWithAccountSelection()
                googleSignInLauncher.launch(signInIntent)
            }
            btnInstagram.setOnClickListener {
                openUrl("https://www.instagram.com/ramos._.xd")
            }

            btnGithub.setOnClickListener {
                openUrl("https://github.com/NiltonRamosE")
            }

            btnLinkedin.setOnClickListener {
                openUrl("https://www.linkedin.com/in/niltonramosencarnacion/")
            }
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}