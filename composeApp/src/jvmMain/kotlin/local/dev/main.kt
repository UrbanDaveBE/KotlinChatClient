package local.dev

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val viewModel = remember { ChatViewModel() }

    Window(
        title = "Kotlin Chat Client",
        onCloseRequest = {
            viewModel.logout()
            Thread.sleep(200)
            exitApplication()
        }
    ) {
        App(viewModel)
    }
}