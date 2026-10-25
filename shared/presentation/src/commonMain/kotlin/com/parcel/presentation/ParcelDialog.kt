package com.parcel.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Shared popup shell for confirmations, notices and editable forms in both appearances. */
@Composable fun ParcelDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(28.dp), color = ParcelSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("parcel_dialog")) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                        ParcelFriend(Modifier.size(48.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismissRequest) { Symbol(Glyph.Close, "Close popup") }
                }
                ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() }
                Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    dismissButton?.invoke()
                    Spacer(Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}
