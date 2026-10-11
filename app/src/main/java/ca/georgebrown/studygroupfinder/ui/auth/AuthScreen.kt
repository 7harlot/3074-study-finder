package ca.georgebrown.studygroupfinder.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.georgebrown.studygroupfinder.data.repository.LocalAuthRepository
import ca.georgebrown.studygroupfinder.ui.components.AppTextField
import ca.georgebrown.studygroupfinder.ui.components.PrimaryButton
import ca.georgebrown.studygroupfinder.ui.components.SegmentedToggle
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/** Screen 2: Login / Sign Up with one toggle, validation, loading and error states. */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val isSignUp = state.mode == AuthMode.SIGN_UP

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "Campus Study Group Finder",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(Dimens.ItemSpacing))
        SegmentedToggle(
            options = listOf("Login", "Sign Up"),
            selectedIndex = if (isSignUp) 1 else 0,
            onSelect = { viewModel.onModeChange(if (it == 1) AuthMode.SIGN_UP else AuthMode.LOGIN) },
        )
        Spacer(Modifier.height(Dimens.ItemSpacing))

        if (isSignUp) {
            AppTextField(
                value = state.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = "Full name",
                leadingIcon = Icons.Outlined.Person,
                errorText = state.fullNameError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        AppTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "College email",
            placeholder = "name@georgebrown.ca",
            leadingIcon = Icons.Outlined.Email,
            errorText = state.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        AppTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Outlined.Lock,
            errorText = state.passwordError,
            supportingText = if (isSignUp) "At least 8 characters, letters and numbers" else null,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done,
            ),
        )
        if (isSignUp) {
            AppTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Confirm password",
                leadingIcon = Icons.Outlined.Lock,
                errorText = state.confirmPasswordError,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )
        }

        // Server-side style errors (wrong password, duplicate account). liveRegion makes TalkBack read it out.
        if (state.formError != null) {
            Text(
                state.formError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Spacer(Modifier.height(Dimens.ItemSpacing))
        PrimaryButton(
            text = if (isSignUp) "Create account" else "Continue",
            isLoading = state.isLoading,
            onClick = {
                focusManager.clearFocus()
                viewModel.submit()
            },
        )
        Spacer(Modifier.height(Dimens.ItemSpacing))
        Text(
            if (isSignUp) "Use your George Brown email. You'll stay signed in until you sign out."
            else "Demo account: ${LocalAuthRepository.DEMO_EMAIL} / ${LocalAuthRepository.DEMO_PASSWORD}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
