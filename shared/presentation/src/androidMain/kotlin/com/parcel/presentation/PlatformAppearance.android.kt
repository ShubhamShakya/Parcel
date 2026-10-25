package com.parcel.presentation
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable internal actual fun PlatformAppearance(dark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(dark, view) {
        var context = view.context
        while (context is ContextWrapper && context !is Activity) context = context.baseContext
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            @Suppress("DEPRECATION")
            window.navigationBarColor = if (dark) android.graphics.Color.rgb(10, 15, 28) else android.graphics.Color.rgb(233, 237, 231)
        }
    }
}
