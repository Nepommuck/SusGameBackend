// WARNING: THIS FILE WAS CLONED AUTOMATICALLY FROM 'SusGameDTO' GITHUB REPOSITORY
// IT SHOULD NOT BE EDITED IN ANY WAY
// IN ORDER TO CHANGE THIS DTO, COMMIT TO 'SusGameDTO' GITHUB REPOSITORY
// IN ORDER TO UPDATE THIS FILE TO NEWEST VERSION, RUN 'scripts/update-DTO.sh'

package edu.agh.susgame.dto.socket

import edu.agh.susgame.dto.common.ColorDTO
import edu.agh.susgame.dto.socket.common.GameStatus
import edu.agh.susgame.dto.socket.server.*
import kotlinx.serialization.Serializable

/**
 * Message sent by server via WebSocket
 */
@Serializable
sealed class ServerSocketMessage {
    /**
     * Informs the client about its ID
     */
    @Serializable
    data class IdConfig(
        val id: Int
    ) : ServerSocketMessage()

    /**
     *
     */
    @Serializable
    data class GameStarted(
        val id: Int
    ) : ServerSocketMessage()

    /**
     * Information about the whole game state that is coming periodically from the server
     */
    @Serializable
    data class GameState(
        val routers: List<RouterDTO>,
        val hosts: List<HostDTO>,
        val server: ServerDTO,
        val edges: List<EdgeDTO>,
        val players: List<PlayerDTO>,
        val gameStatus: GameStatus,
        val remainingSeconds: Int,
    ) : ServerSocketMessage()

    /**
     * Used to send error messages (for example in the case of invalid input)
     */
    @Serializable
    data class ServerError(
        val errorMessage: String,
    ) : ServerSocketMessage()

    /**
     * Used for chat feature, which is planned to be deleted in the future
     */
    @Serializable
    data class ChatMessage(
        val authorNickname: String,
        val message: String,
    ) : ServerSocketMessage()

    /**
     * Used for making a quiz question
     */
    @Serializable
    data class QuizQuestionDTO(
        val questionId: Int,
        val question: String,
        val answers: List<String>,
        val correctAnswer: Int,
    ) : ServerSocketMessage()

    /**
     * Used for informing other players about new one
     */
    @Serializable
    data class PlayerJoining(
        val playerId: Int,
        val playerName: String
    ) : ServerSocketMessage()

    /**
     * Used for handling player changing his state in lobby
     */
    @Serializable
    data class PlayerChangeReadiness(
        val playerId: Int,
        val state: Boolean
    ) : ServerSocketMessage()

    @Serializable
    data class PlayerChangeColor(
        val playerId: Int,
        val color: ColorDTO,
    ) : ServerSocketMessage()

    /**
     * Used for informing other players about other player leaving the lobby
     */
    @Serializable
    data class PlayerLeaving(
        val playerId: Int
    ) : ServerSocketMessage()
}
