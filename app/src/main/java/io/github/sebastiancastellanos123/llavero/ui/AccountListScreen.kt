package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.sebastiancastellanos123.llavero.R
import io.github.sebastiancastellanos123.llavero.otp.Otp
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount

/** Los últimos segundos se marcan en rojo para no copiar un código a punto de vencer. */
private const val WARNING_SECONDS = 5

@Composable
fun AccountList(
    accounts: List<OtpAccount>,
    nowSeconds: Long,
    onCopy: (code: String) -> Unit,
    onDelete: (OtpAccount) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<OtpAccount?>(null) }

    if (accounts.isEmpty()) {
        Box(modifier.fillMaxSize().padding(contentPadding).padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "Aún no tienes cuentas.\nToca «Agregar» para añadir la primera.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("empty"),
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp, // espacio para el botón flotante
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // El índice va en la clave porque puede haber dos cuentas con el mismo nombre.
            itemsIndexed(accounts, key = { index, account -> "$index:${account.issuer}:${account.accountName}" }) { _, account ->
                AccountRow(account, nowSeconds, onCopy, onDeleteRequest = { pendingDelete = account })
            }
        }
    }

    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar cuenta?") },
            text = {
                Text(
                    "Se eliminará «${account.issuer ?: account.accountName}» de este teléfono. " +
                        "Si no tienes otra forma de generar sus códigos, podrías perder el acceso a esa cuenta."
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(account); pendingDelete = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun AccountRow(
    account: OtpAccount,
    nowSeconds: Long,
    onCopy: (code: String) -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val code = account.codeAt(nowSeconds)
    val remaining = Otp.secondsRemaining(nowSeconds, account.period)
    val warning = remaining <= WARNING_SECONDS
    val title = account.issuer ?: account.accountName

    ElevatedCard(onClick = { onCopy(code) }, modifier = Modifier.fillMaxWidth().testTag("account")) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatCode(code),
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("code"),
                )
                if (account.issuer != null) {
                    Text(
                        account.accountName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Countdown(remaining, account.period, warning)
            IconButton(onClick = onDeleteRequest) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Eliminar $title")
            }
        }
    }
}

@Composable
private fun Countdown(remaining: Int, period: Int, warning: Boolean) {
    val color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(Modifier.padding(horizontal = 8.dp).size(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { remaining.toFloat() / period },
            modifier = Modifier.fillMaxSize(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text("$remaining", style = MaterialTheme.typography.labelLarge, color = color)
    }
}
