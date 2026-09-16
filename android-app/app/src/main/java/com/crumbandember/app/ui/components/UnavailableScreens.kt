package com.crumbandember.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crumbandember.app.util.ErrorKind

/**
 * One place that decides which full-screen state to show for a failed
 * Resource.Error, so every screen (Catalog, Cart, Checkout, Orders, ...)
 * reacts to "the gateway is down" or "this one feature is down" the same
 * way instead of each screen inventing its own copy.
 *
 * `featureName` is what shows up in the "we're facing an issue with X"
 * copy for SERVICE_UNAVAILABLE — pass something a customer recognizes
 * ("your cart", "checkout", "order history"), not a service's internal name.
 */
/**
 * For inline, non-blocking failures (login, register, place-order — anything
 * where swapping in a full UnavailableScreen would wipe form input the
 * person already typed). Falls back to the server's own message for
 * anything that isn't one of the outage kinds.
 */
fun friendlyInlineMessage(kind: ErrorKind, serverMessage: String): String = when (kind) {
    ErrorKind.GATEWAY_DOWN -> "Our app server is down right now — please try again shortly."
    ErrorKind.SERVICE_UNAVAILABLE -> "We're having trouble with this right now. We're on it — please try again in a moment."
    ErrorKind.OFFLINE -> "You're offline — check your connection and try again."
    ErrorKind.SERVER_ERROR -> "Something went wrong on our end. Please try again."
    else -> serverMessage
}

@Composable
fun UnavailableScreen(
    kind: ErrorKind,
    featureName: String,
    onRetry: () -> Unit,
    onGoHome: (() -> Unit)? = null
) {
    when (kind) {
        ErrorKind.OFFLINE -> OfflineScreen(onRetry, onGoHome)
        ErrorKind.GATEWAY_DOWN -> GatewayDownScreen(onRetry, onGoHome)
        ErrorKind.SERVICE_UNAVAILABLE -> ServiceUnavailableScreen(featureName, onRetry, onGoHome)
        ErrorKind.SERVER_ERROR -> ServerErrorScreen(onRetry, onGoHome)
        else -> ServiceUnavailableScreen(featureName, onRetry, onGoHome) // safe fallback, still friendly
    }
}

/** A small, hand-drawn-feeling chef/bread mark built from vector shapes (no raster assets), with a
 *  gentle idle float so branded error states feel alive rather than static clip-art. */
@Composable
private fun BakeryIllustration(accent: Color, badge: String) {
    val bob by rememberFloatBob()
    Box(
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer { translationY = bob },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.75f)))),
            contentAlignment = Alignment.Center
        ) {
            Text("👨‍🍳", fontSize = 34.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(badge, fontSize = 16.sp)
        }
    }
}

@Composable
private fun UnavailableScaffold(
    badgeEmoji: String,
    accent: Color,
    title: String,
    body: String,
    errorCode: String? = null,
    onRetry: () -> Unit,
    onGoHome: (() -> Unit)?,
    retryLabel: String = "Try Again"
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BakeryIllustration(accent = accent, badge = badgeEmoji)
            Spacer(Modifier.height(20.dp))
            Text(
                "Oops!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (errorCode != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Error Code", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(errorCode, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            AnimatedButton(text = retryLabel, onClick = onRetry, modifier = Modifier.fillMaxWidth(0.7f))
            if (onGoHome != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onGoHome) { Text("Go to Home") }
            }
        }
    }
}

/** Whole app is unreachable — api-gateway itself didn't respond. */
@Composable
fun GatewayDownScreen(onRetry: () -> Unit, onGoHome: (() -> Unit)? = null) {
    UnavailableScaffold(
        badgeEmoji = "🛠️",
        accent = Color(0xFFD9752B),
        title = "Bad Gateway",
        body = "The server is temporarily unavailable. Please try again in a little while.",
        errorCode = "502",
        onRetry = onRetry,
        onGoHome = onGoHome
    )
}

/** api-gateway is up, but the one service behind this screen isn't. */
@Composable
fun ServiceUnavailableScreen(featureName: String, onRetry: () -> Unit, onGoHome: (() -> Unit)? = null) {
    UnavailableScaffold(
        badgeEmoji = "⏳",
        accent = Color(0xFF7A5233),
        title = "Service Unavailable",
        body = "We're currently having trouble with $featureName. We're on it — please try again in a moment. The rest of the app should still work fine.",
        errorCode = "503",
        onRetry = onRetry,
        onGoHome = onGoHome
    )
}

/** Device has no network connection at all. */
@Composable
fun OfflineScreen(onRetry: () -> Unit, onGoHome: (() -> Unit)? = null) {
    UnavailableScaffold(
        badgeEmoji = "📡",
        accent = Color(0xFF6F8F5B),
        title = "You're Offline",
        body = "Check your Wi-Fi or mobile data and try again.",
        onRetry = onRetry,
        onGoHome = onGoHome
    )
}

/** api-gateway's own 500, not a downstream service. */
@Composable
fun ServerErrorScreen(onRetry: () -> Unit, onGoHome: (() -> Unit)? = null) {
    UnavailableScaffold(
        badgeEmoji = "⚠️",
        accent = Color(0xFFB3261E),
        title = "Something Went Wrong",
        body = "We're experiencing a temporary issue on our server. Please try again later.",
        errorCode = "500",
        onRetry = onRetry,
        onGoHome = onGoHome
    )
}
