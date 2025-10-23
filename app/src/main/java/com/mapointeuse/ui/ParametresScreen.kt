package com.mapointeuse.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.mapointeuse.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametresScreen(viewModel: ParametresViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var autoStart by remember { mutableStateOf(false) }
    var autoStop by remember { mutableStateOf(false) }
    var notifyOnEnter by remember { mutableStateOf(true) }
    var notifyOnExit by remember { mutableStateOf(true) }

    // Charger les données existantes
    LaunchedEffect(uiState.workPlace) {
        uiState.workPlace?.let { wp ->
            name = wp.name
            latitude = wp.latitude.toString()
            longitude = wp.longitude.toString()
            radius = wp.radiusMeters.toString()
            autoStart = wp.autoStart
            autoStop = wp.autoStop
            notifyOnEnter = wp.notifyOnEnter
            notifyOnExit = wp.notifyOnExit
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // Permission accordée, récupérer la position
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                location?.let {
                    latitude = it.latitude.toString()
                    longitude = it.longitude.toString()
                    viewModel.getAddressFromLocation(it.latitude, it.longitude)?.let { address ->
                        if (name.isEmpty()) name = address
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Messages
            uiState.successMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Carte principale
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lieu de travail",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom du lieu") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Place, null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Utiliser ma position actuelle")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = radius,
                        onValueChange = { radius = it },
                        label = { Text("Rayon de détection (mètres)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.RadioButtonChecked, null) }
                    )
                }
            }

            // Options de détection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Options de détection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = notifyOnEnter,
                            onCheckedChange = { notifyOnEnter = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notification à l'arrivée")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = notifyOnExit,
                            onCheckedChange = { notifyOnExit = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notification au départ")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Mode automatique (expérimental)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = autoStart,
                            onCheckedChange = { autoStart = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Démarrage automatique")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = autoStop,
                            onCheckedChange = { autoStop = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Arrêt automatique")
                    }
                }
            }

            // Boutons d'action
            Button(
                onClick = {
                    val lat = latitude.toDoubleOrNull() ?: 0.0
                    val lon = longitude.toDoubleOrNull() ?: 0.0
                    val rad = radius.toIntOrNull() ?: 100

                    viewModel.saveWorkPlace(
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        radiusMeters = rad,
                        autoStart = autoStart,
                        autoStop = autoStop,
                        notifyOnEnter = notifyOnEnter,
                        notifyOnExit = notifyOnExit
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotEmpty() && latitude.isNotEmpty() && longitude.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enregistrer")
            }

            if (uiState.workPlace != null) {
                OutlinedButton(
                    onClick = { viewModel.deleteWorkPlace() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Supprimer le lieu")
                }
            }
        }
    }
}
