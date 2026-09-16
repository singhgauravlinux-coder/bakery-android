package com.crumbandember.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.AuthRepository
import com.crumbandember.app.ui.components.AnimatedButton
import com.crumbandember.app.ui.components.friendlyInlineMessage
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(factory = ViewModelFactory { AuthViewModel(authRepository) })
    val registerState by viewModel.registerState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }
    // TEMP: on-device diagnostics for the register-400 investigation. Remove
    // this along with Resource.Error.debugDetail once that's closed out.
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(registerState) {
        if (registerState is Resource.Success) onRegisterSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🌾", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Create Your Account", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Join us for fresh bakes, exclusive offers and more!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name (optional)") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min. 8 characters)") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Toggle password visibility")
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
            Text("I agree to the Terms & Conditions and Privacy Policy", style = MaterialTheme.typography.bodySmall)
        }

        if (registerState is Resource.Error) {
            val error = registerState as Resource.Error
            Spacer(Modifier.height(8.dp))
            Text(
                friendlyInlineMessage(error.kind, error.message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            if (error.debugDetail != null) {
                TextButton(onClick = { showDetails = true }) {
                    Text("View details")
                }
            }
        }

        if (showDetails && registerState is Resource.Error) {
            val detail = (registerState as Resource.Error).debugDetail.orEmpty()
            AlertDialog(
                onDismissRequest = { showDetails = false },
                confirmButton = {
                    TextButton(onClick = { showDetails = false }) { Text("Close") }
                },
                title = { Text("Request details") },
                text = {
                    SelectionContainer {
                        Text(detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }

        Spacer(Modifier.height(20.dp))
        AnimatedButton(
            text = "Create Account",
            onClick = { viewModel.register(email, password, name) },
            loading = registerState is Resource.Loading,
            enabled = agreed,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? ", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Login",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onNavigateToLogin)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
