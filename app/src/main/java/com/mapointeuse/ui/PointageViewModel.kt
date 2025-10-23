package com.mapointeuse.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapointeuse.data.Pointage
import com.mapointeuse.data.PointageRepository
import com.mapointeuse.data.StatutPointage
import com.mapointeuse.service.PointageService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class PointageUiState(
    val pointageActuel: Pointage? = null,
    val pointagesJour: List<Pointage> = emptyList(),
    val sessionDurationSeconds: Long = 0,
    val totalMinutesJour: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PointageViewModel(
    private val repository: PointageRepository,
    private val context: Context
) : ViewModel() {

    private var timerJob: Job? = null
    private val today = LocalDate.now()

    private val _uiState = MutableStateFlow(PointageUiState(isLoading = true))
    val uiState: StateFlow<PointageUiState> = _uiState.asStateFlow()

    init {
        // Charger de manière asynchrone sans bloquer
        viewModelScope.launch {
            combine(
                repository.observeActivePointageForDate(today),
                repository.getPointagesForDate(today)
            ) { active, pointages ->
                active to pointages
            }.collect { (active, pointages) ->
                val durationSeconds = calculateDurationSeconds(active)
                val totalMinutes = calculateTotalMinutes(pointages)
                _uiState.value = _uiState.value.copy(
                    pointageActuel = active,
                    pointagesJour = pointages,
                    sessionDurationSeconds = durationSeconds,
                    totalMinutesJour = totalMinutes,
                    isLoading = false
                )
                scheduleTimer(active)
            }
        }
    }

    fun startWork() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                repository.startWork()
                // Démarrer le service de suivi en arrière-plan
                PointageService.startTracking(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun endWork() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                repository.endWork()
                // Arrêter le service de suivi en arrière-plan
                PointageService.stopTracking(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun startPause() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                repository.startPause()
                // Mettre à jour la notification pour afficher l'état pause
                PointageService.updatePauseStatus(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun endPause() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                repository.endPause()
                // Mettre à jour la notification pour afficher l'état actif
                PointageService.updatePauseStatus(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun canStartWork(): Boolean {
        return _uiState.value.pointageActuel == null
    }

    fun canEndWork(): Boolean {
        val pointage = _uiState.value.pointageActuel
        return pointage != null && pointage.statut != StatutPointage.TERMINE
    }

    fun canStartPause(): Boolean {
        val pointage = _uiState.value.pointageActuel
        return pointage != null && pointage.statut == StatutPointage.EN_COURS
    }

    fun canEndPause(): Boolean {
        val pointage = _uiState.value.pointageActuel
        return pointage != null && pointage.statut == StatutPointage.EN_PAUSE
    }

    private fun scheduleTimer(pointage: Pointage?) {
        timerJob?.cancel()
        if (pointage?.statut == StatutPointage.EN_COURS) {
            timerJob = viewModelScope.launch {
                while (isActive) {
                    val seconds = calculateDurationSeconds(pointage)
                    _uiState.value = _uiState.value.copy(sessionDurationSeconds = seconds)
                    delay(1_000L)
                }
            }
        }
    }

    private fun calculateDurationSeconds(pointage: Pointage?): Long {
        return when {
            pointage == null -> 0L
            pointage.statut == StatutPointage.TERMINE -> pointage.tempsTravailleMinutes * 60
            pointage.statut == StatutPointage.EN_PAUSE -> ChronoUnit.SECONDS.between(pointage.heureDebut, LocalDateTime.now())
            else -> ChronoUnit.SECONDS.between(pointage.heureDebut, LocalDateTime.now())
        }.coerceAtLeast(0)
    }

    private fun calculateTotalMinutes(pointages: List<Pointage>): Long {
        return pointages
            .filter { it.tempsTravailleMinutes > 0 }
            .sumOf { it.tempsTravailleMinutes }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}

class PointageViewModelFactory(
    private val repository: PointageRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PointageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PointageViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
