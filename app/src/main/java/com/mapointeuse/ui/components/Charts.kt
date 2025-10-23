package com.mapointeuse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

/**
 * Données pour un point du graphique
 */
data class ChartData(
    val label: String,
    val value: Float,
    val color: Color? = null
)

/**
 * Graphique en barres animé
 * Affiche les heures travaillées par jour de la semaine
 */
@Composable
fun BarChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    barColor: Color = MaterialTheme.colorScheme.primary,
    showValues: Boolean = true
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val actualMaxValue = maxValue ?: data.maxOfOrNull { it.value } ?: 1f
    val safeMaxValue = max(actualMaxValue, 0.1f) // Éviter division par zéro

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp)
        ) {
            val barWidth = size.width / (data.size * 2f)
            val spacing = barWidth / 2f
            val chartHeight = size.height - 40.dp.toPx() // Laisser de l'espace pour les labels

            data.forEachIndexed { index, chartData ->
                val barHeight = (chartData.value / safeMaxValue) * chartHeight * animatedProgress.value
                val x = spacing + (index * (barWidth + spacing))
                val y = chartHeight - barHeight + 20.dp.toPx()

                // Dessiner la barre avec coins arrondis
                drawRoundRect(
                    color = chartData.color ?: barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }

        // Labels en dessous
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { chartData ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = chartData.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp
                    )
                    if (showValues && chartData.value > 0) {
                        Text(
                            text = "${chartData.value.toInt()}h",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Graphique en ligne animé
 * Affiche l'évolution du temps de travail sur une période
 */
@Composable
fun LineChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    showDots: Boolean = true,
    showValues: Boolean = false
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.value } ?: 1f
    val safeMaxValue = max(maxValue, 0.1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            val chartHeight = size.height - 40.dp.toPx()
            val chartWidth = size.width
            val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

            // Créer le path pour la ligne
            val path = Path()
            val points = mutableListOf<Offset>()

            data.forEachIndexed { index, chartData ->
                val x = index * stepX
                val y = chartHeight - (chartData.value / safeMaxValue) * chartHeight + 20.dp.toPx()
                points.add(Offset(x, y))

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Dessiner la ligne avec animation
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4.dp.toPx()),
                alpha = animatedProgress.value
            )

            // Dessiner les points
            if (showDots) {
                points.forEach { point ->
                    drawCircle(
                        color = lineColor,
                        radius = 6.dp.toPx(),
                        center = point,
                        alpha = animatedProgress.value
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = point,
                        alpha = animatedProgress.value
                    )
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { index, chartData ->
                if (index == 0 || index == data.size - 1 || data.size <= 7) {
                    Text(
                        text = chartData.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = if (index == 0) TextAlign.Start else if (index == data.size - 1) TextAlign.End else TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Prépare les données pour un graphique de la semaine
 * @param pointagesByDay Map des minutes travaillées par jour (DayOfWeek -> minutes)
 */
fun prepareWeekChartData(
    pointagesByDay: Map<DayOfWeek, Long>,
    barColor: Color
): List<ChartData> {
    return DayOfWeek.entries.map { day ->
        val minutes = pointagesByDay[day] ?: 0L
        val hours = (minutes / 60f)
        ChartData(
            label = day.getDisplayName(TextStyle.SHORT, Locale.FRENCH).take(3),
            value = hours,
            color = barColor
        )
    }
}

/**
 * Prépare les données pour un graphique mensuel (derniers 30 jours)
 * @param pointagesByDate Map des minutes travaillées par date
 */
fun prepareMonthChartData(
    pointagesByDate: Map<String, Long>,
    lineColor: Color
): List<ChartData> {
    return pointagesByDate.map { (date, minutes) ->
        val hours = (minutes / 60f)
        ChartData(
            label = date.takeLast(2), // Afficher juste le jour (ex: "23")
            value = hours,
            color = lineColor
        )
    }
}

/**
 * Mini graphique de tendance (sparkline)
 * Affiche une petite ligne de tendance dans une carte
 */
@Composable
fun TrendSparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it } ?: 1f
    val safeMaxValue = max(maxValue, 0.1f)

    Canvas(modifier = modifier.height(40.dp)) {
        val chartHeight = size.height
        val chartWidth = size.width
        val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = chartHeight - (value / safeMaxValue) * chartHeight

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
