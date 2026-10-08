package com.ynov.helloworld.ui.add

import android.Manifest
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.ynov.helloworld.R
import com.ynov.helloworld.databinding.ActivityAddNoteBinding
import com.ynov.helloworld.location.hasLocationPermission
import com.ynov.helloworld.ui.formatCoordinates
import com.ynov.helloworld.ui.spokenCoordinates
import kotlinx.coroutines.launch
import java.io.File

/**
 * Création d'une note (vue de [AddNoteViewModel]). Ne garde que ce qu'Android impose à une
 * activité : les lanceurs de l'appareil photo, de la galerie et des permissions.
 *
 * Le bouton « Enregistrer » n'est jamais désactivé : un bouton grisé n'explique pas pourquoi
 * l'action est impossible, alors que le champ en erreur le dit.
 */
class AddNoteActivity : AppCompatActivity() {

    private val viewModel: AddNoteViewModel by viewModels { AddNoteViewModel.Factory }
    private lateinit var binding: ActivityAddNoteBinding
    private var shownPhoto: String? = null

    // region Lanceurs système

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) viewModel.locate() else viewModel.onLocationDenied()
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        viewModel.onPictureTaken(it)
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onImagePicked(uri)
    }

    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { discardAndFinish() }
        onBackPressedDispatcher.addCallback(this) { discardAndFinish() }
        binding.title.doAfterTextChanged { viewModel.clearTitleError() }
        binding.takePhoto.setOnClickListener { takePicture() }
        binding.retakePhoto.setOnClickListener { takePicture() }
        binding.pickPhoto.setOnClickListener { pickImage() }
        binding.repickPhoto.setOnClickListener { pickImage() }
        binding.removePhoto.setOnClickListener { viewModel.removePhoto() }
        binding.refreshLocation.setOnClickListener { requestLocation() }
        binding.save.setOnClickListener {
            viewModel.save(binding.title.text.toString(), binding.content.text.toString())
        }

        if (viewModel.locationNeeded) requestLocation()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun discardAndFinish() {
        viewModel.discard()
        finish()
    }

    private fun takePicture() {
        val file = viewModel.preparePhotoFile()
        takePictureLauncher.launch(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
    }

    private fun pickImage() {
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun requestLocation() {
        if (hasLocationPermission(this)) {
            viewModel.locate()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // region Affichage

    private fun render(state: AddNoteState) {
        if (state.saved) {
            finish()
            return
        }
        binding.titleLayout.error = if (state.titleError) getString(R.string.add_field_title_error) else null
        if (state.titleError && !binding.title.hasFocus()) binding.title.requestFocus()
        renderPhoto(state)
        renderLocation(state)
    }

    private fun renderPhoto(state: AddNoteState) {
        val path = state.photoPath
        val processing = state.photoProcessing
        binding.photoProcessing.visibility = if (processing) View.VISIBLE else View.GONE
        binding.photoEmpty.visibility = if (!processing && path == null) View.VISIBLE else View.GONE
        binding.photoPreview.visibility = if (!processing && path != null) View.VISIBLE else View.GONE
        if (path != null && path != shownPhoto) binding.photo.load(File(path))
        shownPhoto = path
    }

    private fun renderLocation(state: AddNoteState) {
        val lat = state.latitude
        val lon = state.longitude
        val failed = !state.locating && lat == null
        val view = binding.root

        binding.locationProgress.visibility = if (state.locating) View.VISIBLE else View.GONE
        binding.locationIcon.visibility = if (state.locating) View.GONE else View.VISIBLE
        binding.locationIcon.setImageResource(if (failed) R.drawable.ic_location_off else R.drawable.ic_my_location)
        binding.locationBadge.backgroundTintList = ColorStateList.valueOf(
            MaterialColors.getColor(view, if (failed) MaterialR.attr.colorErrorContainer else MaterialR.attr.colorPrimaryContainer)
        )
        binding.locationIcon.imageTintList = ColorStateList.valueOf(
            MaterialColors.getColor(view, if (failed) MaterialR.attr.colorOnErrorContainer else MaterialR.attr.colorOnPrimaryContainer)
        )
        binding.refreshLocation.isEnabled = !state.locating

        binding.locationStatus.text = when {
            state.locating -> getString(R.string.add_location_searching)
            lat != null && lon != null -> formatCoordinates(lat, lon)
            else -> getString(state.locationError ?: R.string.add_location_none)
        }
        binding.locationStatus.contentDescription =
            if (!state.locating && lat != null && lon != null) spokenCoordinates(lat, lon) else null
        binding.locationStatus.setTextColor(
            MaterialColors.getColor(view, if (failed) androidx.appcompat.R.attr.colorError else MaterialR.attr.colorOnSurfaceVariant)
        )
    }

    // endregion
}
