package com.crumbandember.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onRetry: () -> Unit
) {
    when (kind) {
        ErrorKind.OFFLINE -> OfflineScreen(onRetry)
        ErrorKind.GATEWAY_DOWN -> GatewayDownScreen(onRetry)
        ErrorKind.SERVICE_UNAVAILABLE -> ServiceUnavailableScreen(featureName, onRetry)
        ErrorKind.SERVER_ERROR -> ServerErrorScreen(onRetry)
        else -> ServiceUnavailableScreen(featureName, onRetry) // safe fallback, still friendly
    }
}

@Composable
private fun UnavailableScaffold(
    emoji: String,
    title: String,
    body: String,
    onRetry: () -> Unit,
    retryLabel: String = "Try again"
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text(retryLabel) }
        }
    }
}

/** Whole app is unreachable — api-gateway itself didn't respond. */
@Composable
fun GatewayDownScreen(onRetry: () -> Unit) {
    UnavailableScaffold(
        emoji = "🛠️",
        title = "Our app server is down",
        body = "We can't reach Crumb & Ember right now. This usually clears up in a few minutes — please try again shortly.",
        onRetry = onRetry
    )
}

/** api-gateway is up, but the one service behind this screen isn't. */
@Composable
fun ServiceUnavailableScreen(featureName: String, onRetry: () -> Unit) {
    UnavailableScaffold(
        emoji = "😕",
        title = "We're having trouble with $featureName",
        body = "Something's not working on our end right now — we're on it and it'll be fixed ASAP. The rest of the app should still work fine.",
        onRetry = onRetry
    )
}

/** Device has no network connection at all. */
@Composable
fun OfflineScreen(onRetry: () -> Unit) {
    UnavailableScaffold(
        emoji = "📡",
        title = "You're offline",
        body = "Check your Wi-Fi or mobile data and try again.",
        onRetry = onRetry
    )
}

/** api-gateway's own 500, not a downstream service. */
@Composable
fun ServerErrorScreen(onRetry: () -> Unit) {
    UnavailableScaffold(
        emoji = "⚠️",
        title = "Something went wrong on our end",
        body = "That wasn't supposed to happen. We've logged it — please try again in a moment.",
        onRetry = onRetry
    )
}
