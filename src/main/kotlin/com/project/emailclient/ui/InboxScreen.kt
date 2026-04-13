package com.project.emailclient.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.emailclient.MessageSummary

// ── Inbox list panel ──────────────────────────────────────────────────────────

@Composable
fun InboxListPanel(state: AppState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxHeight()) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            ColumnHeader("Sender",  Modifier.weight(0.35f))
            ColumnHeader("Subject", Modifier.weight(0.40f))
            ColumnHeader("Date",    Modifier.weight(0.25f))
        }
        HorizontalDivider()

        if (state.messages.isEmpty()) {
            // Empty-state guidance
            Box(
                modifier          = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment  = Alignment.TopStart
            ) {
                Text(
                    text  = if (state.connectedProfile == null)
                                "Not connected. Click \"Connect / Profiles\" to get started."
                            else
                                "No messages in inbox.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.messages) { index, msg ->
                    MessageRow(
                        message  = msg,
                        selected = state.selectedIndex == index,
                        onClick  = { state.selectMessage(index) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader(text: String, modifier: Modifier) {
    Text(
        text       = text,
        modifier   = modifier,
        fontWeight = FontWeight.Bold,
        style      = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun MessageRow(message: MessageSummary, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else          MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CellText(message.sender,  Modifier.weight(0.35f))
        CellText(message.subject, Modifier.weight(0.40f))
        CellText(message.date,    Modifier.weight(0.25f))
    }
}

@Composable
private fun CellText(text: String, modifier: Modifier) {
    Text(
        text     = text,
        modifier = modifier,
        style    = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// ── Message viewer panel ──────────────────────────────────────────────────────

@Composable
fun MessageViewerPanel(state: AppState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxHeight()) {
        // Panel header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Message Content", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
        HorizontalDivider()

        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            when {
                state.selectedIndex == null -> Text(
                    "Select a message to view its content.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.selectedContent == null -> Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                else -> Text(
                    text     = state.selectedContent!!,
                    modifier = Modifier.verticalScroll(scrollState),
                    style    = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
