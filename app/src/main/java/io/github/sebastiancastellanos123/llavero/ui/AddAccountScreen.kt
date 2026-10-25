package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.sebastiancastellanos123.llavero.R
import io.github.sebastiancastellanos123.llavero.otp.HashAlgorithm
import io.github.sebastiancastellanos123.llavero.otp.Otp
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onSave: (OtpAccount) -> Unit,
    onBack: () -> Unit,
    scannedForm: AccountForm? = null,
) {
    // remember y no rememberSaveable: rememberSaveable guardaría la clave secreta
    // en el estado que Android escribe a disco cuando cierra la app en segundo plano.
    var form by remember(scannedForm) { mutableStateOf(scannedForm ?: AccountForm()) }
    var errors by remember { mutableStateOf(FormErrors()) }
    var showSecret by remember { mutableStateOf(false) }
    // Si el QR trae valores poco comunes, se muestran para que el usuario los vea.
    var showAdvanced by remember(scannedForm) { mutableStateOf(scannedForm?.hasDefaultAdvancedOptions == false) }

    fun submit() {
        when (val result = form.validate()) {
            is FormResult.Valid -> onSave(result.account)
            is FormResult.Invalid -> errors = result.errors
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (scannedForm != null) "Revisar cuenta" else "Agregar cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (scannedForm != null) {
                    "Estos datos vienen del código QR. Revisa que el servicio y la cuenta sean los que esperas; " +
                        "puedes cambiar los nombres antes de guardar."
                } else {
                    "Escribe los datos que te muestra el servicio cuando eliges «ingresar la clave manualmente»."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = form.issuer,
                onValueChange = { form = form.copy(issuer = it) },
                label = { Text("Servicio (opcional)") },
                placeholder = { Text("Ej.: GitHub") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("issuer"),
            )
            OutlinedTextField(
                value = form.accountName,
                onValueChange = { form = form.copy(accountName = it); errors = errors.copy(accountName = null) },
                label = { Text("Cuenta") },
                placeholder = { Text("Ej.: tu correo o usuario") },
                singleLine = true,
                isError = errors.accountName != null,
                supportingText = errors.accountName?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("accountName"),
            )
            OutlinedTextField(
                value = form.secret,
                onValueChange = { form = form.copy(secret = it); errors = errors.copy(secret = null) },
                label = { Text("Clave secreta") },
                singleLine = true,
                isError = errors.secret != null,
                supportingText = errors.secret?.let { { Text(it) } },
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            painterResource(if (showSecret) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                            contentDescription = if (showSecret) "Ocultar clave" else "Mostrar clave",
                        )
                    }
                },
                // Tipo contraseña y sin autocorrección: el teclado no aprende ni sugiere la clave.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().testTag("secret"),
            )

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Ocultar opciones avanzadas" else "Opciones avanzadas")
            }
            if (showAdvanced) {
                Text(
                    "Cámbialas solo si el servicio lo indica. La mayoría usa SHA1, 6 dígitos y 30 segundos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Algoritmo", style = MaterialTheme.typography.labelLarge)
                Choice(HashAlgorithm.entries, form.algorithm, { it.name }) { form = form.copy(algorithm = it) }
                Text("Dígitos", style = MaterialTheme.typography.labelLarge)
                Choice(Otp.SUPPORTED_DIGITS.toList(), form.digits, { it.toString() }) { form = form.copy(digits = it) }
                OutlinedTextField(
                    value = form.period,
                    onValueChange = { form = form.copy(period = it.filter(Char::isDigit).take(3)); errors = errors.copy(period = null) },
                    label = { Text("Periodo (segundos)") },
                    singleLine = true,
                    isError = errors.period != null,
                    supportingText = errors.period?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(onClick = ::submit, modifier = Modifier.fillMaxWidth().testTag("save")) {
                Text("Guardar")
            }
        }
    }
}

@Composable
private fun <T> Choice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label(option)) }
        }
    }
}
