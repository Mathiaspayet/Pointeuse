package com.mapointeuse.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapointeuse.R
import com.mapointeuse.data.Pointage
import com.mapointeuse.data.StatutPointage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatistiquesScreen(
    viewModel: StatistiquesViewModel,
    onPointageClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.statistiques)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodeStatistique.entries.forEach { periode ->
                    FilterChip(
                        selected = uiState.periode == periode,
                        onClick = { viewModel.changePeriode(periode) },
                        label = {
                            Text(
                                when (periode) {
                                    PeriodeStatistique.JOUR -> stringResource(id = R.string.jour)
                                    PeriodeStatistique.SEMAINE -> stringResource(id = R.string.semaine)
                                    PeriodeStatistique.MOIS -> stringResource(id = R.string.mois)
                                    PeriodeStatistique.ANNEE -> stringResource(id = R.string.annee)
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val totalHours = uiState.totalMinutes / 60
                val totalMinutes = uiState.totalMinutes % 60

                StatCard(
                    icon = Icons.Default.Timer,
                    title = stringResource(id = R.string.temps_total),
                    value = "${totalHours}h ${totalMinutes}min",
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.CalendarMonth,
                        title = stringResource(id = R.string.nb_jours),
                        value = "${uiState.nombreJours}",
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    val moyenneHours = uiState.moyenneParJour / 60
                    val moyenneMinutes = uiState.moyenneParJour % 60
                    StatCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = stringResource(id = R.string.moyenne_par_jour),
                        value = "${moyenneHours}h ${moyenneMinutes}m",
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.detail_pointages),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.pointages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(id = R.string.aucun_pointage_periode))
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.pointages) { pointage ->
                            PointageItem(
                                pointage = pointage,
                                onClick = { onPointageClick(pointage.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointageItem(pointage: Pointage, onClick: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val statusIcon = when (pointage.statut) {
        StatutPointage.EN_COURS -> Icons.Default.PlayArrow
        StatutPointage.EN_PAUSE -> Icons.Default.Pause
        StatutPointage.TERMINE -> Icons.Default.CheckCircle
    }
    val statusColor = when (pointage.statut) {
        StatutPointage.EN_COURS -> Color(0xFF4CAF50)
        StatutPointage.EN_PAUSE -> Color(0xFFFF9800)
        StatutPointage.TERMINE -> Color(0xFF2196F3)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = statusColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pointage.date.format(dateFormatter).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${pointage.heureDebut.format(timeFormatter)} - ${pointage.heureFin?.format(timeFormatter) ?: "..."}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                    if (pointage.tempsTravailleMinutes > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val hours = pointage.tempsTravailleMinutes / 60
                    val minutes = pointage.tempsTravailleMinutes % 60
                    Text(
                        text = "${hours}h ${minutes}min",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
