package com.crumbandember.app.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crumbandember.app.data.model.ConsentRecord
import com.crumbandember.app.data.repository.AuthRepository
import com.crumbandember.app.data.repository.ConsentRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    consentRepository: ConsentRepository,
    onBack: () -> Unit,
    onViewOrders: () -> Unit,
    onManageLocationAccess: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val userName by authRepository.userName.collectAsState(initial = null)

    var consentState by remember { mutableStateOf<Resource<ConsentRecord>>(Resource.Loading) }
    // Re-checked on every composition of this screen, so a decision made on
    // the consent screen shows up here immediately on returning to it.
    LaunchedEffect(Unit) {
        val userId = authRepository.currentUserId() ?: ""
        consentState = consentRepository.getLocationConsent(userId)
    }

    val locationStatusText = when (val s = consentState) {
        is Resource.Success -> when (s.data.granted) {
            true -> "Allowed"
            false -> "Not allowed"
            null -> "Not asked yet"
        }
        else -> "…"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
            Text("Signed in as", style = MaterialTheme.typography.bodyMedium)
            Text(userName ?: "—", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onViewOrders, modifier = Modifier.fillMaxWidth()) {
                Text("Order history")
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onManageLocationAccess)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Location access", style = MaterialTheme.typography.titleMedium)
                    Text(locationStatusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Manage")
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { scope.launch { authRepository.logout(); onLoggedOut() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log out")
            }
        }
    }
}
