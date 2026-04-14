package com.project.emailclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.emailclient.MailProfile
import com.project.emailclient.MailProfileStore

private val PROTOCOLS = listOf("pop3s", "pop3", "imaps", "imap")

/**
 * Profile add/edit dialog content.
 * Validates required fields before calling [onSaved].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorContent(
    existing:     MailProfile?,
    profileStore: MailProfileStore,
    onSaved:      (MailProfile) -> Unit,
    onDismiss:    () -> Unit
) {
    var profileName    by remember { mutableStateOf(existing?.profileName ?: "") }
    var username       by remember { mutableStateOf(existing?.username ?: "") }
    var protocol       by remember { mutableStateOf(existing?.incomingProtocol ?: "pop3s") }
    var inHost         by remember { mutableStateOf(existing?.incomingHost ?: "") }
    var inPort         by remember { mutableStateOf(existing?.incomingPort?.toString() ?: "995") }
    var inSsl          by remember { mutableStateOf(existing?.isIncomingSsl ?: true) }
    var smtpHost       by remember { mutableStateOf(existing?.smtpHost ?: "") }
    var smtpPort       by remember { mutableStateOf(existing?.smtpPort?.toString() ?: "587") }
    var smtpStartTls   by remember { mutableStateOf(existing?.isSmtpStartTls ?: true) }
    var trustInvalidSsl by remember { mutableStateOf(existing?.isTrustInvalidSsl ?: false) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }

    fun autoFillPort() = when (protocol) {
        "pop3s" -> { inPort = "995"; inSsl = true }
        "pop3"  -> { inPort = "110"; inSsl = false }
        "imaps" -> { inPort = "993"; inSsl = true }
        "imap"  -> { inPort = "143"; inSsl = false }
        else    -> {}
    }

    Card(
        modifier  = Modifier.width(500.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (existing == null) "New Profile" else "Edit Profile",
                style = MaterialTheme.typography.titleLarge
            )
            HorizontalDivider()

            // Basic
            OutlinedTextField(
                value = profileName, onValueChange = { profileName = it },
                label = { Text("Profile Name *") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("Username / Email *") }, modifier = Modifier.fillMaxWidth()
            )

            SectionLabel("Incoming Mail")

            // Protocol dropdown
            var protoExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = protoExpanded, onExpandedChange = { protoExpanded = it }) {
                OutlinedTextField(
                    value = protocol, onValueChange = {},
                    readOnly = true, label = { Text("Protocol") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(protoExpanded) }
                )
                ExposedDropdownMenu(expanded = protoExpanded, onDismissRequest = { protoExpanded = false }) {
                    PROTOCOLS.forEach { p ->
                        DropdownMenuItem(text = { Text(p) }, onClick = {
                            protocol = p; autoFillPort(); protoExpanded = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = inHost, onValueChange = { inHost = it },
                label = { Text("Incoming Host *") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = inPort, onValueChange = { inPort = it },
                label = { Text("Incoming Port *") }, modifier = Modifier.fillMaxWidth()
            )
            LabeledCheckbox(checked = inSsl, onCheckedChange = { inSsl = it }, label = "Use SSL/TLS")

            SectionLabel("Outgoing Mail (SMTP)")

            OutlinedTextField(
                value = smtpHost, onValueChange = { smtpHost = it },
                label = { Text("SMTP Host *") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = smtpPort, onValueChange = { smtpPort = it },
                label = { Text("SMTP Port *") }, modifier = Modifier.fillMaxWidth()
            )
            LabeledCheckbox(checked = smtpStartTls, onCheckedChange = { smtpStartTls = it }, label = "Use STARTTLS")
            LabeledCheckbox(
                checked = trustInvalidSsl,
                onCheckedChange = { trustInvalidSsl = it },
                label = "Trust invalid SSL certificates"
            )

            errorMessage?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = {
                    val inPortInt  = inPort.trim().toIntOrNull()
                        ?: run { errorMessage = "Incoming port must be an integer."; return@Button }
                    val smtpPortInt = smtpPort.trim().toIntOrNull()
                        ?: run { errorMessage = "SMTP port must be an integer."; return@Button }

                    val candidate = MailProfile(
                        existing?.id,
                        profileName.trim(), username.trim(),
                        protocol, inHost.trim(), inPortInt, inSsl,
                        smtpHost.trim(), smtpPortInt, smtpStartTls, trustInvalidSsl
                    )
                    val errors = candidate.validate()
                    if (errors.isNotEmpty()) { errorMessage = errors.joinToString("\n"); return@Button }

                    if (existing == null) profileStore.addProfile(candidate)
                    else profileStore.updateProfile(candidate)
                    onSaved(candidate)
                }) { Text("Save") }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun LabeledCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
