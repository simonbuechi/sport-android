package ch.simibu.sport.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Primary action button / FloatingActionButton with signature Sport color gradient (SportPurple -> SportRuby)
 * matching the web application's primary action button styling.
 */
@Composable
fun SportGradientFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    gradient: Brush = Brush.linearGradient(
        colors = listOf(SportPurple, SportRuby)
    ),
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val tactileColors = LocalTactileColors.current

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Button
        }
    } else {
        Modifier.semantics { this.role = Role.Button }
    }

    Box(
        modifier = modifier
            .then(semanticsModifier)
            .tactileClickable(onClick = onClick)
            .tactileShadow(
                shape = shape,
                elevation = TactileElevation.RAISED,
                highlightColor = tactileColors.highlight,
                shadowColor = tactileColors.shadow,
                spread = 3.dp
            )
            .clip(shape)
            .background(gradient)
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            content()
        }
    }
}
