// WARNING: THIS FILE WAS CLONED AUTOMATICALLY FROM 'SusGameDTO' GITHUB REPOSITORY
// IT SHOULD NOT BE EDITED IN ANY WAY
// IN ORDER TO CHANGE THIS DTO, COMMIT TO 'SusGameDTO' GITHUB REPOSITORY
// IN ORDER TO UPDATE THIS FILE TO NEWEST VERSION, RUN 'scripts/update-DTO.sh'

package edu.agh.susgame.dto.socket

import edu.agh.susgame.dto.common.ColorDTO
import edu.agh.susgame.dto.socket.common.GameStatus
import kotlinx.serialization.Serializable

/**
 * Message sent by client via WebSocket
 */
@Serializable
sealed class ClientSocketMessage {
    /**
     * Used for host configuration, change of selected path or number of packets generated per tick
     */
    @Serializable
    data class HostRouteDTO(
        val id: Int,
        val packetPath: List<Int>,
    ) : ClientSocketMessage()

    @Serializable
    data class HostFlowDTO(
        val id: Int,
        val packetsSentPerTick: Int,
    ) : ClientSocketMessage()

    /**
     * Used when the player wants to update given device
     */
    @Serializable
    data class UpgradeDTO(
        val deviceId: Int,
    ) : ClientSocketMessage()

    @Serializable
    data class FixRouterDTO(
        val deviceId: Int,
    ) : ClientSocketMessage()

    /**
     * Used to initialize a game
     */
    @Serializable
    data class GameState(
        val gameStatus: GameStatus,
    ) : ClientSocketMessage()

    /**
     * Used for chat feature, which is planned to be deleted in the future
     */
    @Serializable
    data class ChatMessage(
        val message: String,
    ) : ClientSocketMessage()

    /**
     * Used for answering the quiz question
     */
    @Serializable
    data class QuizAnswerDTO(
        val questionId: Int,
        val answer: Int,
    ) : ClientSocketMessage()

    /**
     * Used for handling player changing state in lobby
     */
    @Serializable
    data class PlayerChangeReadiness(
        val playerId: Int,
        val state: Boolean
    ) : ClientSocketMessage()

    @Serializable
    data class PlayerChangeColor(
        val playerId: Int,
        val color: ColorDTO,
    ) : ClientSocketMessage()

    /**
     * Used for handling player leaving lobby
     */
    @Serializable
    data class PlayerLeaving(
        val playerId: Int
    ) : ClientSocketMessage()
}
