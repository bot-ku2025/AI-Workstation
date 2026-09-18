package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.security.CredentialKind
import com.example.security.CredentialVault
import com.example.ui.components.CyberActionButton
import com.example.ui.theme.TerminalGreen
import com.example.ui.viewmodel.WorkstationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(viewModel: WorkstationViewModel) {
    val context = LocalContext.current
    val vault = remember { CredentialVault(context) }
    var refresh by remember { mutableStateOf(0) }
    var provider by remember { mutableStateOf("Gemini") }
    var label by remember { mutableStateOf("Gemini Key #1") }
    var secret by remember { mutableStateOf("") }
    var dialog by remember { mutableStateOf<String?>(null) }
    val credentials = remember(refresh) { vault.listCredentials() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AUTHENTICATION & INTEGRATIONS", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { viewModel.navigateBack() }) { Icon(Icons.Default.ArrowBack, "Kembali", tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("CREDENTIAL VAULT", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("API key/token disimpan terenkripsi dengan Android Keystore. Tidak ditulis ke source, prompt, checkpoint, atau GitHub.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

            listOf("Gemini" to CredentialKind.AI_API_KEY, "OpenAI" to CredentialKind.AI_API_KEY, "Claude" to CredentialKind.AI_API_KEY, "9Router" to CredentialKind.ROUTER_API_KEY, "OpenRouter" to CredentialKind.ROUTER_API_KEY).forEach { (name, kind) ->
                val count = credentials.count { it.provider.equals(name, true) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                        Text(if (count > 0) "$count credential tersimpan" else "Not Configured", color = if (count > 0) TerminalGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.Lock, "Secure", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("TAMBAH API KEY / TOKEN", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(provider, { provider = it }, label = { Text("Provider") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(label, { label = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(secret, { secret = it }, label = { Text("API Key / Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            CyberActionButton(
                text = "SIMPAN KE CREDENTIAL VAULT",
                onClick = {
                    if (secret.isNotBlank()) {
                        val kind = if (provider.contains("router", true)) CredentialKind.ROUTER_API_KEY else CredentialKind.AI_API_KEY
                        vault.putCredential("${provider.lowercase().replace(" ", "-")}-${System.currentTimeMillis()}", provider, kind, label, secret)
                        secret = ""
                        refresh++
                        dialog = "Credential tersimpan terenkripsi di perangkat ini."
                    }
                }, modifier = Modifier.fillMaxWidth(), icon = Icons.Default.Save, isPrimary = true
            )

            Spacer(Modifier.height(8.dp))
            Text("CLOUD & DEVELOPMENT", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("GitHub / Google Cloud Build / Codemagic / Appcircle / Bitrise / Custom dapat disimpan sebagai credential/config terpisah. Status tetap Not Connected sampai autentikasi nyata berhasil.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Button(onClick = { dialog = "GitHub Account: Not Connected\nCloud Build: Not Configured\nTidak ada status palsu." }, modifier = Modifier.fillMaxWidth()) { Text("GITHUB — NOT CONNECTED") }
            Button(onClick = { dialog = "Cloud providers siap dikonfigurasi melalui credential vault dan project binding." }, modifier = Modifier.fillMaxWidth()) { Text("CLOUD PROVIDERS — CONFIGURE") }

            Spacer(Modifier.height(8.dp))
            Text("TERSIMPAN", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            credentials.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.label, color = MaterialTheme.colorScheme.onBackground)
                        Text("${item.provider} • ${item.kind.name}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { vault.removeCredential(item.id); refresh++ }) { Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
    dialog?.let { message ->
        AlertDialog(onDismissRequest = { dialog = null }, title = { Text("AI WORKSTATION") }, text = { Text(message) }, confirmButton = { Button(onClick = { dialog = null }) { Text("OK") } })
    }
}
