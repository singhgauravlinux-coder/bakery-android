package com.crumbandember.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crumbandember.app.data.model.Product
import com.crumbandember.app.ui.theme.BakeryTokens
import java.util.Locale

/**
 * Deterministic decorative art for a product card in place of a real photo —
 * product-catalog-service carries no image asset today, so this derives a
 * stable gradient + emoji from the product's name/category instead of
 * pointing at a fake image URL.
 */
private data class ProductArt(val colors: List<Color>, val emoji: String)

private val CATEGORY_ART: Map<String, ProductArt> = mapOf(
    "cake" to ProductArt(listOf(Color(0xFF6B3A22), Color(0xFFD9752B)), "🍰"),
    "cakes" to ProductArt(listOf(Color(0xFF6B3A22), Color(0xFFD9752B)), "🍰"),
    "pastry" to ProductArt(listOf(Color(0xFF8C5A2E), Color(0xFFEFC988)), "🥐"),
    "pastries" to ProductArt(listOf(Color(0xFF8C5A2E), Color(0xFFEFC988)), "🥐"),
    "bread" to ProductArt(listOf(Color(0xFF7A5233), Color(0xFFC08A4E)), "🍞"),
    "cookies" to ProductArt(listOf(Color(0xFF9C6B45), Color(0xFFF0965A)), "🍪"),
    "cupcakes" to ProductArt(listOf(Color(0xFFB3547A), Color(0xFFF0965A)), "🧁"),
    "cupcake" to ProductArt(listOf(Color(0xFFB3547A), Color(0xFFF0965A)), "🧁"),
    "desserts" to ProductArt(listOf(Color(0xFF5C3A6B), Color(0xFFD9752B)), "🍮")
)
private val DEFAULT_ART = ProductArt(listOf(Color(0xFF3B2417), Color(0xFFD9752B)), "🥖")

private fun artFor(product: Product): ProductArt =
    CATEGORY_ART[product.category?.trim()?.lowercase(Locale.getDefault())] ?: DEFAULT_ART

fun formatPrice(price: Double): String = "₹${String.format(Locale.getDefault(), "%,.0f", price)}"

/**
 * Premium grid-style product card: gradient "photo" surface, favorite
 * toggle, name/category, price and a floating add-to-cart action with a
 * short press animation. Used across Home, Catalog/Search and Related
 * Products rails.
 */
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onQuickAdd: (() -> Unit)? = null
) {
    val art = remember(product.id) { artFor(product) }
    var favorited by remember(product.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .shadow(4.dp, RoundedCornerShape(22.dp), clip = false)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(art.colors))
        ) {
            Text(
                art.emoji,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.Center)
            )
            // Star rating pill
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("⭐", fontSize = 10.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "4.8",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            FavoriteButton(
                favorited = favorited,
                onToggle = { favorited = !favorited },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            product.category?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it.replaceFirstChar { c -> c.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    formatPrice(product.price),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable3D(onQuickAdd ?: onClick)
                ) {
                    Icon(
                        Icons.Filled.AddShoppingCart,
                        contentDescription = "Add to cart",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(6.dp).size(16.dp)
                    )
                }
            }
        }
    }
}

/** A wide, row-style variant used inside the cart and order lists. */
@Composable
fun ProductRow(product: Product, quantity: Int, onRemove: (() -> Unit)?, modifier: Modifier = Modifier) {
    val art = remember(product.id) { artFor(product) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(art.colors)),
            contentAlignment = Alignment.Center
        ) {
            Text(art.emoji, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "Qty $quantity  ·  ${formatPrice(product.price)} each",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(formatPrice(product.price * quantity), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Standard top bar: brand title, optional notification bell, badged cart icon, and profile avatar. */
@Composable
fun BakeryTopBar(
    title: String,
    cartCount: Int = 0,
    onCartClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌾", fontSize = 20.sp)
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        actions = {
            if (onNotificationsClick != null) {
                IconButton(onClick = onNotificationsClick) {
                    Box {
                        Icon(Icons.Filled.NotificationsNone, contentDescription = "Notifications")
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
            if (onCartClick != null) {
                BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                    IconButton(onClick = onCartClick) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                    }
                }
            }
            if (onProfileClick != null) {
                IconButton(onClick = onProfileClick) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨‍🍳", fontSize = 15.sp)
                    }
                }
            }
        }
    )
}

/** Back-navigation top bar used on pushed detail/flow screens (Cart, Checkout, Product, Orders...). */
@Composable
fun BakeryDetailTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions
    )
}
