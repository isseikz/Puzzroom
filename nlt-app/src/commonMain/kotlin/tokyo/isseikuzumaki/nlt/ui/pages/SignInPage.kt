package tokyo.isseikuzumaki.nlt.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.nlt.ui.state.AuthState
import tokyo.isseikuzumaki.nlt.ui.viewmodel.NLTViewModel
import tokyo.isseikuzumaki.shared.ui.atoms.AppButton
import tokyo.isseikuzumaki.shared.ui.atoms.AppText

/**
 * Sign-in page for authentication with Google
 */
@Composable
fun SignInPage(
    viewModel: NLTViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    
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
        
        // Google Sign-in button
        AppButton(
            text = if (authState is AuthState.Loading) "サインイン中..." else "Googleでサインイン",
            onClick = {
                viewModel.signInWithGoogle()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
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
            text = "Googleアカウントでサインインしてください",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
