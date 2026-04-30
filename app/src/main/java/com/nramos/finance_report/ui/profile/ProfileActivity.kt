package com.nramos.finance_report.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.R
import com.nramos.finance_report.data.repository.CloudinaryRepository
import com.nramos.finance_report.databinding.ActivityProfileBinding
import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.domain.usecase.auth.GetCurrentUserUseCase
import com.nramos.finance_report.domain.usecase.profile.UpdateProfileUseCase
import com.nramos.finance_report.utils.NetworkResult
import com.nramos.finance_report.utils.ProfileUpdateEvent
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    @Inject
    lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @Inject
    lateinit var updateProfileUseCase: UpdateProfileUseCase

    @Inject
    lateinit var profileUpdateEvent: ProfileUpdateEvent

    @Inject
    lateinit var cloudinaryRepository: CloudinaryRepository

    private var selectedImageUri: Uri? = null
    private var currentUserData: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cloudinaryRepository.initCloudinary(
            this,
            BuildConfig.CLOUDINARY_CLOUD_NAME,
            BuildConfig.CLOUDINARY_API_KEY,
            BuildConfig.CLOUDINARY_API_SECRET
        )

        setupToolbar()
        loadUserData()
        setupListeners()
        setupAvatarClick()
        setupFieldIcons()
    }

    private fun setupFieldIcons() {
        binding.fieldNombre.ivFieldIcon.setImageResource(R.drawable.ic_user_avatar)
        binding.fieldApellidoPaterno.ivFieldIcon.setImageResource(R.drawable.ic_user_avatar)
        binding.fieldApellidoMaterno.ivFieldIcon.setImageResource(R.drawable.ic_user_avatar)
        binding.fieldGenero.ivFieldIcon.setImageResource(R.drawable.ic_gender)
        binding.fieldEmail.ivFieldIcon.setImageResource(R.drawable.ic_email)

        binding.fieldNombre.tvFieldLabel.text = "Nombre"
        binding.fieldApellidoPaterno.tvFieldLabel.text = "Apellido Paterno"
        binding.fieldApellidoMaterno.tvFieldLabel.text = "Apellido Materno"
        binding.fieldGenero.tvFieldLabel.text = "Género"
        binding.fieldEmail.tvFieldLabel.text = "Correo Electrónico"
    }

    private fun setupAvatarClick() {
        binding.flAvatarContainer.setOnClickListener {
            selectImage()
        }
    }

    private fun selectImage() {
        ImagePicker.with(this)
            .cropSquare()
            .compress(512)
            .maxResultSize(512, 512)
            .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                val uri = data?.data
                if (uri != null) {
                    selectedImageUri = uri
                    uploadImageToCloudinary(uri)
                }
            }
            ImagePicker.RESULT_ERROR -> {
                showToast(ImagePicker.getError(data))
            }
        }
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        lifecycleScope.launch {
            showToast("Subiendo imagen...")
            binding.progressBar.visibility = View.VISIBLE

            try {
                cloudinaryRepository.uploadImage(uri, BuildConfig.CLOUDINARY_UPLOAD_PRESET).collect { url ->
                    Log.d("ProfileActivity", "Upload URL: $url")

                    val currentUser = getCurrentUserUseCase()

                    updateProfileUseCase(
                        name = currentUser?.name ?: "",
                        paternalSurname = currentUser?.paternalSurname,
                        maternalSurname = currentUser?.maternalSurname,
                        gender = currentUser?.gender,
                        avatarUrl = url
                    ).collect { result ->
                        when (result) {
                            is NetworkResult.Loading -> {
                                // Loading
                            }
                            is NetworkResult.Success -> {
                                showToast("Foto de perfil actualizada")
                                refreshUserData()
                                profileUpdateEvent.emitUpdate()
                                binding.progressBar.visibility = View.GONE
                            }
                            is NetworkResult.Error -> {
                                showToast(result.message ?: "Error al actualizar")
                                binding.progressBar.visibility = View.GONE
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error: ${e.message}")
                showToast("Error: ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun refreshUserData() {
        lifecycleScope.launch {
            val user = getCurrentUserUseCase()
            user?.let { updateUI(it) }
        }
    }

    private fun updateUI(user: UserProfile) {
        currentUserData = user

        binding.tvFullName.text = "${user.name} ${user.paternalSurname ?: ""} ${user.maternalSurname ?: ""}".trim()
        binding.tvEmailSub.text = user.email

        binding.fieldNombre.tvFieldValue.text = user.name
        binding.fieldApellidoPaterno.tvFieldValue.text = user.paternalSurname ?: "—"
        binding.fieldApellidoMaterno.tvFieldValue.text = user.maternalSurname ?: "—"
        binding.fieldEmail.tvFieldValue.text = user.email

        val genderText = when (user.gender) {
            'M' -> "Masculino"
            'F' -> "Femenino"
            else -> "—"
        }
        binding.fieldGenero.tvFieldValue.text = genderText

        val initials = "${user.name.firstOrNull() ?: ""}${user.paternalSurname?.firstOrNull() ?: ""}"
        binding.tvInitials.text = initials.ifEmpty { "U" }

        user.avatarUrl?.let { url ->
            Glide.with(this@ProfileActivity)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_user_avatar)
                .error(R.drawable.ic_user_avatar)
                .into(binding.ivAvatar)
            binding.ivAvatar.visibility = View.VISIBLE
            binding.tvInitials.visibility = View.GONE
        } ?: run {
            binding.ivAvatar.visibility = View.GONE
            binding.tvInitials.visibility = View.VISIBLE
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val user = getCurrentUserUseCase()
            user?.let { updateUI(it) }
        }
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            showEditDialog()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun showEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEditName)?.text.toString()
                val paternalSurname = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEditPaternalSurname)?.text.toString()
                val maternalSurname = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEditMaternalSurname)?.text.toString()
                val genderSpinner = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.etEditGender)
                val genderText = genderSpinner?.text.toString()

                val gender = when (genderText) {
                    "Masculino" -> 'M'
                    "Femenino" -> 'F'
                    else -> null
                }

                if (name.isNotEmpty()) {
                    saveProfileChanges(name, paternalSurname, maternalSurname, gender)
                } else {
                    showToast("El nombre es requerido")
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        currentUserData?.let { user ->
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEditName)?.setText(user.name)
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEditPaternalSurname)?.setText(user.paternalSurname ?: "")
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEditMaternalSurname)?.setText(user.maternalSurname ?: "")

            val genderSpinner = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.etEditGender)
            val genders = listOf("Masculino", "Femenino", "Prefiero no decir")
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
            genderSpinner?.setAdapter(adapter)

            val genderText = when (user.gender) {
                'M' -> "Masculino"
                'F' -> "Femenino"
                else -> ""
            }
            genderSpinner?.setText(genderText, false)
        }

        dialog.show()
    }

    private fun saveProfileChanges(name: String, paternalSurname: String, maternalSurname: String, gender: Char?) {
        lifecycleScope.launch {
            updateProfileUseCase(
                name = name,
                paternalSurname = paternalSurname.takeIf { it.isNotEmpty() },
                maternalSurname = maternalSurname.takeIf { it.isNotEmpty() },
                gender = gender,
                avatarUrl = currentUserData?.avatarUrl
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        showToast("Actualizando...")
                    }
                    is NetworkResult.Success -> {
                        showToast("Perfil actualizado correctamente")
                        refreshUserData()
                        profileUpdateEvent.emitUpdate()
                    }
                    is NetworkResult.Error -> {
                        showToast(result.message ?: "Error al actualizar perfil")
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        showEditDialog()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}