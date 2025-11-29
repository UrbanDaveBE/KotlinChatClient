package local.dev

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatViewModel {
    private var api: ChatApi? = null

    private val _users = MutableStateFlow<List<UserUiItem>>(emptyList())
    val users = _users.asStateFlow()

    private val _messages = MutableStateFlow<List<UiChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    var myToken = ""
    var myUsername = ""

    private val viewModelScope = CoroutineScope(Dispatchers.IO)
    private var pollingJob: Job? = null

    private fun ensureApi(host: String, port: Int) {
        api = ChatApi(host, port)
    }

    fun ping(host: String, port: String, onResult: (Boolean) -> Unit) {
        val portInt = port.toIntOrNull() ?: 50001
        ensureApi(host, portInt)

        viewModelScope.launch {
            try {
                val success = api!!.ping()
                onResult(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun register(host: String, port: String, username: String, password: String, onResult: (String?) -> Unit) {
        val portInt = port.toIntOrNull() ?: 50001
        ensureApi(host, portInt)

        viewModelScope.launch {
            val error = api!!.register(username, password)
            onResult(error)
        }
    }

    fun login(host: String, port: String, username: String, password: String, onResult: (Boolean) -> Unit) {
        val portInt = port.toIntOrNull() ?: 50001
        ensureApi(host, portInt)

        viewModelScope.launch {
            try {
                val token = api!!.login(username, password)
                myToken = token
                myUsername = username
                startPolling()
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun sendMessage(msg: String, to: String = "all") {
        if (msg.isBlank() || api == null) return

        viewModelScope.launch {
            try {
                if (to == "all") {
                    println("Starte Broadcast an ONLINE User...")
                    val localMsg = UiChatMessage(sender = myUsername, recipient = "all", message = "[Broadcast] $msg")
                    _messages.update { currentList -> currentList + localMsg }

                    val currentlyOnline = try { api!!.getOnlineUsers() } catch (e: Exception) { emptyList() }

                    var count = 0
                    currentlyOnline.forEach { targetName ->
                        if (targetName != myUsername) {
                            launch {
                                try {
                                    api!!.sendMessage(myToken, targetName, "[Broadcast] $msg")
                                } catch (e: Exception) {
                                    println("Fehler beim Senden an $targetName")
                                }
                            }
                            count++
                        }
                    }
                    println("Broadcast an $count Online-User gesendet.")

                } else {
                    val success = api!!.sendMessage(myToken, to, msg)
                    if (success) {
                        println("Nachricht gesendet an: $to")
                        val localMsg = UiChatMessage(sender = myUsername, recipient = to, message = msg)
                        _messages.update { currentList -> currentList + localMsg }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPolling() {
        if (api == null) return
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val allKnownUsers = api!!.getAllUsers()
                    val onlineUsers = api!!.getOnlineUsers()
                    val newMessages = api!!.poll(myToken)

                    val uiList = allKnownUsers.map { name ->
                        UserUiItem(username = name, isOnline = onlineUsers.contains(name))
                    }
                    val sortedList = uiList.sortedWith(compareByDescending<UserUiItem> { it.isOnline }.thenBy { it.username })
                    _users.value = sortedList

                    if (newMessages.isNotEmpty()) {
                        val convertedMessages = newMessages.map { incoming ->
                            UiChatMessage(sender = incoming.username, recipient = "me", message = incoming.message)
                        }
                        _messages.update { currentList -> currentList + convertedMessages }
                    }
                } catch (e: Exception) {
                    println("Polling error: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    fun logout() {
        pollingJob?.cancel()
        if (myToken.isNotEmpty() && api != null) {
            println("Logge aus...")
            viewModelScope.launch {
                api!!.logout(myToken)
                myToken = ""
                myUsername = ""
                _users.value = emptyList()
                _messages.value = emptyList()
                println("Logout fertig.")
            }
        }
    }
}