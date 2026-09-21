package pl.lab512.puls512.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF071018)
val Surface = Color(0xFF0D1923)
val SurfaceRaised = Color(0xFF132431)
val Ice = Color(0xFF73DBFF)
val IceSoft = Color(0xFFB8EEFF)
val Mint = Color(0xFF69E1BA)
val White = Color(0xFFF4FAFD)
val Muted = Color(0xFF93A7B4)
val Border = Color(0xFF213846)
val Coral = Color(0xFFFF8D78)

private val PulsColors = darkColorScheme(
    primary = Ice,
    onPrimary = Ink,
    primaryContainer = Color(0xFF123849),
    onPrimaryContainer = IceSoft,
    secondary = Mint,
    onSecondary = Ink,
    background = Ink,
    onBackground = White,
    surface = Surface,
    onSurface = White,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Muted,
    outline = Border,
    error = Coral
)

@Composable
fun PulsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PulsColors, content = content)
}
