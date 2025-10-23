package com.mapointeuse

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mapointeuse.data.AppDatabase
import com.mapointeuse.data.Pointage
import com.mapointeuse.data.PointageRepository
import com.mapointeuse.data.StatutPointage
import com.mapointeuse.ui.*
import com.mapointeuse.ui.theme.MaPointeuseTheme
import com.mapointeuse.utils.PermissionHelper
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class Screen(val route: String, val title: String) {
    object Pointage : Screen("pointage", "Pointage")
    object Statistiques : Screen("statistiques", "Statistiques")
    object Historique : Screen("historique", "Historique")
    object Parametres : Screen("parametres", "Paramètres")
    object EditPointage : Screen("edit_pointage/{pointageId}", "Modifier")

    fun createRoute(vararg args: Any): String {
        var route = this.route
        args.forEach { arg ->
            route = route.replaceFirst(Regex("\\{[^}]+\\}"), arg.toString())
        }
        return route
    }
}

private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds < 0) {
        Log.w("MaPointeuse", "formatDuration: Negative duration detected: $totalSeconds seconds")
    }
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return String.format(Locale.FRENCH, "%02d:%02d:%02d", hours, minutes, seconds)
}

class MainActivity : ComponentActivity() {

    private var permissionsGranted = false

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            permissionsGranted = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !PermissionHelper.hasBackgroundLocationPermission(this)
            ) {
                PermissionHelper.requestBackgroundLocationPermission(this)
            }
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installer le splash screen avant super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PointageRepository(database.pointageDao())

        checkAndRequestPermissions()

        setContent {
            MaPointeuseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(repository, intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionHelper.hasLocationPermissions(this)) {
            val permissions = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }

            requestPermissionsLauncher.launch(permissions.toTypedArray())
        } else {
            permissionsGranted = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !PermissionHelper.hasBackgroundLocationPermission(this)
            ) {
                PermissionHelper.requestBackgroundLocationPermission(this)
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.permissions_title)
            .setMessage(R.string.permissions_message)
            .setPositiveButton(R.string.settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

@Composable
fun MainScreen(repository: PointageRepository, intent: Intent?) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Gérer les actions provenant des notifications de geofencing
    LaunchedEffect(intent?.action) {
        when (intent?.action) {
            "START_WORK" -> {
                Log.d("MainActivity", "START_WORK action received from geofencing notification")
                navController.navigate(Screen.Pointage.route)
            }
            "STOP_WORK" -> {
                Log.d("MainActivity", "STOP_WORK action received from geofencing notification")
                navController.navigate(Screen.Pointage.route)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(id = R.string.cd_nav_pointage)) },
                    label = { Text(stringResource(id = R.string.pointage)) },
                    selected = currentRoute == Screen.Pointage.route,
                    onClick = {
                        navController.navigate(Screen.Pointage.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Assessment, contentDescription = stringResource(id = R.string.cd_nav_statistiques)) },
                    label = { Text(stringResource(id = R.string.statistiques)) },
                    selected = currentRoute == Screen.Statistiques.route,
                    onClick = {
                        navController.navigate(Screen.Statistiques.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = stringResource(id = R.string.cd_nav_historique)) },
                    label = { Text(stringResource(id = R.string.historique)) },
                    selected = currentRoute == Screen.Historique.route,
                    onClick = {
                        navController.navigate(Screen.Historique.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Navigation vers les paramètres") },
                    label = { Text("Paramètres") },
                    selected = currentRoute == Screen.Parametres.route,
                    onClick = {
                        navController.navigate(Screen.Parametres.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Pointage.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Pointage.route) {
                val vm: PointageViewModel = viewModel(
                    factory = PointageViewModelFactory(repository, context)
                )
                PointageScreen(viewModel = vm)
            }

            composable(Screen.Statistiques.route) {
                val vm: StatistiquesViewModel = viewModel(
                    factory = StatistiquesViewModelFactory(repository)
                )
                StatistiquesScreen(
                    viewModel = vm,
                    onPointageClick = { pointageId ->
                        navController.navigate(Screen.EditPointage.createRoute(pointageId))
                    }
                )
            }

            composable(Screen.Historique.route) {
                val vm: HistoriqueViewModel = viewModel(
                    factory = HistoriqueViewModelFactory(repository)
                )
                HistoriqueScreen(
                    viewModel = vm,
                    onPointageClick = { pointageId ->
                        navController.navigate(Screen.EditPointage.createRoute(pointageId))
                    }
                )
            }

            composable(Screen.Parametres.route) {
                val vm: ParametresViewModel = viewModel(
                    factory = ParametresViewModelFactory(context)
                )
                ParametresScreen(viewModel = vm)
            }

            composable(
                route = Screen.EditPointage.route,
                arguments = listOf(navArgument("pointageId") { type = NavType.LongType })
            ) { backStackEntry ->
                val pointageId = backStackEntry.arguments?.getLong("pointageId") ?: 0L
                val vm: EditPointageViewModel = viewModel(
                    factory = EditPointageViewModelFactory(repository, pointageId)
                )
                EditPointageScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointageScreen(viewModel: PointageViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val sessions = uiState.pointagesJour
    val activePointage = uiState.pointageActuel
    val referencePointage = activePointage ?: sessions.firstOrNull()
    val totalMinutesJour = uiState.totalMinutesJour
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var showSessions by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

                // Erreurs
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearError() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.cd_dismiss_error),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Grande carte centrale avec chronomètre
                SimplifiedStatusCard(
                    referencePointage = referencePointage,
                    sessionDurationSeconds = uiState.sessionDurationSeconds,
                    totalMinutesJour = totalMinutesJour
                )

                // Boutons d'action principaux
                ActionButtons(
                    viewModel = viewModel,
                    uiState = uiState
                )

                // Résumé du jour (collapsible)
                if (sessions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSessions = !showSessions },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.sessions_du_jour),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (showSessions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            if (showSessions) {
                                Spacer(modifier = Modifier.height(12.dp))
                                sessions.forEach { pointage ->
                                    val isActive = pointage.id == activePointage?.id
                                    val liveDuration = if (isActive) uiState.sessionDurationSeconds else pointage.tempsTravailleMinutes * 60
                                    CompactSessionItem(
                                        pointage = pointage,
                                        timeFormatter = timeFormatter,
                                        isActive = isActive,
                                        liveDurationSeconds = liveDuration
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Overlay de chargement
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Nouveaux composables simplifiés
@Composable
private fun SimplifiedStatusCard(
    referencePointage: Pointage?,
    sessionDurationSeconds: Long,
    totalMinutesJour: Long
) {
    val statusIcon = when (referencePointage?.statut) {
        StatutPointage.EN_COURS -> Icons.Default.PlayArrow
        StatutPointage.EN_PAUSE -> Icons.Default.Pause
        StatutPointage.TERMINE -> Icons.Default.CheckCircle
        null -> Icons.Default.AccessTime
    }
    val statusTint = when (referencePointage?.statut) {
        StatutPointage.EN_COURS -> MaterialTheme.colorScheme.primary
        StatutPointage.EN_PAUSE -> MaterialTheme.colorScheme.secondary
        StatutPointage.TERMINE -> MaterialTheme.colorScheme.tertiary
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (referencePointage?.statut) {
        StatutPointage.EN_COURS -> stringResource(id = R.string.status_en_cours)
        StatutPointage.EN_PAUSE -> stringResource(id = R.string.status_en_pause)
        StatutPointage.TERMINE -> stringResource(id = R.string.status_termine)
        null -> stringResource(id = R.string.status_pas_de_pointage)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (referencePointage?.statut) {
                StatutPointage.EN_COURS -> MaterialTheme.colorScheme.primaryContainer
                StatutPointage.EN_PAUSE -> MaterialTheme.colorScheme.secondaryContainer
                StatutPointage.TERMINE -> MaterialTheme.colorScheme.tertiaryContainer
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icône de statut
            Icon(
                imageVector = statusIcon,
                contentDescription = stringResource(id = R.string.cd_status_icon),
                modifier = Modifier.size(64.dp),
                tint = statusTint
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statut
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Chronomètre géant
            Text(
                text = formatDuration(sessionDurationSeconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = statusTint,
                fontSize = 56.sp
            )

            // Total du jour
            if (totalMinutesJour > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth(0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                val hours = totalMinutesJour / 60
                val minutes = totalMinutesJour % 60
                Text(
                    text = stringResource(id = R.string.temps_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${hours}h ${minutes}min",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    viewModel: PointageViewModel,
    uiState: PointageUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bouton Démarrer
        if (viewModel.canStartWork()) {
            Button(
                onClick = { viewModel.startWork() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(id = R.string.cd_start_work),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.commencer_journee),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Boutons Pause / Reprendre
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.canStartPause()) {
                Button(
                    onClick = { viewModel.startPause() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = stringResource(id = R.string.cd_pause_work),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.bouton_pause),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (viewModel.canEndPause()) {
                Button(
                    onClick = { viewModel.endPause() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(id = R.string.cd_resume_work),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.bouton_reprendre),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Bouton Terminer
        if (viewModel.canEndWork()) {
            Button(
                onClick = { viewModel.endWork() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(id = R.string.cd_end_work),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.terminer_journee),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CompactSessionItem(
    pointage: Pointage,
    timeFormatter: DateTimeFormatter,
    isActive: Boolean,
    liveDurationSeconds: Long
) {
    val statusIcon = when (pointage.statut) {
        StatutPointage.EN_COURS -> Icons.Default.PlayArrow
        StatutPointage.EN_PAUSE -> Icons.Default.Pause
        StatutPointage.TERMINE -> Icons.Default.CheckCircle
    }
    val statusTint = when (pointage.statut) {
        StatutPointage.EN_COURS -> MaterialTheme.colorScheme.primary
        StatutPointage.EN_PAUSE -> MaterialTheme.colorScheme.secondary
        StatutPointage.TERMINE -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = stringResource(id = R.string.cd_status_icon),
                tint = statusTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "${pointage.heureDebut.format(timeFormatter)} - ${pointage.heureFin?.format(timeFormatter) ?: "..."}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val durationSeconds = if (isActive) liveDurationSeconds else pointage.tempsTravailleMinutes * 60
        Text(
            text = formatDuration(durationSeconds),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = statusTint
        )
    }
}
