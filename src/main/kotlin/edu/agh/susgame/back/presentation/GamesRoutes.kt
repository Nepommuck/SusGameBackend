package edu.agh.susgame.back.presentation

import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.back.services.rest.GamesRestImpl
import edu.agh.susgame.back.services.rest.GamesRestImpl.DeleteGameResult
import edu.agh.susgame.back.services.socket.GamesWebSocketConnection
import edu.agh.susgame.dto.rest.games.model.*
import edu.agh.susgame.dto.rest.model.LobbyId
import edu.agh.susgame.dto.socket.ClientSocketMessage
import edu.agh.susgame.dto.socket.ServerSocketMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

val gamesRestImpl = GamesRestImpl()

@Serializable
data class HttpErrorResponseBody(val errorMessage: String)

val HttpUnknownErrorResponseBody = HttpErrorResponseBody("Unknown error")

@OptIn(ExperimentalSerializationApi::class)
fun Route.gameRouting() {
    route("/games") {
        get {
            val result = gamesRestImpl.getAllGames().await()

            call.respond(
                status = result.let { HttpStatusCode.fromValue(it.responseCode) },
                message = when (result) {
                    is GetAllGamesApiResult.Success -> result.lobbies
                    GetAllGamesApiResult.Error -> HttpUnknownErrorResponseBody
                }
            )
        }

        get("{gameId}") {
            call.parameters["gameId"]
                ?.toInt()
                ?.let { LobbyId(it) }
                ?.let { lobbyId ->
                    val result = gamesRestImpl.getGame(lobbyId).await()

                    call.respond(
                        status = result.let { HttpStatusCode.fromValue(it.responseCode) },
                        message = when (result) {
                            is GetGameApiResult.Success -> result.lobby

                            GetGameApiResult.DoesNotExist ->
                                HttpErrorResponseBody("Game with ${lobbyId.value} not found")

                            GetGameApiResult.OtherError -> HttpUnknownErrorResponseBody
                        }
                    )
                }
        }

        get("map/{gameId}") {
            call.parameters["gameId"]
                ?.toInt()
                ?.let { LobbyId(it) }
                ?.let { lobbyId ->
                    val result = gamesRestImpl.getGameMap(lobbyId).await()

                    call.respond(
                        status = result.let { HttpStatusCode.fromValue(it.responseCode) },
                        message = when (result) {
                            is GetGameMapApiResult.Success -> result.gameMap

                            GetGameMapApiResult.GameDoesNotExist ->
                                HttpErrorResponseBody("Game ${lobbyId.value} was not found")

                            GetGameMapApiResult.GameNotYetStarted ->
                                HttpErrorResponseBody("Game ${lobbyId.value} is not yet started")

                            GetGameMapApiResult.OtherError -> HttpUnknownErrorResponseBody
                        }
                    )
                }
        }

        post {
            val request = call.receive<GameCreationRequest>()

            val result = gamesRestImpl.createGame(request.gameName, request.maxNumberOfPlayers, request.gamePin).await()

            call.respond(
                status = result.let { HttpStatusCode.fromValue(it.responseCode) },
                message = when (result) {
                    is CreateGameApiResult.Success -> GameCreationApiResponse(result.createdLobbyId)

                    CreateGameApiResult.NameAlreadyExists ->
                        HttpErrorResponseBody("Game with name ${request.gameName} already exists")

                    CreateGameApiResult.OtherError -> HttpUnknownErrorResponseBody
                }
            )
        }

        // This method is not a part of contract (frontend doesn't use it), but it can stay for now
        delete("{gameId}") {
            val gameId = call.parameters["gameId"]?.toInt()
            gameId?.let {
                when (gamesRestImpl.deleteGame(it)) {
                    DeleteGameResult.Success ->
                        call.respond(HttpStatusCode.OK)

                    DeleteGameResult.GameDoesNotExist ->
                        call.respond(HttpStatusCode.NotFound, HttpErrorResponseBody("Game with $gameId not found"))
                }
            }
        }

        webSocket("/join") {
            val gameId = call.request.queryParameters["gameId"]?.toIntOrNull()
            val playerName = call.request.queryParameters["playerName"]
            if (gameId == null || playerName == null) {
                close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        HttpErrorResponseBody("Missing gameName or playerName").toString()
                    )
                )
                return@webSocket
            }
            val game = gamesRestImpl.findGameById(gameId)
            if (game == null) {
                close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        HttpErrorResponseBody("Game with id $gameId not found").toString()
                    )
                )
                return@webSocket
            }

            val thisConnection = GamesWebSocketConnection(this)
            val playerIndex = game.getNextPlayerIdx()

            thisConnection.sendServerSocketMessage(ServerSocketMessage.IdConfig(playerIndex))

            val thisPlayer = Player(index = playerIndex, name = playerName)
            game.addPlayer(thisConnection, newPlayer = thisPlayer)

            game.handlePlayerJoiningRequest(thisConnection, thisPlayer)

            try {
                for (frame in incoming) {
                    val playerMap = game.getPlayers()
                    frame as? Frame.Binary ?: continue

                    when (val receivedMessage = Cbor.decodeFromByteArray<ClientSocketMessage>(frame.data)) {
                        // Handle lobby
                        is ClientSocketMessage.PlayerChangeReadiness -> game.handlePlayerChangeReadinessRequest(
                            thisConnection,
                            thisPlayer,
                            receivedMessage
                        )

                        is ClientSocketMessage.PlayerChangeColor -> game.handlePlayerChangeColor(
                            thisConnection,
                            thisPlayer,
                            receivedMessage
                        )

                        is ClientSocketMessage.PlayerLeaving -> game.handlePlayerLeavingRequest(
                            thisConnection,
                            thisPlayer
                        )

                        // Handle game
                        is ClientSocketMessage.ChatMessage -> game.handleChatMessage(
                            thisConnection,
                            thisPlayer,
                            receivedMessage
                        )

                        is ClientSocketMessage.GameState -> game.handleGameState(receivedMessage, this)

                        is ClientSocketMessage.HostRouteDTO -> game.handleHostRoute(thisConnection, receivedMessage)

                        is ClientSocketMessage.HostFlowDTO -> game.handleHostFlow(thisConnection, receivedMessage)

                        is ClientSocketMessage.UpgradeDTO -> game.handleUpgradeDTO(
                            thisConnection,
                            receivedMessage,
                            thisPlayer
                        )

                        is ClientSocketMessage.QuizAnswerDTO -> {
                            val player = playerMap[thisConnection] ?: throw IllegalStateException("Player not found")
                            val question = game.getQuestionById(receivedMessage.questionId)
                            if (question.correctAnswer == receivedMessage.answer && receivedMessage.questionId == player.activeQuestionId) {
                                player.addMoneyForCorrectAnswer()
                            }

                            if (question.correctAnswer != receivedMessage.answer && receivedMessage.questionId == player.activeQuestionId) {
                                player.activeQuestionId = -1
                            }

                        }

                        is ClientSocketMessage.FixRouterDTO -> game.handleFixRouterDTO(thisConnection, receivedMessage)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection!")
                game.removePlayer(playerName)
            }
        }
    }
}
