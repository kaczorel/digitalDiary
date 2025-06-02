package pl.edu.pja.s27599.digitaldiary.ui.auth

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import pl.edu.pja.s27599.digitaldiary.R

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val dataStore = application.dataStore
    private val PIN_KEY = stringPreferencesKey("app_pin")


    val pinInput = mutableStateOf("")
    val isAuthenticated = mutableStateOf(false)
    val showSetupPinDialog = mutableStateOf(false)
    val setupPinConfirmation = mutableStateOf("")
    val setupPinError = mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            val savedPin = getSavedPin()
            if (savedPin.isEmpty()) {
                showSetupPinDialog.value = true
            }
        }
    }

    private suspend fun getSavedPin(): String {
        val preferences = dataStore.data.first()
        return preferences[PIN_KEY] ?: ""
    }

    fun verifyPin() {
        viewModelScope.launch {
            val savedPin = getSavedPin()
            if (pinInput.value == savedPin) {
                isAuthenticated.value = true
                setupPinError.value = null
            } else {
                isAuthenticated.value = false
                setupPinError.value = getApplication<Application>().getString(R.string.incorrect_pin_error)
            }
        }
    }

    fun setPin() {
        if (pinInput.value.length == 4 && pinInput.value == setupPinConfirmation.value) {
            viewModelScope.launch {
                dataStore.edit { settings ->
                    settings[PIN_KEY] = pinInput.value
                }
                showSetupPinDialog.value = false
                isAuthenticated.value = true
                setupPinError.value = null
            }
        } else {
            setupPinError.value = getApplication<Application>().getString(R.string.pin_mismatch_error)
        }
    }

    fun clearError() {
        setupPinError.value = null
    }

    fun resetState() {
        pinInput.value = ""
        setupPinConfirmation.value = ""
        setupPinError.value = null
    }
}