package pl.edu.pja.s27599.digitaldiary.ui.entry_detail

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import pl.edu.pja.s27599.digitaldiary.R
import pl.edu.pja.s27599.digitaldiary.data.local.model.Entry
import pl.edu.pja.s27599.digitaldiary.data.local.repository.EntryRepository
import pl.edu.pja.s27599.digitaldiary.utils.AudioRecorder
import pl.edu.pja.s27599.digitaldiary.utils.LocationUtils
import java.io.IOException
import java.util.Date
import androidx.core.net.toUri

const val ENTRY_ID_ARG = "entryId"

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    application: Application,
    private val repository: EntryRepository,
    private val audioRecorder: AudioRecorder,
    private val locationUtils: LocationUtils,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val initialEntryId: Int? = savedStateHandle[ENTRY_ID_ARG]

    val entryId = mutableStateOf<Int?>(null)
    val title = mutableStateOf("")
    val content = mutableStateOf("")
    val location = mutableStateOf<String?>(null)
    val photoUri = mutableStateOf<String?>(null)
    val audioUri = mutableStateOf<String?>(null)

    val isRecording = mutableStateOf(false)
    val isLocationLoading = mutableStateOf(false)
    val isPlayingAudio = mutableStateOf(false)

    val titleError = mutableStateOf<String?>(null)
    val contentError = mutableStateOf<String?>(null)

    private var mediaPlayer: MediaPlayer? = null

    init {
        if (initialEntryId != null && initialEntryId != -1) {
            entryId.value = initialEntryId
            loadEntry(initialEntryId)
        }
    }

    private fun loadEntry(id: Int) {
        viewModelScope.launch {
            repository.getEntryById(id)?.let { entry ->
                entryId.value = entry.id
                title.value = entry.title
                content.value = entry.content
                location.value = entry.location
                photoUri.value = entry.photoUri
                audioUri.value = entry.audioUri
            }
        }
    }

    fun saveEntry(): Boolean {
        titleError.value = null
        contentError.value = null

        var isValid = true

        if (title.value.isBlank()) {
            titleError.value = getApplication<Application>().getString(R.string.title_cannot_be_empty)
            isValid = false
        }
        if (content.value.isBlank()) {
            contentError.value = getApplication<Application>().getString(R.string.content_cannot_be_empty)
            isValid = false
        }

        if (isValid) {
            viewModelScope.launch {
                val currentEntry = Entry(
                    id = entryId.value ?: 0,
                    title = title.value,
                    content = content.value,
                    location = location.value,
                    photoUri = photoUri.value,
                    audioUri = audioUri.value,
                    timestamp = Date()
                )

                if (entryId.value == null || entryId.value == 0) {
                    repository.insert(currentEntry)
                } else {
                    repository.update(currentEntry)
                }
            }
            return true
        }
        return false
    }

    fun clearErrors() {
        titleError.value = null
        contentError.value = null
    }

    fun fetchCurrentLocation() {
        if (locationUtils.hasLocationPermissions()) {
            isLocationLoading.value = true
            viewModelScope.launch {
                locationUtils.getCurrentLocation().collect { loc ->
                    loc?.let {
                        location.value = locationUtils.getCityName(it)
                    } ?: run {
                        location.value = null
                    }
                    isLocationLoading.value = false
                }
            }
        } else {
            location.value = null
        }
    }

    fun setPhotoUri(uri: Uri?) {
        photoUri.value = uri?.toString()
    }

    fun deletePhoto() {
        photoUri.value = null
    }

    fun startAudioRecording(): Boolean {
        if (audioRecorder.hasRecordAudioPermission()) {
            val path = audioRecorder.startRecording()
            if (path != null) {
                audioUri.value = path
                isRecording.value = true
                return true
            }
        }
        return false
    }

    fun stopAudioRecording() {
        audioRecorder.stopRecording()
        isRecording.value = false
    }

    fun playAudio() {
        audioUri.value?.let { uriString ->
            val uri = uriString.toUri()
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    try {
                        setDataSource(getApplication(), uri)
                        prepare()
                        start()
                        isPlayingAudio.value = true
                        setOnCompletionListener {
                            stopAudio()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        stopAudio()
                    }
                }
            } else if (!mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
                isPlayingAudio.value = true
            }
        }
    }

    fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        isPlayingAudio.value = false
    }

    fun deleteAudio() {
        stopAudio()
        audioUri.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}