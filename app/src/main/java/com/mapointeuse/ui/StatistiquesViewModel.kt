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
    val isLoading: Boolean = false,
    // Données pour les graphiques
    val minutesByDay: Map<DayOfWeek, Long> = emptyMap(),  // Pour le graphique en barres (semaine)
    val minutesByDate: Map<LocalDate, Long> = emptyMap(), // Pour le graphique en ligne (mois)
    val trendData: List<Float> = emptyList(),             // Pour le mini sparkline de tendance
    val tendancePercentage: Float = 0f                    // % de changement vs période précédente
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

                // Préparer les données pour les graphiques
                val minutesByDay = if (periode == PeriodeStatistique.SEMAINE) {
                    pointages.groupBy { it.date.dayOfWeek }
                        .mapValues { entry -> entry.value.sumOf { it.tempsTravailleMinutes } }
                } else emptyMap()

                val minutesByDate = if (periode == PeriodeStatistique.MOIS || periode == PeriodeStatistique.ANNEE) {
                    pointages.groupBy { it.date }
                        .mapValues { entry -> entry.value.sumOf { it.tempsTravailleMinutes } }
                } else emptyMap()

                // Créer les données de tendance (derniers 7 jours d'heures)
                val trendData = pointages.sortedBy { it.date }
                    .takeLast(7)
                    .map { (it.tempsTravailleMinutes / 60f) }

                // Calculer la tendance (comparaison avec période précédente)
                val tendancePercentage = calculateTendance(periode, startDate, endDate, totalMinutes)

                _uiState.value = _uiState.value.copy(
                    pointages = pointages,
                    totalMinutes = totalMinutes,
                    nombreJours = nombreJours,
                    moyenneParJour = moyenneParJour,
                    minutesByDay = minutesByDay,
                    minutesByDate = minutesByDate,
                    trendData = trendData,
                    tendancePercentage = tendancePercentage,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun calculateTendance(
        periode: PeriodeStatistique,
        startDate: LocalDate,
        endDate: LocalDate,
        currentTotal: Long
    ): Float {
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val previousStart = startDate.minusDays(daysDiff.toLong())
        val previousEnd = endDate.minusDays(daysDiff.toLong())

        var previousTotal = 0L
        repository.getPointagesBetweenDates(previousStart, previousEnd).collect { pointages ->
            previousTotal = pointages.sumOf { it.tempsTravailleMinutes }
        }

        return if (previousTotal > 0) {
            ((currentTotal - previousTotal).toFloat() / previousTotal) * 100f
        } else if (currentTotal > 0) {
            100f // Si pas de données précédentes mais données actuelles = +100%
        } else {
            0f
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
