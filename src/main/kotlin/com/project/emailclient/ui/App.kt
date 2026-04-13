package com.project.emailclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Root composable for the email client.
 * Renders the main window content and overlays modal dialog panels on top.
 */
@Composable
fun App(state: AppState) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {

                // ── main window content ───────────────────────────────────────
                Column(modifier = Modifier.fillMaxSize()) {
                    TopActionBar(state)
                    StatusBar(state.statusMessage, state.isLoading)

                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        InboxListPanel(state, modifier = Modifier.weight(0.40f))
                        VerticalDivider()
                        MessageViewerPanel(state, modifier = Modifier.weight(0.60f))
                    }

                    BottomActionBar(state)
                }

                // ── overlay dialogs ───────────────────────────────────────────
                if (state.showConnect) {
                    ModalOverlay {
                        ConnectDialogContent(
                            state     = state,
                            onDismiss = { state.showConnect = false }
                        )
                    }
                }

                state.composeRequest?.let { req ->
                    ModalOverlay {
                        ComposeDialogContent(
                            request     = req,
                            defaultFrom = state.currentUsername(),
                            onSend      = { from, to, subject, body ->
                                state.sendMessage(from, to, subject, body)
                            },
                            onDismiss   = { state.composeRequest = null }
                        )
                    }
                }


            }
        }
    }
}

// ── Top action bar ────────────────────────────────────────────────────────────

@Composable
private fun TopActionBar(state: AppState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { state.showConnect = true }) {
            Text("Connect / Profiles")
        }
        Button(
            onClick  = { state.composeNew() },
            enabled  = state.connectedProfile != null
        ) {
            Text("Compose Mail")
        }
        Spacer(Modifier.weight(1f))
        state.connectedProfile?.let { profile ->
            Text(
                text  = "Connected: ${profile.profileName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Status bar ────────────────────────────────────────────────────────────────

@Composable
private fun StatusBar(message: String?, loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        }
        Text(
            text  = message ?: "Ready",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Bottom action bar ─────────────────────────────────────────────────────────

@Composable
private fun BottomActionBar(state: AppState) {
    val hasSelection = state.selectedIndex != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { state.replyToSelected() }, enabled = hasSelection) {
            Text("Reply")
        }
        Button(onClick = { state.forwardSelected() }, enabled = hasSelection) {
            Text("Forward")
        }
        Button(
            onClick  = { state.deleteSelected() },
            enabled  = hasSelection,
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Delete")
        }
    }
}

// ── Shared overlay helper ─────────────────────────────────────────────────────

/**
 * Semi-transparent dimming overlay that prevents interaction with content behind it.
 * Dialogs are rendered centered inside this overlay.
 */
@Composable
fun ModalOverlay(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier          = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment  = Alignment.Center,
        content           = content
    )
}
