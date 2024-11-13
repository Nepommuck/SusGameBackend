package edu.agh.susgame.back.rest.games

import edu.agh.susgame.back.socket.GamesWebSocketConnection
import edu.agh.susgame.back.net.BFS
import edu.agh.susgame.back.net.Player
import edu.agh.susgame.back.rest.games.GamesRestImpl.DeleteGameResult
import edu.agh.susgame.config.BFS_FREQUENCY
import edu.agh.susgame.config.CLIENT_REFRESH_FREQUENCY
import edu.agh.susgame.config.GAME_QUESTION_SENDING_INTERVAL
import edu.agh.susgame.dto.rest.games.model.CreateGameApiResult
import edu.agh.susgame.dto.rest.games.model.GameCreationRequest
import edu.agh.susgame.dto.rest.games.model.GetAllGamesApiResult
import edu.agh.susgame.dto.rest.games.model.GetGameApiResult
import edu.agh.susgame.dto.rest.model.LobbyId
import edu.agh.susgame.dto.socket.ClientSocketMessage
import edu.agh.susgame.dto.socket.ServerSocketMessage
import edu.agh.susgame.dto.socket.common.GameStatus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
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

        post {
            val request = call.receive<GameCreationRequest>()

            val result = gamesRestImpl.createGame(request.gameName, request.maxNumberOfPlayers, request.gamePin).await()

            call.respond(
                status = result.let { HttpStatusCode.fromValue(it.responseCode) },
                message = when (result) {
                    is CreateGameApiResult.Success -> result.createdLobbyId
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
            val thisPlayer = Player(index = game.getNextPlayerIdx(), name = playerName)
            game.addPlayer(thisConnection, newPlayer = thisPlayer)
            try {
                for (frame in incoming) {
                    val playerMap = game.getPlayers()
                    frame as? Frame.Binary ?: continue

                    when (val receivedMessage = Cbor.decodeFromByteArray<ClientSocketMessage>(frame.data)) {
                        // Handle lobby
                        is ClientSocketMessage.PlayerJoiningRequest -> game.handlePlayerJoiningRequest(thisConnection, thisPlayer)

                        is ClientSocketMessage.PlayerChangeReadinessRequest -> {}

                        is ClientSocketMessage.PlayerLeavingRequest -> game.handlePlayerLeavingRequest(thisConnection, thisPlayer)


                        // Handle game
                        is ClientSocketMessage.ChatMessage -> {
                            playerMap.forEach {
                                val connection = it.key
                                val playerNickname = it.value

                                if (connection != thisConnection) {
                                    connection.sendServerSocketMessage(
                                        ServerSocketMessage.ChatMessage(
                                            authorNickname = playerNickname.name,
                                            message = receivedMessage.message,
                                        )
                                    )
                                }
                            }
                        }

                        is ClientSocketMessage.GameState -> {
                            when (receivedMessage.gameStatus) {
                                GameStatus.WAITING ->
                                    thisConnection.sendServerSocketMessage(
                                        ServerSocketMessage.ServerError(
                                            errorMessage = "Game cannot be set to WAITING by client!",
                                        )
                                    )

                                GameStatus.RUNNING -> {
                                    when (game.gameStatus) {
                                        GameStatus.WAITING -> {
                                            game.startGame()
                                            // sends game status updates to all players
                                            launch {
                                                while (game.gameStatus == GameStatus.RUNNING) {
                                                    game.endGameIfPossible()
                                                    playerMap.keys.forEach { connection ->
                                                        connection.sendServerSocketMessage(
                                                            ServerSocketMessageParser.gameToGameState(game)
                                                        )
                                                    }
                                                    kotlinx.coroutines.delay(CLIENT_REFRESH_FREQUENCY)
                                                }
                                            }

                                            // performs BFS on the graph
                                            val bfs = BFS(game.gameGraph, game.gameGraph.getServer())
                                            launch {
                                                while (game.gameStatus == GameStatus.RUNNING) {
                                                    kotlinx.coroutines.delay(BFS_FREQUENCY)
                                                    game.addMoneyPerIterationForAllPlayers()
                                                    bfs.run()
                                                }
                                            }
                                            // sends questions to players
                                            launch {
                                                while (game.gameStatus == GameStatus.RUNNING) {
                                                    game.getPlayers().toMap().forEach { (connection, player) ->
                                                        val (questionId, question) = game.getRandomQuestion()
                                                        player.activeQuestionId = questionId
                                                        connection.sendServerSocketMessage(
                                                            ServerSocketMessage.QuizQuestionDTO(
                                                                questionId = questionId,
                                                                question = question.question,
                                                                answers = question.answers,
                                                                correctAnswer = question.correctAnswer,
                                                            )
                                                        )
                                                    }
                                                    kotlinx.coroutines.delay(GAME_QUESTION_SENDING_INTERVAL)
                                                }
                                            }
                                        }

                                        else -> thisConnection.sendServerSocketMessage(
                                            ServerSocketMessage.ServerError(
                                                errorMessage = "Game cannot be set to FINISHED by client!",
                                            )
                                        )
                                    }
                                }

                                GameStatus.FINISHED_WON, GameStatus.FINISHED_LOST->
                                    thisConnection.sendServerSocketMessage(
                                        ServerSocketMessage.ServerError(
                                            errorMessage = "Game cannot be set to FINISHED by client!",
                                        )
                                    )
                            }
                        }

                        is ClientSocketMessage.HostDTO -> {
                            when (game.gameStatus) {
                                GameStatus.RUNNING -> when (val host = game.gameGraph.getHost(receivedMessage.id)) {
                                    null -> thisConnection.sendServerSocketMessage(
                                        ServerSocketMessage.ServerError(
                                            "There is no host with id of ${receivedMessage.id}"
                                        )
                                    )

                                    else -> try {
                                        val route = receivedMessage.packetPath.flatMap { nodeId ->
                                            when (val node = game.gameGraph.getNodeById(nodeId)) {
                                                null -> emptyList()
                                                else -> listOf(node)
                                            }
                                        }
                                        host.setRoute(route)

                                        host.setMaxPacketsPerTick(receivedMessage.packetsSentPerTick)
                                    } catch (e: IllegalArgumentException) {
                                        thisConnection.sendServerSocketMessage(
                                            ServerSocketMessage.ServerError(e.message ?: "Unknown error")
                                        )
                                    }
                                }

                                else -> thisConnection.sendServerSocketMessage(
                                    ServerSocketMessage.ServerError("Game is not in a running state")
                                )
                            }
                        }

                        is ClientSocketMessage.UpgradeDTO -> {
                            when (game.gameStatus) {
                                GameStatus.RUNNING -> try {
                                    val deviceIdToUpgrade = receivedMessage.deviceId

                                    val edge = game.gameGraph.getEdgeById(deviceIdToUpgrade)
                                    val router = game.gameGraph.getRouter(deviceIdToUpgrade)

                                    if (edge != null) {
                                        edge.upgradeWeight(thisPlayer)
                                    } else if (router != null) {
                                        router.upgradeBuffer(thisPlayer)
                                    } else {
                                        thisConnection.sendServerSocketMessage(
                                            ServerSocketMessage.ServerError(
                                                "There is neither an edge not a host with id of $deviceIdToUpgrade."
                                            )
                                        )
                                    }
                                } catch (e: IllegalStateException) {
                                    thisConnection.sendServerSocketMessage(
                                        ServerSocketMessage.ServerError(e.message ?: "Unknown error")
                                    )
                                }

                                else -> thisConnection.sendServerSocketMessage(
                                    ServerSocketMessage.ServerError("Game is not in running state.")
                                )
                            }
                        }

                        is ClientSocketMessage.QuizAnswerDTO -> {
                            val player = playerMap[thisConnection] ?: throw IllegalStateException("Player not found")
                            val question = game.getQuestionById(receivedMessage.questionId)
                            if (question.correctAnswer == receivedMessage.answer && receivedMessage.questionId == player.activeQuestionId) {
                                player.addMoneyForCorrectAnswer()
                            }
                        }
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
