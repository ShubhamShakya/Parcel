package com.parcel.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/** Desktop test target has no native share chooser; copy the same description. */
@Composable internal actual fun rememberProductShare(text: String): () -> Unit {
    val clipboard = LocalClipboardManager.current
    return { clipboard.setText(AnnotatedString(text)) }
}
