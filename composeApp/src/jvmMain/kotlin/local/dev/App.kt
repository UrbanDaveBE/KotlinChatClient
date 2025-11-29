package local.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun App(viewModel: ChatViewModel) {
    val users by viewModel.users.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var isLoggedIn by remember { mutableStateOf(false) }

    MaterialTheme {
        if (!isLoggedIn) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { isLoggedIn = true }
            )
        } else {
            ChatScreen(
                viewModel = viewModel,
                users = users,
                messages = messages,
                onLogout = {
                    viewModel.logout()
                    isLoggedIn = false
                }
            )
        }
    }
}

// ---------------------------------------------------------
// SCREEN 1: LOGIN
// ---------------------------------------------------------
@Composable
fun LoginScreen(viewModel: ChatViewModel, onLoginSuccess: () -> Unit) {
    var host by remember { mutableStateOf("javaprojects.ch") }
    var port by remember { mutableStateOf("50001") }
    var username by remember { mutableStateOf("Bern") }
    var password by remember { mutableStateOf("abc123") }

    var isLoading by remember { mutableStateOf(false) }
    var infoMsg by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    var pingStatus by remember { mutableStateOf("") }
    var isPingError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(elevation = 8.dp, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kotlin Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host") }, modifier = Modifier.weight(0.7f))
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, modifier = Modifier.weight(0.3f))
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        pingStatus = "Pinging..."
                        isPingError = false
                        viewModel.ping(host, port) { success ->
                            if (success) {
                                pingStatus = "Ping: OK"
                                isPingError = false
                            } else {
                                pingStatus = "Ping: Fehler"
                                isPingError = true
                            }
                        }
                    }) {
                        Text("Verbindung testen (Ping)")
                    }

                    if (pingStatus.isNotEmpty()) {
                        Text(
                            text = pingStatus,
                            color = if (isPingError) Color.Red else Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            isLoading = true
                            infoMsg = ""
                            viewModel.register(host, port, username, password) { error ->
                                isLoading = false
                                if (error == null) {
                                    isError = false
                                    infoMsg = "Registriert! Bitte einloggen."
                                } else {
                                    isError = true
                                    infoMsg = "Fehler: $error"
                                }
                            }
                        }) {
                            Text("Registrieren")
                        }

                        Button(onClick = {
                            isLoading = true
                            infoMsg = ""
                            viewModel.login(host, port, username, password) { success ->
                                isLoading = false
                                if (success) {
                                    onLoginSuccess()
                                } else {
                                    isError = true
                                    infoMsg = "Login fehlgeschlagen!"
                                }
                            }
                        }) {
                            Text("Einloggen")
                        }
                    }
                }
                if (infoMsg.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(infoMsg, color = if (isError) Color.Red else Color.Green)
                }
            }
        }
    }
}

// ---------------------------------------------------------
// SCREEN 2: HAUPTANSICHT
// ---------------------------------------------------------
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    users: List<UserUiItem>,
    messages: List<UiChatMessage>,
    onLogout: () -> Unit
) {
    var selectedRecipient by remember { mutableStateOf("all") }
    var inputText by remember { mutableStateOf("") }

    var unreadSenders by remember { mutableStateOf(setOf<String>()) }
    var lastMessageCount by remember { mutableStateOf(0) }

    LaunchedEffect(messages) {
        if (messages.size > lastMessageCount) {
            val newMsgs = messages.subList(lastMessageCount, messages.size)
            newMsgs.forEach { msg ->
                val isFromMe = msg.sender == viewModel.myUsername
                val chatPartner = if (msg.recipient == "all") "all" else msg.sender
                if (!isFromMe && chatPartner != selectedRecipient) {
                    unreadSenders = unreadSenders + chatPartner
                }
            }
            lastMessageCount = messages.size
        }
    }

    LaunchedEffect(selectedRecipient) {
        unreadSenders = unreadSenders - selectedRecipient
    }

    val isTargetOnline = if (selectedRecipient == "all") true else users.find { it.username == selectedRecipient }?.isOnline == true

    val visibleMessages = remember(messages, selectedRecipient) {
        messages.filter { msg ->
            if (selectedRecipient == "all") msg.recipient == "all"
            else (msg.sender == selectedRecipient) || (msg.recipient == selectedRecipient)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) listState.animateScrollToItem(visibleMessages.size - 1)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.width(260.dp).fillMaxHeight().background(Color(0xFFE0E0E0))) {
            TopAppBar(title = { Text("User Online") }, backgroundColor = Color(0xFF3F51B5), contentColor = Color.White)
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    UserRow("ALLE (Broadcast)", true, unreadSenders.contains("all"), selectedRecipient == "all") { selectedRecipient = "all" }
                    Divider(color = Color.Gray)
                }
                items(users) { user ->
                    if (user.username != viewModel.myUsername) {
                        UserRow(user.username, user.isOnline, unreadSenders.contains(user.username), selectedRecipient == user.username) { selectedRecipient = user.username }
                    }
                }
            }
            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFCDD2)), modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Logout", color = Color.Red) }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White)) {
            TopAppBar(
                title = {
                    Column {
                        Text(if(selectedRecipient == "all") "Chat mit ALLEN" else "Chat mit $selectedRecipient")
                        if (selectedRecipient != "all") {
                            Text(if(isTargetOnline) "Online" else "Offline", style = MaterialTheme.typography.caption)
                        }
                    }
                },
                backgroundColor = Color.White,
                elevation = 4.dp
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleMessages) { msg ->
                    MessageBubble(msg, msg.sender == viewModel.myUsername)
                }
            }

            Divider()

            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f).onKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                            if (inputText.isNotBlank() && isTargetOnline) {
                                viewModel.sendMessage(inputText, to = selectedRecipient)
                                inputText = ""
                            }
                            true
                        } else false
                    },
                    placeholder = { Text(if(isTargetOnline) "Nachricht..." else "User ist offline") },
                    singleLine = true,
                    enabled = isTargetOnline
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText, to = selectedRecipient)
                            inputText = ""
                        }
                    },
                    enabled = isTargetOnline
                ) {
                    Text("Senden")
                }
            }
        }
    }
}

@Composable
fun UserRow(name: String, isOnline: Boolean, hasUnread: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).background(if (isSelected) Color(0xFFBBDEFB) else Color.Transparent).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(if (isOnline) Color.Green else Color.Gray, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(name, fontWeight = if(hasUnread) FontWeight.Bold else FontWeight.Normal, color = if(isOnline) Color.Black else Color.Gray)
    }
}

@Composable
fun MessageBubble(msg: UiChatMessage, isMe: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        if (!isMe) Text(msg.sender, style = MaterialTheme.typography.caption, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
        Surface(color = if (isMe) Color(0xFF3F51B5) else Color(0xFFEEEEEE), shape = RoundedCornerShape(8.dp), elevation = 2.dp) {
            Text(text = msg.message, modifier = Modifier.padding(10.dp), color = if (isMe) Color.White else Color.Black)
        }
    }
}