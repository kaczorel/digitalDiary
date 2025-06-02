package pl.edu.pja.s27599.digitaldiary.ui.entry_detail

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import pl.edu.pja.s27599.digitaldiary.R
import pl.edu.pja.s27599.digitaldiary.ui.components.ImageEditor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    viewModel: EntryDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val title by viewModel.title
    val content by viewModel.content
    val location by viewModel.location
    val photoUri by viewModel.photoUri
    val audioUri by viewModel.audioUri
    val isRecording by viewModel.isRecording
    val isLocationLoading by viewModel.isLocationLoading
    val isPlayingAudio by viewModel.isPlayingAudio

    val titleError by viewModel.titleError
    val contentError by viewModel.contentError

    val isEditing = viewModel.entryId.value != null

    var showCameraPreview by remember { mutableStateOf(false) }
    var showImageEditor by remember { mutableStateOf(false) }
    var currentPhotoFileUri by remember { mutableStateOf<Uri?>(null) }


    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showCameraPreview = true
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val started = viewModel.startAudioRecording()
            if (!started) {
                Toast.makeText(context, context.getString(R.string.audio_record_error), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.audio_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap: Map<String, Boolean> ->
        val fineLocationGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.fetchCurrentLocation()
        } else {
            Toast.makeText(context, context.getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.edit_entry_title else R.string.add_entry_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel_button))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { newValue ->
                    viewModel.title.value = newValue
                    viewModel.clearErrors()
                },
                label = { Text(stringResource(R.string.entry_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) {
                        Text(text = titleError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { newValue ->
                    viewModel.content.value = newValue
                    viewModel.clearErrors()
                },
                label = { Text(stringResource(R.string.entry_content_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                isError = contentError != null,
                supportingText = {
                    if (contentError != null) {
                        Text(text = contentError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.location_label)}: ${location ?: stringResource(R.string.location_not_set)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isLocationLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(onClick = {
                        val fineLocGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarseLocGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                        if (fineLocGranted || coarseLocGranted) {
                            viewModel.fetchCurrentLocation()
                        } else {
                            locationPermissionsLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    }) {
                        Icon(Icons.Filled.LocationOn, stringResource(R.string.current_location_button))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Photo
            if (photoUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = stringResource(R.string.image_thumbnail_description),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = { currentPhotoFileUri = photoUri.toString().toUri(); showImageEditor = true }) {
                        Text(stringResource(R.string.add_text_to_image_button))
                    }
                    Button(onClick = { viewModel.deletePhoto() }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_photo))
                        Text(stringResource(R.string.delete_photo))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    showCameraPreview = true
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text(stringResource(R.string.add_photo_button))
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Audio Recording
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (audioUri != null) stringResource(R.string.audio_recorded_message) else stringResource(R.string.no_audio_message),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isRecording) {
                    Button(onClick = { viewModel.stopAudioRecording() }) {
                        Icon(Icons.Filled.Stop, stringResource(R.string.stop_recording_button))
                        Text(stringResource(R.string.stop_recording_button))
                    }
                } else {
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            val started = viewModel.startAudioRecording()
                            if (!started) {
                                Toast.makeText(context, context.getString(R.string.audio_record_error), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Icon(Icons.Filled.Mic, stringResource(R.string.record_audio_button))
                        Text(stringResource(R.string.record_audio_button))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (audioUri != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = {
                        if (isPlayingAudio) viewModel.stopAudio() else viewModel.playAudio()
                    }) {
                        Icon(
                            imageVector = if (isPlayingAudio) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlayingAudio) stringResource(R.string.stop_audio_button) else stringResource(R.string.audio_play_button)
                        )
                        Text(if (isPlayingAudio) stringResource(R.string.stop_audio_button) else stringResource(R.string.audio_play_button))
                    }
                    Button(onClick = { viewModel.deleteAudio() }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_audio))
                        Text(stringResource(R.string.delete_audio))
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
                Button(onClick = {
                    if (viewModel.saveEntry()) {
                        Toast.makeText(context, context.getString(R.string.entry_saved_successfully), Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                }) {
                    Text(stringResource(R.string.save_button))
                }
            }
        }

        if (showCameraPreview) {
            CameraPreviewDialog(
                onDismiss = { showCameraPreview = false },
                onPhotoCaptured = { uri ->
                    viewModel.setPhotoUri(uri)
                    showCameraPreview = false
                }
            )
        }

        if (showImageEditor && currentPhotoFileUri != null) {
            ImageEditorDialog(
                imageUri = currentPhotoFileUri!!,
                onDismiss = { showImageEditor = false },
                onImageEdited = { editedUri ->
                    viewModel.setPhotoUri(editedUri)
                    showImageEditor = false
                }
            )
        }
    }
}

@Composable
fun CameraPreviewDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.photo_capture_error, e.message), Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.take_photo_button)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val photoFile = File(
                        context.externalCacheDir,
                        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                                onPhotoCaptured(savedUri)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(context, context.getString(R.string.photo_capture_error, exception.message), Toast.LENGTH_SHORT).show()
                                exception.printStackTrace()
                            }
                        }
                    )
                }) {
                    Text(stringResource(R.string.take_photo_button))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun ImageEditorDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onImageEdited: (Uri) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val saveTrigger = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.drawing_on_image_description)) },
        text = {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageEditor(
                    modifier = Modifier.fillMaxSize(),
                    imageUri = imageUri,
                    onImageEdited = { editedUri ->
                        onImageEdited(editedUri)
                        isLoading = false
                        saveTrigger.value = false
                        onDismiss()
                    },
                    saveTrigger = saveTrigger
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                isLoading = true
                saveTrigger.value = true

            }) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        },
        modifier = Modifier.fillMaxSize(0.95f)
    )
}