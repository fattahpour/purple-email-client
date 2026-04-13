package com.project.emailclient

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.project.emailclient.ui.App
import com.project.emailclient.ui.AppState

fun main() {
    val profileStore = MailProfileStore()
    profileStore.load()
    val mailService = MailService()
    val state = AppState(profileStore, mailService)

    application {
        Window(
            onCloseRequest = {
                state.onExit()
                exitApplication()
            },
            title = "E-mail Client",
            state = rememberWindowState(width = 960.dp, height = 640.dp)
        ) {
            App(state)
        }
    }
}
