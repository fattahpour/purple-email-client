package com.project.emailclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.project.emailclient.MailProfile

/**
 * Connect dialog content — profile selector, password field, and profile CRUD buttons.
 * Rendered inside a [ModalOverlay] so it blocks interaction with the inbox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectDialogContent(
    state:     AppState,
    onDismiss: () -> Unit
) {
    val profiles = state.profileStore.profiles

    // Resolve initial selection from lastUsed, then first in list
    var selectedProfile by remember {
        mutableStateOf(
            state.profileStore.findById(state.profileStore.lastUsedProfileId ?: "")
                ?: profiles.firstOrNull()
        )
    }
    var password          by remember { mutableStateOf("") }
    var showProfileEditor by remember { mutableStateOf(false) }
    var editingProfile    by remember { mutableStateOf<MailProfile?>(null) }
    var deleteTarget      by remember { mutableStateOf<MailProfile?>(null) }

    Card(
        modifier  = Modifier.width(480.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier             = Modifier.padding(24.dp),
            verticalArrangement  = Arrangement.spacedBy(12.dp)
        ) {
            Text("Connect to Mail Server", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()

            if (profiles.isEmpty()) {
                Text(
                    "No profiles saved. Create one to connect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Profile selector
                Text("Profile", style = MaterialTheme.typography.labelMedium)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded        = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value       = selectedProfile?.toString() ?: "— select —",
                        onValueChange = {},
                        readOnly    = true,
                        modifier    = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded        = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        profiles.forEach { p ->
                            DropdownMenuItem(
                                text    = { Text(p.toString()) },
                                onClick = { selectedProfile = p; expanded = false }
                            )
                        }
                    }
                }

                // Password
                OutlinedTextField(
                    value               = password,
                    onValueChange       = { password = it },
                    label               = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier            = Modifier.fillMaxWidth()
                )
            }

            // Profile management
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editingProfile = null; showProfileEditor = true }) {
                    Text("New Profile")
                }
                if (selectedProfile != null) {
                    OutlinedButton(onClick = { editingProfile = selectedProfile; showProfileEditor = true }) {
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = { deleteTarget = selectedProfile },
                        colors  = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier             = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick  = {
                        val p = selectedProfile
                        if (p != null && password.isNotBlank()) state.connect(p, password)
                    },
                    enabled  = selectedProfile != null && password.isNotBlank()
                ) {
                    Text("Connect")
                }
            }
        }
    }

    // ── Profile editor nested overlay ────────────────────────────────────────
    if (showProfileEditor) {
        ModalOverlay {
            ProfileEditorContent(
                existing     = editingProfile,
                profileStore = state.profileStore,
                onSaved      = { saved ->
                    selectedProfile   = saved
                    showProfileEditor = false
                    editingProfile    = null
                },
                onDismiss    = { showProfileEditor = false }
            )
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title            = { Text("Confirm Delete") },
            text             = { Text("""Delete profile "${target.profileName}"?""") },
            confirmButton    = {
                Button(
                    onClick = {
                        state.profileStore.deleteProfile(target.id)
                        if (selectedProfile?.id == target.id) {
                            selectedProfile = state.profileStore.profiles.firstOrNull()
                        }
                        deleteTarget = null
                    },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton    = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}
