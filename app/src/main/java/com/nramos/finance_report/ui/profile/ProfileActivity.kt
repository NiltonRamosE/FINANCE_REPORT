package com.nramos.finance_report.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nramos.finance_report.databinding.ActivityProfileBinding
import com.nramos.finance_report.domain.usecase.auth.GetCurrentUserUseCase
import com.nramos.finance_report.domain.usecase.profile.UpdateProfileUseCase
import com.nramos.finance_report.utils.NetworkResult
import com.nramos.finance_report.utils.ProfileUpdateEvent
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var isEditMode = false

    @Inject
    lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @Inject
    lateinit var updateProfileUseCase: UpdateProfileUseCase

    @Inject
    lateinit var profileUpdateEvent: ProfileUpdateEvent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupGenderSpinner()
        loadUserData()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mi Perfil"
    }

    private fun setupGenderSpinner() {
        val genders = listOf("Masculino", "Femenino", "Prefiero no decir")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.etGender.setAdapter(adapter)
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val user = getCurrentUserUseCase()
            user?.let {
                binding.etName.setText(it.name)
                binding.etEmail.setText(it.email)
                binding.etPaternalSurname.setText(it.paternalSurname ?: "")
                binding.etMaternalSurname.setText(it.maternalSurname ?: "")

                val genderText = when (it.gender) {
                    'M' -> "Masculino"
                    'F' -> "Femenino"
                    else -> ""
                }
                binding.etGender.setText(genderText, false)
            }
        }
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            toggleEditMode(true)
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun toggleEditMode(edit: Boolean) {
        isEditMode = edit
        binding.apply {
            etName.isEnabled = edit
            etPaternalSurname.isEnabled = edit
            etMaternalSurname.isEnabled = edit
            etGender.isEnabled = edit

            btnEditProfile.visibility = if (edit) View.GONE else View.VISIBLE
            btnSaveProfile.visibility = if (edit) View.VISIBLE else View.GONE
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val paternalSurname = binding.etPaternalSurname.text.toString().trim()
        val maternalSurname = binding.etMaternalSurname.text.toString().trim()
        val genderText = binding.etGender.text.toString().trim()

        val gender = when (genderText) {
            "Masculino" -> 'M'
            "Femenino" -> 'F'
            else -> null
        }

        if (name.isEmpty()) {
            showToast("El nombre es requerido")
            return
        }

        lifecycleScope.launch {
            updateProfileUseCase(
                name = name,
                paternalSurname = paternalSurname.takeIf { it.isNotEmpty() },
                maternalSurname = maternalSurname.takeIf { it.isNotEmpty() },
                gender = gender
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        showToast("Actualizando...")
                    }
                    is NetworkResult.Success -> {
                        showToast("Perfil actualizado correctamente")
                        toggleEditMode(false)

                        profileUpdateEvent.emitUpdate()

                        finish()
                    }
                    is NetworkResult.Error -> {
                        showToast(result.message ?: "Error al actualizar perfil")
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (isEditMode) {
            showToast("Cancela la edición o guarda los cambios")
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
        return true
    }
}