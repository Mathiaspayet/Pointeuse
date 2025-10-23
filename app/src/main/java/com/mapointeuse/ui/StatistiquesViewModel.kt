package com.mapointeuse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapointeuse.data.Pointage
import com.mapointeuse.data.PointageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class PeriodeStatistique {
    JOUR, SEMAINE, MOIS, ANNEE
}

data class StatistiquesUiState(
    val pointages: List<Pointage> = emptyList(),
    val periode: PeriodeStatistique = PeriodeStatistique.SEMAINE,
    val totalMinutes: Long = 0,
    val nombreJours: Int = 0,
    val moyenneParJour: Long = 0,
    val isLoading: Boolean = false
)

class StatistiquesViewModel(private val repository: PointageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StatistiquesUiState())
    val uiState: StateFlow<StatistiquesUiState> = _uiState.asStateFlow()

    init {
        loadStatistiques(PeriodeStatistique.SEMAINE)
    }

    fun loadStatistiques(periode: PeriodeStatistique) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, periode = periode)

            val (startDate, endDate) = getDateRange(periode)

            repository.getPointagesBetweenDates(startDate, endDate).collect { pointages ->
                val totalMinutes = pointages.sumOf { it.tempsTravailleMinutes }
                val nombreJours = pointages.map { it.date }.distinct().size
                val moyenneParJour = if (nombreJours > 0) totalMinutes / nombreJours else 0

                _uiState.value = _uiState.value.copy(
                    pointages = pointages,
                    totalMinutes = totalMinutes,
                    nombreJours = nombreJours,
                    moyenneParJour = moyenneParJour,
                    isLoading = false
                )
            }
        }
    }

    private fun getDateRange(periode: PeriodeStatistique): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (periode) {
            PeriodeStatistique.JOUR -> {
                today to today
            }
            PeriodeStatistique.SEMAINE -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                startOfWeek to endOfWeek
            }
            PeriodeStatistique.MOIS -> {
                val startOfMonth = today.withDayOfMonth(1)
                val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
                startOfMonth to endOfMonth
            }
            PeriodeStatistique.ANNEE -> {
                val startOfYear = today.withDayOfYear(1)
                val endOfYear = today.with(TemporalAdjusters.lastDayOfYear())
                startOfYear to endOfYear
            }
        }
    }

    fun changePeriode(periode: PeriodeStatistique) {
        loadStatistiques(periode)
    }
}

class StatistiquesViewModelFactory(private val repository: PointageRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatistiquesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatistiquesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
