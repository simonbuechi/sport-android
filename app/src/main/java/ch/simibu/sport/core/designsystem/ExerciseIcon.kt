package ch.simibu.sport.core.designsystem

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

object ExerciseIconResolver {
    @Volatile
    private var assetFiles: Set<String>? = null
    private val SLUG_REGEX = Regex("[^a-z0-9]+")

    fun getAssetPath(context: Context, iconUrl: String?, exerciseName: String): String? {
        val files = assetFiles ?: synchronized(this) {
            assetFiles ?: try {
                context.assets.list("exercises")?.toSet() ?: emptySet()
            } catch (e: Exception) {
                emptySet()
            }.also { assetFiles = it }
        }

        // 1. Try explicit iconUrl
        if (!iconUrl.isNullOrBlank()) {
            val clean = iconUrl.substringAfterLast('/').substringAfterLast('\\').trim()
            val webpName = if (clean.endsWith(".webp", ignoreCase = true)) {
                clean
            } else if (clean.contains('.')) {
                clean.substringBeforeLast('.') + ".webp"
            } else {
                "$clean.webp"
            }
            if (files.contains(webpName)) {
                return "file:///android_asset/exercises/$webpName"
            }
            if (files.contains(clean)) {
                return "file:///android_asset/exercises/$clean"
            }
        }

        // 2. Try slugified exerciseName
        val slug = exerciseName.trim().lowercase()
            .replace(SLUG_REGEX, "-")
            .trim('-') + ".webp"

        if (files.contains(slug)) {
            return "file:///android_asset/exercises/$slug"
        }

        return null
    }
}

@Composable
fun ExerciseIcon(
    iconUrl: String?,
    exerciseName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    iconTint: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val assetUri = remember(iconUrl, exerciseName) {
        ExerciseIconResolver.getAssetPath(context, iconUrl, exerciseName)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        if (assetUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(assetUri)
                    .crossfade(true)
                    .build(),
                contentDescription = exerciseName,
                modifier = Modifier
                    .size(size * 0.72f)
                    .padding(2.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(iconTint)
            )
        } else {
            val initial = exerciseName.firstOrNull { it.isLetterOrDigit() }?.uppercase()
            if (initial != null) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = exerciseName,
                    tint = iconTint,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}
