package com.crumbandember.app.ui.screens.consent

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.ConsentRepository
import com.crumbandember.app.ui.components.friendlyInlineMessage
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

/**
 * Asks whether Crumb & Ember can use the device's location — shown once,
 * right after login, when consent-service reports `granted == null` (never
 * asked before). Order and revisit it from Profile > Location access.
 *
 * Two separate yes/no's happen here, deliberately in this order:
 *  1. does the *person* consent (recorded in consent-service, survives
 *     reinstalls/device changes, is what "we asked and they said yes" means
 *     for compliance purposes)
 *  2. does *Android* grant the runtime permission (can be revoked in
 *     system Settings at any time without the app being told directly)
 * Saying yes to (1) immediately triggers the OS dialog for (2); saying no
 * skips the OS prompt entirely. If Android denies the permission after the
 * person said yes, the saved consent flag is corrected back to false so
 * the two never end up out of sync.
 */
@Composable
fun LocationConsentScreen(
    userId: String,
    consentRepository: ConsentRepository,
    onDone: () -> Unit
) {
    val viewModel: ConsentViewModel = viewModel(
        factory = ViewModelFactory { ConsentViewModel(userId, consentRepository) }
    )
    val saveState by viewModel.saveState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { osGranted ->
        // Keep consent-service's flag truthful even if the person allowed
        // in-app but then denied the system dialog (or vice versa isn't
        // possible — Android never grants without asking).
        viewModel.setConsent(osGranted)
    }

    LaunchedEffect(saveState) {
        if (saveState is Resource.Success) onDone()
    }

    fun requestLocation() {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            viewModel.setConsent(true)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📍", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Use your location?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Crumb & Ember can use your location to suggest the nearest bakery and give " +
                    "more accurate pickup and delivery times. You can change this any time in " +
                    "Account > Location access.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (saveState is Resource.Error) {
                val err = saveState as Resource.Error
                Spacer(Modifier.height(12.dp))
                Text(
                    friendlyInlineMessage(err.kind, err.message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { requestLocation() },
                enabled = saveState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saveState is Resource.Loading) "Saving…" else "Allow location access")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { viewModel.setConsent(false) },
                enabled = saveState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not now")
            }
        }
    }
}
