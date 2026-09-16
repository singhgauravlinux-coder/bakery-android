package com.crumbandember.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.crumbandember.app.ui.theme.BakeryTokens
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/**
 * A single promotional slide for the home hero carousel.
 */
data class HeroSlide(
    val title: String,
    val subtitle: String,
    val ctaLabel: String,
    val accentColors: List<Color>,
    val emoji: String
)

/**
 * A 3D rolling carousel: the centered card sits at full scale with the
 * neighbouring cards peeking in at reduced scale/alpha and a slight
 * rotation, mimicking a coverflow-style perspective. Auto-advances and
 * supports swipe; page indicator dots below.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Hero3DCarousel(
    slides: List<HeroSlide>,
    onCtaClick: (HeroSlide) -> Unit,
    modifier: Modifier = Modifier,
    autoScrollMillis: Long = 4200L
) {
    if (slides.isEmpty()) return
    val pageCount = slides.size
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(autoScrollMillis)
            val next = (pagerState.currentPage + 1) % pageCount
            runCatching { pagerState.animateScrollToPage(next, animationSpec = tween(600)) }
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 36.dp),
            pageSpacing = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) { page ->
            Hero3DCard(
                slide = slides[page],
                pageOffset = { (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction },
                onClick = { onCtaClick(slides[page]) }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pageCount) { i ->
                val selected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = if (selected) 20.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

/** A single hero card rendered with perspective-style scale/alpha/rotation driven by [pageOffset]. */
@Composable
fun Hero3DCard(
    slide: HeroSlide,
    pageOffset: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val offset = pageOffset().coerceIn(-1f, 1f)
                val scale = lerp(0.86f, 1f, 1f - offset.absoluteValue)
                scaleX = scale
                scaleY = scale
                rotationY = offset * -18f
                cameraDistance = 16f * density
                alpha = lerp(0.55f, 1f, 1f - offset.absoluteValue)
                translationX = -offset * 26f
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(slide.accentColors))
            .clickable3D(onClick)
            .padding(22.dp)
    ) {
        Text(
            slide.emoji,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                slide.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                slide.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White,
                modifier = Modifier.clickable3D(onClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(slide.ctaLabel, color = slide.accentColors.last(), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = slide.accentColors.last(), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * A decorative image surface that gently parallax-shifts a soft radial glow
 * based on a [progress] value (e.g. scroll offset) — used behind product
 * hero art in place of a real photo, since the product catalog contract
 * carries no image asset today.
 */
@Composable
fun ParallaxImage(
    colors: List<Color>,
    emoji: String,
    modifier: Modifier = Modifier,
    progress: Float = 0f
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.radialGradient(colors, center = Offset(0.5f + progress * 0.15f, 0.35f)))
    ) {
        Text(
            emoji,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationY = progress * 24f
                    scaleX = 3.2f
                    scaleY = 3.2f
                }
        )
    }
}

/** Press-responsive scale/elevation button with a loading state — the app's standard primary CTA. */
@Composable
fun AnimatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, animationSpec = spring(), label = "btnScale")

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interaction,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = modifier
            .height(52.dp)
            .scale(scale)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Heart toggle with a small pop animation on activation. */
@Composable
fun FavoriteButton(
    favorited: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (favorited) 1.15f else 1f, animationSpec = spring(), label = "favScale")
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.9f),
        modifier = modifier
            .size(36.dp)
            .clickable3D(onToggle)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (favorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (favorited) Color(0xFFD9752B) else Color(0xFF3B2417),
                modifier = Modifier.size(18.dp).scale(scale)
            )
        }
    }
}

/** Shimmering placeholder block shown while content loads. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

/** Full skeleton for the home/catalog product grid while the first load is in flight. */
@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(170.dp))
        Spacer(Modifier.height(20.dp))
        ShimmerBox(modifier = Modifier.width(140.dp).height(20.dp))
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                ShimmerBox(modifier = Modifier.weight(1f).height(190.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                ShimmerBox(modifier = Modifier.weight(1f).height(190.dp))
            }
        }
    }
}

/** One stage in an order's lifecycle timeline. */
data class TimelineStep(val label: String, val done: Boolean, val current: Boolean)

/** Horizontal animated progress timeline used on the order tracking / detail screen. */
@Composable
fun OrderTimeline(steps: List<TimelineStep>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { index, step ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(
                                    if (step.done || step.current) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    val dotColor = when {
                        step.done -> MaterialTheme.colorScheme.primary
                        step.current -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }
                    val dotScale by animateFloatAsState(if (step.current) 1.3f else 1f, label = "dotScale")
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(
                                    if (step.done) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    step.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (step.current) FontWeight.Bold else FontWeight.Normal,
                    color = if (step.done || step.current) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Bottom bar tab identifiers shared across Home/Search/Orders/Cart/Profile. */
enum class BakeryTab { HOME, ORDERS, CART, PROFILE }

@Composable
fun BakeryBottomBar(
    selected: BakeryTab,
    cartCount: Int,
    onSelect: (BakeryTab) -> Unit
) {
    NavigationBar(tonalElevation = 6.dp) {
        NavigationBarItem(
            selected = selected == BakeryTab.HOME,
            onClick = { onSelect(BakeryTab.HOME) },
            icon = { Icon(androidx.compose.material.icons.Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selected == BakeryTab.ORDERS,
            onClick = { onSelect(BakeryTab.ORDERS) },
            icon = { Icon(androidx.compose.material.icons.Icons.Filled.Receipt, contentDescription = "Orders") },
            label = { Text("Orders") }
        )
        NavigationBarItem(
            selected = selected == BakeryTab.CART,
            onClick = { onSelect(BakeryTab.CART) },
            icon = {
                BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.ShoppingCart, contentDescription = "Cart")
                }
            },
            label = { Text("Cart") }
        )
        NavigationBarItem(
            selected = selected == BakeryTab.PROFILE,
            onClick = { onSelect(BakeryTab.PROFILE) },
            icon = { Icon(androidx.compose.material.icons.Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

/** Section header with an optional "View all" trailing action, used throughout Home. */
@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** A gentle up/down float animation, used to give static illustrations subtle life without motion sickness risk. */
@Composable
fun rememberFloatBob(): State<Float> {
    val transition = rememberInfiniteTransition(label = "bob")
    return transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "bobValue"
    )
}

/** Tap target with a subtle press-scale, used for anything acting like a 3D button (cards, CTA chips). */
@Composable
fun Modifier.clickable3D(onClick: () -> Unit): Modifier = this.pointerInput(onClick) {
    detectTapGestures(onTap = { onClick() })
}
