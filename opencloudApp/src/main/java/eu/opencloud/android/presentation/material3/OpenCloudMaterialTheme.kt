@file:Suppress("FunctionNaming")

package eu.opencloud.android.presentation.material3

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import eu.opencloud.android.R

/** Shared semantic colors for Compose controls and the XML Material 3 navigation bar. */
@Composable
fun OpenCloudMaterialTheme(content: @Composable () -> Unit) {
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val colors = base.copy(
        primary = colorResource(R.color.m3_primary),
        onPrimary = colorResource(R.color.m3_on_primary),
        primaryContainer = colorResource(R.color.m3_primary_container),
        onPrimaryContainer = colorResource(R.color.m3_on_primary_container),
        secondary = colorResource(R.color.m3_secondary),
        onSecondary = colorResource(R.color.m3_on_secondary),
        secondaryContainer = colorResource(R.color.m3_secondary_container),
        onSecondaryContainer = colorResource(R.color.m3_on_secondary_container),
        background = colorResource(R.color.m3_surface),
        onBackground = colorResource(R.color.m3_on_surface),
        surface = colorResource(R.color.m3_surface),
        onSurface = colorResource(R.color.m3_on_surface),
        surfaceVariant = colorResource(R.color.m3_surface_variant),
        onSurfaceVariant = colorResource(R.color.m3_on_surface_variant),
        outline = colorResource(R.color.m3_outline),
    )
    MaterialTheme(colorScheme = colors, content = content)
}
