package tokyo.isseikuzumaki.nlt.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.nlt.ui.state.AuthState
import tokyo.isseikuzumaki.nlt.ui.viewmodel.NLTViewModel
import tokyo.isseikuzumaki.shared.ui.atoms.AppButton
import tokyo.isseikuzumaki.shared.ui.atoms.AppText
import tokyo.isseikuzumaki.shared.ui.atoms.AppTextField

/**
 * Sign-in page for authentication
 */
@Composable
fun SignInPage(
    viewModel: NLTViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        AppText(
            text = "NLT",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        AppText(
            text = "通知と位置情報トラッカー",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Email field
        AppTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { AppText("Password") },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sign in button
        AppButton(
            text = if (authState is AuthState.Loading) "サインイン中..." else "サインイン",
            onClick = {
                scope.launch {
                    viewModel.signIn(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading && email.isNotBlank() && password.isNotBlank()
        )
        
        // Error message
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            AppText(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Help text
        AppText(
            text = "Firebase Authenticationでサインインしてください",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
