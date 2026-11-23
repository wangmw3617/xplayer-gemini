package com.xplayer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.xplayer.app.data.datastore.AppearancePrefs
import com.xplayer.app.ui.navigation.MainNavGraph
import com.xplayer.app.ui.navigation.Screen
import com.xplayer.app.ui.theme.XPlayerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var hasStoragePermission by mutableStateOf(false)

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkStoragePermission()
    }

    private val legacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            triggerInitialScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkStoragePermission()

        setContent {
            val app = application as XPlayerApp
            val appearancePrefs by app.preferencesRepository.appearancePrefsFlow.collectAsState(
                initial = AppearancePrefs()
            )

            XPlayerTheme(
                darkMode = appearancePrefs.darkMode,
                dynamicColor = appearancePrefs.dynamicColor,
                seedColorIndex = appearancePrefs.seedColorIndex
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasStoragePermission) {
                        val navController = rememberNavController()

                        LaunchedEffect(intent) {
                            handleIntent(intent, onNavigate = { path ->
                                navController.navigate(Screen.Player.createRoute(path))
                            })
                        }

                        MainNavGraph(navController = navController)
                    } else {
                        PermissionGuideScreen(
                            onRequestPermission = { requestStoragePermission() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkStoragePermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun checkStoragePermission() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (granted && !hasStoragePermission) {
            hasStoragePermission = true
            triggerInitialScan()
        } else {
            hasStoragePermission = granted
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                manageStorageLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        } else {
            legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun triggerInitialScan() {
        val app = application as XPlayerApp
        lifecycleScope.launch {
            app.mediaScanner.scanMedia()
        }
    }

    private fun handleIntent(intent: Intent?, onNavigate: (String) -> Unit) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val dataUri = intent.data ?: return
            val path = if (dataUri.scheme == "file") {
                dataUri.path
            } else if (dataUri.scheme == "http" || dataUri.scheme == "https") {
                dataUri.toString()
            } else {
                dataUri.toString()
            }
            if (!path.isNullOrEmpty()) {
                onNavigate(path)
            }
        }
    }
}

@Composable
fun PermissionGuideScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderSpecial,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = stringResource(R.string.permission_storage_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.permission_storage_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permission_grant))
                }
            }
        }
    }
}
