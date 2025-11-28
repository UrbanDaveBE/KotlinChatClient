package local.dev

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class TokenResponse(val token: String)

@Serializable
data class PingResponse(val ping: Boolean)

@Serializable
data class ChatSendRequest(val token: String, val username: String, val message: String)

@Serializable
data class SendResponse(val send: Boolean)

@Serializable
data class PollRequest(val token: String)

@Serializable
data class IncomingMessage(val username: String, val message: String)

@Serializable
data class MessageListResponse(val messages: List<IncomingMessage>)

@Serializable
data class OnlineUsersResponse(val online: List<String>)