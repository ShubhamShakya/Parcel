package com.parcel.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.parcel.domain.*
import kotlinx.coroutines.delay
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(state: AuthState, vm: AuthViewModel) {
    val challenge = state.snapshot.challenge
    var countryIso by rememberSaveable { mutableStateOf("IN") }
    var number by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable(challenge?.phone, challenge?.resendAt) { mutableStateOf("") }
    var first by rememberSaveable(challenge?.phone) { mutableStateOf("") }
    var last by rememberSaveable(challenge?.phone) { mutableStateOf("") }
    var selectCountry by rememberSaveable { mutableStateOf(false) }
    val country = dialCountries.first { it.iso == countryIso }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp).testTag("auth_screen"), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("parcel", color = Teal, style = MaterialTheme.typography.headlineLarge)
                Text("Your little world of happy finds", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            ParcelFriend(Modifier.size(104.dp), greeting = true)
        }
        Surface(color = Lavender, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(when { challenge == null -> "Hello, little shopper!"; challenge.verified -> "Let’s make it yours"; else -> "A tiny code, a big hello" }, style = MaterialTheme.typography.headlineMedium)
                Text(when { challenge == null -> "Log in or sign up with your phone number."; challenge.verified -> "You’re new here! What should we call you?"; else -> "Enter your 4-character OTP for ${challenge.phone}." }, color = Muted)
            }
        }
        when {
            challenge == null -> {
                OutlinedButton(onClick = { selectCountry = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("auth_country")) { Text("${countryFlag(country.iso)} ${country.name} (${country.dialCode})  ▾") }
                OutlinedTextField(number, { number = it }, label = { Text("Phone number") }, prefix = { Text("${country.dialCode} ") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    supportingText = { Text(if (number.isNotEmpty() && phoneError(number) != null) "Enter a valid phone number" else "Enter your phone number without the country code") },
                    isError = number.isNotEmpty() && phoneError(number) != null, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("auth_phone"))
                Button(onClick = { vm.request(countryIso, number) }, enabled = !state.busy && phoneError(number) == null, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("auth_request")) { Text(if (state.busy) "One little moment…" else "Continue with phone") }
                Text("Use a fictional number. This simulator accepts any 4-character OTP and doesn’t send SMS.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            !challenge.verified -> {
                OtpBoxes(code, { code = it }, enabled = !state.busy)
                Button(onClick = { vm.verify(code) }, enabled = !state.busy && otpError(code) == null, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("auth_verify")) { Text(if (state.busy) "Checking your code…" else "Verify & continue") }
                OtpResend(challenge, state.busy, vm::resend)
                TextButton(onClick = vm::changePhone, enabled = !state.busy, modifier = Modifier.testTag("auth_change_phone")) { Text("Change phone number") }
            }
            else -> {
                Text("Phone verified · ${challenge.phone}", color = Teal, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(first, { first = it }, label = { Text("First name") }, singleLine = true, enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth().testTag("auth_first_name"))
                OutlinedTextField(last, { last = it }, label = { Text("Last name") }, singleLine = true, enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth().testTag("auth_last_name"))
                if (first.length > 50 || last.length > 50) Text("Keep each name to 50 characters or fewer.", color = MaterialTheme.colorScheme.error)
                Button(onClick = { vm.register(first, last) }, enabled = !state.busy && nameError(first, last) == null, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("auth_register")) { Text(if (state.busy) "Making room for you…" else "Create my profile") }
                TextButton(onClick = vm::changePhone, enabled = !state.busy) { Text("Use a different number") }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("auth_error")) }
        Text("Profiles and shopping are saved on this device. This is a playful local login, not a real phone verification service.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
    if (selectCountry) {
        var query by rememberSaveable { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { selectCountry = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxHeight(.85f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose your country code", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(query, { query = it }, placeholder = { Text("Search country or code") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("country_search"))
                val matches = dialCountries.filter { query.isBlank() || it.name.contains(query.trim(), true) || it.dialCode.contains(query.trim()) }
                if (matches.isEmpty()) Text("No matching country. Try a country name or dial code.")
                LazyColumn(Modifier.weight(1f).testTag("country_list"), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(matches, key = { it.iso }) { choice ->
                        TextButton(onClick = { countryIso = choice.iso; selectCountry = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("country_${choice.iso}")) {
                            Text("${countryFlag(choice.iso)}  ${choice.name}", Modifier.weight(1f)); Text(choice.dialCode)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpResend(challenge: OtpChallenge, busy: Boolean, resend: () -> Unit) {
    var now by remember(challenge.resendAt) { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(challenge.resendAt) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            if (now >= challenge.resendAt) break
            delay(100)
        }
    }
    val Teal = Teal
    val remaining = challenge.remainingSeconds(now)
    if (remaining > 0) {
        val fraction = 1f - ((challenge.resendAt - now).toFloat() / OTP_RESEND_DELAY_MS).coerceIn(0f, 1f)
        Surface(color = Mint, shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp).testTag("otp_countdown"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Teal.copy(alpha = .12f), style = Stroke(4.dp.toPx()))
                        drawArc(Teal, -90f, 360f * fraction, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Text("$remaining", color = Teal, style = MaterialTheme.typography.titleLarge)
                }
                Column { Text("A little pause…", style = MaterialTheme.typography.titleMedium); Text("Resend OTP in ${remaining}s", style = MaterialTheme.typography.bodySmall, color = Muted) }
            }
        }
    } else TextButton(onClick = resend, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("auth_resend")) { Text("Resend OTP") }
}

// Regional indicator pairs render as country flags on supported platform fonts.
private fun countryFlag(iso: String): String = buildString {
    iso.uppercase().forEach { letter ->
        val point = 0x1F1E6 + (letter - 'A')
        append(((point - 0x10000) / 0x400 + 0xD800).toChar())
        append(((point - 0x10000) % 0x400 + 0xDC00).toChar())
    }
}

@Composable
private fun OtpBoxes(code: String, onChange: (String) -> Unit, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Verification code", style = MaterialTheme.typography.titleSmall)
        BasicTextField(value = code, onValueChange = { value ->
            onChange(value.filterNot { it.isWhitespace() }.take(4))
        }, enabled = enabled, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().testTag("auth_otp").semantics { contentDescription = "4-character verification code" },
            decorationBox = { innerTextField ->
                Box {
                    // Keep one real input for paste, deletion, keyboard and accessibility.
                    Box(Modifier.matchParentSize().alpha(0f)) { innerTextField() }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(4) { index ->
                            Box(Modifier.weight(1f).height(64.dp)
                                .background(if (index < code.length) Mint else MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                .border(if (index == code.length) 2.dp else 1.dp, if (index == code.length) Teal else Muted.copy(alpha = .35f), RoundedCornerShape(16.dp))
                                .testTag("otp_box_$index"), contentAlignment = Alignment.Center) {
                                Text(code.getOrNull(index)?.toString() ?: "", style = MaterialTheme.typography.headlineMedium, color = Teal)
                            }
                        }
                    }
                }
            })
        Text("Any four non-space characters work here.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
