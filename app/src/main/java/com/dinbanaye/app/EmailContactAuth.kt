package com.dinbanaye.app

import android.content.Context
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Separate preferences: never writes task storage, Room, or Firebase Auth identities.
internal fun saveContactPhone(context: Context, uid: String, phone: String): Boolean =
    context.getSharedPreferences("dinbanaye_contact_profiles", Context.MODE_PRIVATE)
        .edit().putString("$uid.phone", phone).commit()

internal fun normalizeContactPhone(raw: String): String? {
    val compact = raw.trim().replace(Regex("[\\s()\\-]"), "")
    return when {
        compact.matches(Regex("[6-9][0-9]{9}")) -> "+91$compact"
        compact.matches(Regex("\\+[1-9][0-9]{7,14}")) -> compact
        else -> null
    }
}

@Composable
fun AuthScreen(
    loading: Boolean,
    error: String,
    notice: String,
    onClearMessage: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSignup: (String, String, String) -> Unit,
    onForgotPassword: (String) -> Unit
) {
    var mode by remember { mutableStateOf("login") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    // Password is deliberately not saved across activity recreation.
    var password by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf("") }
    var cooldown by remember { mutableStateOf(0) }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) { delay(1000); cooldown -= 1 }
    }
    fun switchMode(next: String) {
        mode = next
        password = ""
        validation = ""
        onClearMessage()
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text("DinBanaye", style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary)
            Text(when (mode) {
                "signup" -> "Create account"
                "reset" -> "Reset password by email"
                else -> "Login with email"
            }, style = MaterialTheme.typography.titleLarge)
            if (mode == "signup") {
                OutlinedTextField(phone, { phone = it; validation = "" },
                    label = { Text("Contact phone number") },
                    supportingText = { Text("10-digit Indian number, or include country code (+...).") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true, enabled = !loading, modifier = Modifier.fillMaxWidth())
                Text("Saved with your account on this device only. This number is not verified and is not used to log in or recover a password.")
            }
            OutlinedTextField(email, { email = it; validation = ""; onClearMessage() },
                label = { Text("Email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, enabled = !loading, modifier = Modifier.fillMaxWidth())
            if (mode != "reset") {
                OutlinedTextField(password, { password = it; validation = "" },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true, enabled = !loading, modifier = Modifier.fillMaxWidth())
            } else {
                Text("Enter the email used to create your account. Your phone number alone cannot recover the account.")
                Text("1. Open Gmail and check Inbox.\n2. If missing, open Gmail menu → Spam. Check the sender, then choose Report not spam for the expected DinBanaye reset email.\n3. Open the latest reset link and set a new password.\n4. Return here and log in with email + new password.")
            }
            if (validation.isNotEmpty() || error.isNotEmpty()) {
                Text(validation.ifEmpty { error }, color = MaterialTheme.colorScheme.error)
            }
            if (notice.isNotEmpty()) Text(notice, color = MaterialTheme.colorScheme.primary)
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && (mode != "reset" || cooldown == 0),
                onClick = {
                    val address = email.trim()
                    val normalizedPhone = normalizeContactPhone(phone)
                    validation = when {
                        !Patterns.EMAIL_ADDRESS.matcher(address).matches() -> "Enter a valid email address."
                        mode == "signup" && normalizedPhone == null -> "Enter a valid phone number."
                        mode == "signup" && password.length < 6 -> "Use at least 6 characters for your password."
                        mode == "login" && password.isEmpty() -> "Enter your password."
                        else -> ""
                    }
                    if (validation.isEmpty()) {
                        onClearMessage()
                        when (mode) {
                            "signup" -> onSignup(address, password, normalizedPhone!!)
                            "reset" -> { cooldown = 60; onForgotPassword(address) }
                            else -> onLogin(address, password)
                        }
                    }
                }
            ) {
                Text(when {
                    loading -> "Please wait..."
                    mode == "reset" && cooldown > 0 -> "Try again in ${cooldown}s"
                    mode == "reset" -> "Send reset link"
                    mode == "signup" -> "Create account"
                    else -> "Login"
                })
            }
            if (mode == "login") {
                TextButton(enabled = !loading, onClick = { switchMode("reset") }) { Text("Forgot password?") }
                TextButton(enabled = !loading, onClick = { switchMode("signup") }) { Text("Create a new account") }
            } else {
                TextButton(enabled = !loading, onClick = { switchMode("login") }) { Text("Back to login") }
            }
            Text("No SMS is sent. Password reset uses your account email.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ContactProfileDialog(uid: String, email: String, verifiedPhone: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var phone by remember(uid) {
        mutableStateOf(context.getSharedPreferences("dinbanaye_contact_profiles", Context.MODE_PRIVATE)
            .getString("$uid.phone", verifiedPhone.orEmpty()).orEmpty())
    }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Email: $email")
                OutlinedTextField(phone, { phone = it; error = "" },
                    label = { Text("Contact phone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Text("Contact detail only, saved on this device. It does not verify phone ownership or change your login. Use your email for password recovery.")
                if (!verifiedPhone.isNullOrEmpty()) Text("Existing Firebase verified phone: $verifiedPhone")
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val normalized = normalizeContactPhone(phone)
                if (normalized == null) error = "Enter a valid phone number with country code."
                else if (saveContactPhone(context, uid, normalized)) onDismiss()
                else error = "Could not save. Please try again."
            }) { Text("Save contact") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
