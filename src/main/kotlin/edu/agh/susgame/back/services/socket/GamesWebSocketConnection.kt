package edu.agh.susgame.back.services.socket

import edu.agh.susgame.dto.socket.ServerSocketMessage
import io.ktor.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray


class GamesWebSocketConnection(private val session: DefaultWebSocketSession) {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sendServerSocketMessage(serverSocketMessage: ServerSocketMessage) {
        session.send(
            Cbor.encodeToByteArray(serverSocketMessage)
        )
    }
}
