package com.crumbandember.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.AuthRepository
import com.crumbandember.app.ui.components.AnimatedButton
import com.crumbandember.app.ui.components.BakeryDetailTopBar
import com.crumbandember.app.ui.components.friendlyInlineMessage
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

@Composable
fun ForgotPasswordScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(factory = ViewModelFactory { AuthViewModel(authRepository) })
    val state by viewModel.forgotPasswordState.collectAsState()
    var email by remember { mutableStateOf("") }

    Scaffold(topBar = { BakeryDetailTopBar(title = "Forgot Password?", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            if (state is Resource.Success) {
                Spacer(Modifier.height(40.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📬", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(16.dp))
                    Text("Check your email", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        (state as Resource.Success<String>).data,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    AnimatedButton(text = "Back to Login", onClick = onBack, modifier = Modifier.fillMaxWidth())
                }
            } else {
                Text(
                    "Enter the email address you signed up with and we'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state is Resource.Error) {
                    val err = state as Resource.Error
                    Spacer(Modifier.height(8.dp))
                    Text(
                        friendlyInlineMessage(err.kind, err.message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(24.dp))
                AnimatedButton(
                    text = "Send Reset Link",
                    onClick = { viewModel.forgotPassword(email) },
                    loading = state is Resource.Loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
