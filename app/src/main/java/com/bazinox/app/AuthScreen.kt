package com.bazinox.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var signedEmail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { signedEmail = SupabaseAuthRepository.savedEmail(context) }

    Column(
        modifier = modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("حساب کاربری بازارینو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (signedEmail != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("وارد حساب شده‌اید")
                    Text(signedEmail.orEmpty(), fontWeight = FontWeight.Bold)
                    Text("نشست ورود روی این دستگاه ذخیره شده است.")
                    OutlinedButton(onClick = {
                        SupabaseAuthRepository.signOut(context)
                        signedEmail = null
                        message = "از حساب خارج شدی."
                    }) { Text("خروج از حساب") }
                }
            }
        } else {
            Text(if (isSignUp) "ساخت حساب جدید با ایمیل" else "ورود با ایمیل")
            if (isSignUp) {
                OutlinedTextField(
                    value = displayName, onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("نام نمایشی") },
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it.trim() },
                modifier = Modifier.fillMaxWidth(), label = { Text("ایمیل") },
                singleLine = true
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("رمز عبور (حداقل ۶ نویسه)") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && email.contains("@") && password.length >= 6 &&
                    (!isSignUp || displayName.isNotBlank()),
                onClick = {
                    busy = true
                    message = ""
                    scope.launch {
                        try {
                            val result = if (isSignUp) {
                                SupabaseAuthRepository.signUp(context, email, password, displayName.trim())
                            } else {
                                SupabaseAuthRepository.signIn(context, email, password)
                            }
                            message = result.message
                            if (result.signedIn) signedEmail = email
                        } catch (e: Exception) {
                            message = e.message ?: "ارتباط برقرار نشد؛ اینترنت را بررسی کن."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) { Text(if (busy) "لطفاً صبر کن…" else if (isSignUp) "ثبت‌نام" else "ورود") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                onClick = { isSignUp = !isSignUp; message = "" }
            ) { Text(if (isSignUp) "قبلاً حساب دارم؛ ورود" else "حساب ندارم؛ ثبت‌نام") }
        }

        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        Text(
            "برای ثبت‌نام، ایمیل معتبر لازم است. با فعال بودن تأیید ایمیل، ابتدا لینک تأیید را از صندوق ورودی باز کن.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
