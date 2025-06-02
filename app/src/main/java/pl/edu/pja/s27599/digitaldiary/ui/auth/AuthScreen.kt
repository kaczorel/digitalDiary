package pl.edu.pja.s27599.digitaldiary.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pl.edu.pja.s27599.digitaldiary.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit
) {
    val isAuthenticated by viewModel.isAuthenticated
    val showSetupPinDialog by viewModel.showSetupPinDialog

    if (isAuthenticated) {
        onAuthenticated()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.enter_pin),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = viewModel.pinInput.value,
            onValueChange = { newValue ->
                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                    viewModel.pinInput.value = newValue
                    viewModel.clearError()
                }
            },
            label = { Text(stringResource(R.string.enter_pin)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = viewModel.setupPinError.value != null
        )

        viewModel.setupPinError.value?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.verifyPin() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.enter_pin))
        }

        if (showSetupPinDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.setup_pin)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = viewModel.pinInput.value,
                            onValueChange = { newValue ->
                                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                    viewModel.pinInput.value = newValue
                                    viewModel.clearError()
                                }
                            },
                            label = { Text(stringResource(R.string.setup_pin)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            isError = viewModel.setupPinError.value != null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = viewModel.setupPinConfirmation.value,
                            onValueChange = { newValue ->
                                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                    viewModel.setupPinConfirmation.value = newValue
                                    viewModel.clearError()
                                }
                            },
                            label = { Text(stringResource(R.string.confirm_pin)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            isError = viewModel.setupPinError.value != null
                        )
                        viewModel.setupPinError.value?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.setPin() }) {
                        Text(stringResource(R.string.set_pin))
                    }
                }
            )
        }
    }
}


