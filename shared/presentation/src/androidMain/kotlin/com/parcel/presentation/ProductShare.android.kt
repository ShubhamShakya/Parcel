package com.parcel.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable internal actual fun rememberProductShare(text: String): () -> Unit {
    val context = LocalContext.current
    return remember(context, text) { {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share your Parcel find"))
    } }
}
