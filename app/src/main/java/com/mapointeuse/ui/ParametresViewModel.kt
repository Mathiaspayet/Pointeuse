package com.mapointeuse.ui

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapointeuse.data.AppDatabase
import com.mapointeuse.data.WorkPlace
import com.mapointeuse.data.WorkPlaceDao
import com.mapointeuse.service.NativeGeofencingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class ParametresUiState(
    val workPlace: WorkPlace? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentLocation: Location? = null
)

class ParametresViewModel(
    private val workPlaceDao: WorkPlaceDao,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParametresUiState())
    val uiState: StateFlow<ParametresUiState> = _uiState.asStateFlow()

    private val nativeGeofencingManager = NativeGeofencingManager(context)

    init {
        loadWorkPlace()
    }

    private fun loadWorkPlace() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            workPlaceDao.getActiveWorkPlace().collect { workPlace ->
                _uiState.value = _uiState.value.copy(
                    workPlace = workPlace,
                    isLoading = false
                )
            }
        }
    }

    fun saveWorkPlace(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        autoStart: Boolean,
        autoStop: Boolean,
        notifyOnEnter: Boolean,
        notifyOnExit: Boolean
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                val workPlace = WorkPlace(
                    id = _uiState.value.workPlace?.id ?: 0,
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    isActive = true,
                    autoStart = autoStart,
                    autoStop = autoStop,
                    notifyOnEnter = notifyOnEnter,
                    notifyOnExit = notifyOnExit
                )

                val id = workPlaceDao.insert(workPlace)
                workPlaceDao.deactivateOthers(id)

                // Enregistrer le geofence natif Android
                val savedWorkPlace = workPlace.copy(id = id)
                nativeGeofencingManager.registerGeofence(savedWorkPlace)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Lieu de travail enregistré avec détection automatique activée"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun deleteWorkPlace() {
        viewModelScope.launch {
            try {
                _uiState.value.workPlace?.let { workPlace ->
                    // Désenregistrer le geofence natif Android
                    nativeGeofencingManager.unregisterGeofence(workPlace.id)

                    workPlaceDao.delete(workPlace)
                    _uiState.value = _uiState.value.copy(
                        workPlace = null,
                        successMessage = "Lieu de travail supprimé et détection automatique désactivée"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun useCurrentLocation(location: Location) {
        _uiState.value = _uiState.value.copy(currentLocation = location)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                listOfNotNull(
                    address.thoroughfare,
                    address.locality
                ).joinToString(", ")
            }
        } catch (e: Exception) {
            null
        }
    }
}

class ParametresViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParametresViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            @Suppress("UNCHECKED_CAST")
            return ParametresViewModel(database.workPlaceDao(), context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
