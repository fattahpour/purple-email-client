package com.project.emailclient.ui

import androidx.compose.runtime.*
import com.project.emailclient.MailProfile
import com.project.emailclient.MailProfileStore
import com.project.emailclient.MailService
import com.project.emailclient.MessageSummary
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException

// ── Compose request model ─────────────────────────────────────────────────────

enum class ComposeType { NEW, REPLY, FORWARD }

data class ComposeRequest(
    val type: ComposeType,
    val replyTo: String? = null,
    val subject: String? = null,
    val quotedBody: String? = null
)

// ── App state ─────────────────────────────────────────────────────────────────

/**
 * Central state holder for the Compose Desktop email client.
 *
 * All async operations run on [Dispatchers.IO] and post state changes back to
 * [Dispatchers.Main] (the Swing/AWT event thread, backed by coroutines-swing).
 * Passwords are held only in [sessionPassword] and never written to disk or logs.
 */
class AppState(
    val profileStore: MailProfileStore,
    private val mailService: MailService
) {
    companion object {
        /** Polling interval for automatic inbox refresh after connect. */
        const val POLL_INTERVAL_MS = 60_000L
    }

    /** Coroutine scope for background operations. */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── connection ───────────────────────────────────────────────────────────

    var connectedProfile by mutableStateOf<MailProfile?>(null)
        private set

    private var sessionPassword: String? = null

    // ── auto-receive ─────────────────────────────────────────────────────────

    private var pollJob: Job? = null
    private var consecutivePollErrors = 0

    // ── inbox ────────────────────────────────────────────────────────────────

    var messages by mutableStateOf<List<MessageSummary>>(emptyList())
        private set

    var selectedIndex by mutableStateOf<Int?>(null)
        private set

    var selectedContent by mutableStateOf<String?>(null)
        private set

    // ── loading / status ─────────────────────────────────────────────────────

    var isLoading by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)

    // ── dialog visibility ────────────────────────────────────────────────────

    var showConnect    by mutableStateOf(true)   // show connect dialog on startup
    var composeRequest by mutableStateOf<ComposeRequest?>(null)

    // ── operations ───────────────────────────────────────────────────────────

    fun connect(profile: MailProfile, password: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                isLoading = true
                statusMessage = "Connecting to ${profile.profileName}…"
            }
            try {
                val msgs = mailService.connect(profile, password)
                withContext(Dispatchers.Main) {
                    connectedProfile  = profile
                    sessionPassword   = password
                    messages          = msgs
                    selectedIndex     = null
                    selectedContent   = null
                    isLoading         = false
                    statusMessage     = "Connected — ${msgs.size} message(s)"
                    profileStore.setLastUsedProfileId(profile.id)
                    showConnect       = false
                }
                startAutoReceive()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading     = false
                    statusMessage = "Connection failed: ${e.message}"
                }
            }
        }
    }

    fun selectMessage(index: Int) {
        if (index < 0 || index >= messages.size) return
        selectedIndex   = index
        selectedContent = null
        scope.launch {
            try {
                val content = mailService.getContent(messages[index])
                withContext(Dispatchers.Main) {
                    if (selectedIndex == index) {
                        selectedContent = content.ifBlank { "(empty)" }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (selectedIndex == index) {
                        selectedContent = "Unable to load message content: ${e.message}"
                    }
                }
            }
        }
    }

    fun deleteSelected() {
        val idx = selectedIndex ?: return
        val msg = messages.getOrNull(idx) ?: return
        scope.launch {
            withContext(Dispatchers.Main) { isLoading = true }
            try {
                mailService.deleteMessage(msg)
                withContext(Dispatchers.Main) {
                    val updated = messages.toMutableList()
                    updated.removeAt(idx)
                    messages        = updated
                    selectedIndex   = null
                    selectedContent = null
                    isLoading       = false
                    statusMessage   = "Message deleted."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading     = false
                    statusMessage = "Delete failed: ${e.message}"
                }
            }
        }
    }

    fun composeNew() {
        if (connectedProfile == null) { statusMessage = "Connect first."; return }
        composeRequest = ComposeRequest(ComposeType.NEW)
    }

    fun replyToSelected() {
        val idx = selectedIndex ?: return
        val msg = messages.getOrNull(idx) ?: return
        val original = selectedContent ?: ""
        val subj = msg.subject.let { if (it.startsWith("RE:")) it else "RE: $it" }
        val body = "\n----------------- REPLIED TO MESSAGE -----------------\n$original"
        composeRequest = ComposeRequest(ComposeType.REPLY, replyTo = msg.sender, subject = subj, quotedBody = body)
    }

    fun forwardSelected() {
        val idx = selectedIndex ?: return
        val msg = messages.getOrNull(idx) ?: return
        val original = selectedContent ?: ""
        val subj = msg.subject.let { if (it.startsWith("FWD:")) it else "FWD: $it" }
        val body = "\n----------------- FORWARDED MESSAGE -----------------\n$original"
        composeRequest = ComposeRequest(ComposeType.FORWARD, subject = subj, quotedBody = body)
    }

    fun sendMessage(from: String, to: String, subject: String, body: String) {
        val profile  = connectedProfile ?: return
        val password = sessionPassword  ?: return
        scope.launch {
            withContext(Dispatchers.Main) { isLoading = true }
            try {
                mailService.sendMessage(profile, password, from, to, subject, body)
                withContext(Dispatchers.Main) {
                    isLoading      = false
                    statusMessage  = "Message sent."
                    composeRequest = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading     = false
                    statusMessage = "Send failed: ${e.message}"
                }
            }
        }
    }

    /** Username of the currently connected profile, or empty string if not connected. */
    fun currentUsername(): String = connectedProfile?.username ?: ""

    // ── auto-receive ─────────────────────────────────────────────────────────

    /**
     * Starts a background polling loop that refreshes the inbox every
     * [POLL_INTERVAL_MS] milliseconds. Any existing poll job is cancelled first
     * so duplicate pollers are never created.
     */
    internal fun startAutoReceive() {
        stopAutoReceive()
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (connectedProfile == null) break
                try {
                    val refreshed = mailService.refreshInbox()
                    consecutivePollErrors = 0
                    withContext(Dispatchers.Main) {
                        val oldCount = messages.size
                        messages = refreshed
                        if (refreshed.size > oldCount) {
                            statusMessage = "Inbox updated — ${refreshed.size} message(s)"
                        }
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    consecutivePollErrors++
                    // Only surface the error on first failure or every 5th, to avoid status spam.
                    if (consecutivePollErrors == 1 || consecutivePollErrors % 5 == 0) {
                        withContext(Dispatchers.Main) {
                            statusMessage = "Auto-refresh error: ${e.message}"
                        }
                    }
                }
            }
        }
    }

    /**
     * Cancels the active polling job. Safe to call when no poll is running.
     */
    internal fun stopAutoReceive() {
        pollJob?.cancel()
        pollJob = null
        consecutivePollErrors = 0
    }

    /** Clean up resources on app exit. */
    fun onExit() {
        stopAutoReceive()
        mailService.disconnect()
        scope.cancel()
    }
}
