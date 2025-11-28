package local.dev

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("TEST 1: Raw API")

    val api = ChatApi("javaprojects.ch", 50001)

    println("1. Ping...")
    val ping = api.ping()
    println("-> Ping Result: $ping")

    println("2. Login...")
    val token = api.login("Bern", "abc123")
    println("-> Token erhalten: $token")

    println("3. Get All Users...")
    val users = api.getAllUsers()
    println("-> User count: ${users.size}")
    println("-> Erste 5: ${users.take(5)}")

    println("4. Send Message...")
    val sent = api.sendMessage(token, "BernCLI", "Raw API Test Message")
    println("-> Gesendet: $sent")

    println("5. Logout...")
    api.logout(token)
    println("Test 1 beendet.")
}