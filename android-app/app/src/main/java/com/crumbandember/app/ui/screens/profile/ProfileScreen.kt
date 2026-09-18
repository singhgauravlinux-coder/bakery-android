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
import androidx.compose.ui.unit.sp
import com.crumbandember.app.data.model.ConsentRecord
import com.crumbandember.app.data.model.LoyaltyAccount
import com.crumbandember.app.data.repository.AuthRepository
import com.crumbandember.app.data.repository.ConsentRepository
import com.crumbandember.app.data.repository.LoyaltyRepository
import com.crumbandember.app.ui.components.BakeryBottomBar
import com.crumbandember.app.ui.components.BakeryDetailTopBar
import com.crumbandember.app.ui.components.BakeryTab
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    consentRepository: ConsentRepository,
    loyaltyRepository: LoyaltyRepository,
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
    // Previously this card was static markup ("350 Flour Points" / "₹50
    // discount unlocked") with no API call behind it at all, so it never
    // reflected what a user had actually earned. Wired to loyalty-service.
    var loyaltyState by remember { mutableStateOf<Resource<LoyaltyAccount>>(Resource.Loading) }
    LaunchedEffect(Unit) {
        val userId = authRepository.currentUserId() ?: ""
        consentState = consentRepository.getLocationConsent(userId)
        loyaltyState = loyaltyRepository.getAccount(userId)
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
                        BakeryTab.SEARCH -> onHomeClick()
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
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (userName?.firstOrNull()?.toString() ?: "👨‍🍳").uppercase(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(userName ?: "Artisan Baker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Crumb Club Member", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(20.dp))
            // Loyalty Points Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Crumb Club Rewards",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(4.dp))
                        when (val ls = loyaltyState) {
                            is Resource.Success -> {
                                Text(
                                    "${ls.data.points} Flour Points",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (ls.data.tier == "golden-crust")
                                        "₹50 discount unlocked on your next order"
                                    else
                                        "Earn ${200 - ls.data.points} more points to unlock a ₹50 discount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                            is Resource.Error -> Text(
                                "Points unavailable right now",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            else -> Text(
                                "Loading…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Text("🌾", fontSize = 36.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
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
