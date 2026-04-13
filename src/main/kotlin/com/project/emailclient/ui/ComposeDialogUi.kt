package com.project.emailclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Compose / Reply / Forward dialog.
 * Pre-fills fields from [request]; calls [onSend] when the user clicks Send.
 * [defaultFrom] is pre-populated from the connected profile username.
 */
@Composable
fun ComposeDialogContent(
    request:     ComposeRequest,
    defaultFrom: String,
    onSend:      (from: String, to: String, subject: String, body: String) -> Unit,
    onDismiss:   () -> Unit
) {
    val title = when (request.type) {
        ComposeType.REPLY   -> "Reply To Message"
        ComposeType.FORWARD -> "Forward Message"
        ComposeType.NEW     -> "New Message"
    }

    var from         by remember { mutableStateOf(defaultFrom) }
    var to           by remember { mutableStateOf(request.replyTo ?: "") }
    var subject      by remember { mutableStateOf(request.subject ?: "") }
    var body         by remember { mutableStateOf(request.quotedBody ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier  = Modifier.width(640.dp).heightIn(max = 720.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier             = Modifier.padding(24.dp),
            verticalArrangement  = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()

            OutlinedTextField(
                value = from, onValueChange = { from = it },
                label = { Text("From") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = to, onValueChange = { to = it },
                label = { Text("To") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = subject, onValueChange = { subject = it },
                label = { Text("Subject") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value         = body,
                onValueChange = { body = it },
                label         = { Text("Message") },
                modifier      = Modifier.fillMaxWidth().height(280.dp),
                maxLines      = Int.MAX_VALUE
            )

            errorMessage?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            Row(
                modifier             = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = {
                    when {
                        from.isBlank()    -> errorMessage = "From is required."
                        to.isBlank()      -> errorMessage = "To is required."
                        subject.isBlank() -> errorMessage = "Subject is required."
                        body.isBlank()    -> errorMessage = "Message body is required."
                        else -> { errorMessage = null; onSend(from.trim(), to.trim(), subject.trim(), body) }
                    }
                }) { Text("Send") }
            }
        }
    }
}
