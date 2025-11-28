package local.dev

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ChatApi(private val host: String, private val port: Int) {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonParser)
        }
        expectSuccess = true
    }

    private fun url(path: String) = "http://$host:$port$path"

    suspend fun ping(): Boolean {
        val response = client.get(url("/ping"))
        val text = response.body<String>() // Holen als Text
        val obj = jsonParser.decodeFromString<PingResponse>(text)
        return obj.ping
    }

    suspend fun login(username: String, password: String): String {
        val response = client.post(url("/user/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
        val text = response.body<String>()
        val obj = jsonParser.decodeFromString<TokenResponse>(text)
        return obj.token
    }

    suspend fun getOnlineUsers(): List<String> {
        val response = client.get(url("/users/online"))
        val text = response.body<String>()

        val res = jsonParser.decodeFromString<OnlineUsersResponse>(text)

        return res.online
    }

    suspend fun getAllUsers(): List<String> {
        val response = client.get(url("/users"))
        val text = response.body<String>()
        val res = jsonParser.decodeFromString<UserListResponse>(text)
        return res.users
    }

    suspend fun sendMessage(token: String, to: String, msg: String): Boolean {
        val response = client.post(url("/chat/send")) {
            contentType(ContentType.Application.Json)
            setBody(ChatSendRequest(token, to, msg))
        }
        val text = response.body<String>()
        val obj = jsonParser.decodeFromString<SendResponse>(text)
        return obj.send
    }

    suspend fun poll(token: String): List<IncomingMessage> {
        val response = client.post(url("/chat/poll")) {
            contentType(ContentType.Application.Json)
            setBody(PollRequest(token))
        }
        val text = response.body<String>()
        val obj = jsonParser.decodeFromString<MessageListResponse>(text)
        return obj.messages
    }

    suspend fun logout(token: String) {
        try {
            client.post(url("/user/logout")) {
                contentType(ContentType.Application.Json)
                setBody(LogoutRequest(token))
            }
        } catch (e: Exception) {
            println("Logout warning: ${e.message}")
        }
    }
}