package com.crumbandember.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crumbandember.app.data.model.ConsentRecord
import com.crumbandember.app.data.repository.AuthRepository
import com.crumbandember.app.data.repository.ConsentRepository
import com.crumbandember.app.ui.components.BakeryBottomBar
import com.crumbandember.app.ui.components.BakeryDetailTopBar
import com.crumbandember.app.ui.components.BakeryTab
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    consentRepository: ConsentRepository,
    onBack: () -> Unit,
    onViewOrders: () -> Unit,
    onManageLocationAccess: () -> Unit,
    onLoggedOut: () -> Unit,
    cartCount: Int = 0,
    onHomeClick: () -> Unit = onBack,
    onCartClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val userName by authRepository.userName.collectAsState(initial = null)

    var consentState by remember { mutableStateOf<Resource<ConsentRecord>>(Resource.Loading) }
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
        topBar = { BakeryDetailTopBar(title = "Account", onBack = onBack) },
        bottomBar = {
            BakeryBottomBar(
                selected = BakeryTab.PROFILE,
                cartCount = cartCount,
                onSelect = { tab ->
                    when (tab) {
                        BakeryTab.HOME -> onHomeClick()
                        BakeryTab.ORDERS -> onViewOrders()
                        BakeryTab.CART -> onCartClick()
                        BakeryTab.PROFILE -> {}
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 20.dp).fillMaxSize()) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text((userName?.firstOrNull() ?: '🥐').toString().uppercase(), style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(userName ?: "Guest baker", style = MaterialTheme.typography.titleLarge)
                    Text("Signed in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Account", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            ProfileRow(icon = Icons.Filled.Receipt, title = "Order history", subtitle = "View past & current orders", onClick = onViewOrders)
            ProfileRow(
                icon = Icons.Filled.LocationOn,
                title = "Location access",
                subtitle = locationStatusText,
                onClick = onManageLocationAccess
            )

            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { authRepository.logout(); onLoggedOut() } }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Text("Log out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = "Manage")
    }
}
