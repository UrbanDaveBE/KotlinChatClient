package local.dev

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val api = ChatApi("javaprojects.ch", 50001)

    println("Testing ping...")
    println("Ping: ${api.ping()}")

    println("Logging in...")
    val token = api.login("Bern2", "abc123")
    println("Token = $token")

    val users = api.getOnlineUsers()
    println("Online users: $users")

    if (users.isNotEmpty()) {
        val target = users.first() // Sorry erster :)
        println("Sending message to $target ...")
        val result = api.sendMessage(token, target, "Hello World!")
        println("Send result: $result")
    }

    println("Polling messages...")
    val incoming = api.poll(token)
    println("Incoming messages: $incoming")
}
