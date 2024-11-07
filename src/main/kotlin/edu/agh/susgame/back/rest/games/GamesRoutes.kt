package edu.agh.susgame.back.rest.games

import edu.agh.susgame.back.net.Generator
import edu.agh.susgame.back.Connection
import edu.agh.susgame.back.net.BFS
import edu.agh.susgame.back.net.Player
import edu.agh.susgame.back.net.node.Node
import edu.agh.susgame.back.rest.games.GamesRestImpl.DeleteGameResult
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
import kotlinx.serialization.encodeToByteArray
import edu.agh.susgame.config.*

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

            call.respond(gamesRestImpl.createGame(request.gameName, request.maxNumberOfPlayers, request.gamePin))
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
            val game = gamesRestImpl.gameStorage.findGameById(gameId)
            if (game == null) {
                close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        HttpErrorResponseBody("Game with id $gameId not found").toString()
                    )
                )
                return@webSocket
            }

            val thisConnection = Connection(this)
            val thisPlayer = Player(index = game.getPlayers().size, name = playerName)
            game.addPlayer(thisConnection, newPlayer = thisPlayer)
            try {
                for (frame in incoming) {
                    val playerMap = game.getPlayers()
                    frame as? Frame.Binary ?: continue

                    when (val receivedMessage = Cbor.decodeFromByteArray<ClientSocketMessage>(frame.data)) {
                        is ClientSocketMessage.ChatMessage -> {
                            playerMap.forEach {
                                val connection = it.key
                                val playerNickname = it.value.name

                                if (connection != thisConnection) {
                                    val serverMessage: ServerSocketMessage = ServerSocketMessage.ChatMessage(
                                        authorNickname = playerNickname,
                                        message = receivedMessage.message,
                                    )

                                    val encodedServerMessage = Cbor.encodeToByteArray(serverMessage)
                                    connection.session.send(encodedServerMessage)
                                }
                            }
                        }

                        is ClientSocketMessage.GameState -> {
                            when (receivedMessage.gameStatus) {
                                GameStatus.WAITING -> {
                                    val serverMessage: ServerSocketMessage = ServerSocketMessage.ServerError(
                                        errorMessage = "Game cannot be set to WAITING by client!",
                                    )
                                    val encodedServerMessage = Cbor.encodeToByteArray(serverMessage)
                                    thisConnection.session.send(encodedServerMessage)
                                }

                                GameStatus.RUNNING -> {
                                    when (game.gameStatus) {
                                        GameStatus.WAITING -> {
                                            game.gameStatus = GameStatus.RUNNING
                                            game.gameGraph = Generator.getGraph(playerMap.values.toList())
                                            // sends game status updates to all players
                                            game.gameStartTime = System.currentTimeMillis()
                                            launch {
                                                while (game.gameStatus == GameStatus.RUNNING) {
                                                    val gameStateMessage: ServerSocketMessage =
                                                        ServerSocketMessage.GameState(
                                                            routers = game.gameGraph.getRoutersList()
                                                                .map { it.toDTO() },
                                                            servers = game.gameGraph.getServersList()
                                                                .map { it.toDTO() },
                                                            hosts = game.gameGraph.getHostsList().map { it.toDTO() },
                                                            edges = game.gameGraph.getEdges().map { it.toDTO() },
                                                            players = playerMap.values.map { it.toDTO() },
                                                            gameStatus = game.gameStatus,
                                                        )
                                                    val encodedServerMessage = Cbor.encodeToByteArray(gameStateMessage)

                                                    playerMap.keys.forEach { it.session.send(encodedServerMessage) }
                                                    kotlinx.coroutines.delay(CLIENT_REFRESH_FREQUENCY)
                                                }
                                            }

                                            val bfs = BFS(game.gameGraph, game.gameGraph.getServersList()[0])
                                            launch {
                                                while (game.gameStatus == GameStatus.RUNNING) {

                                                    if (game.endGame(game.gameGraph.getTotalPacketsDelivered())) {

                                                    }
                                                    kotlinx.coroutines.delay(BFS_FREQUENCY)
                                                    game.addMoneyForAllPlayers()
                                                    bfs.run()
                                                }
                                            }
                                        }

                                        else -> {
                                            val serverMessage: ServerSocketMessage = ServerSocketMessage.ServerError(
                                                errorMessage = "Current game status: ${game.gameStatus}. " +
                                                        "Game can be started only from WAITING status.",
                                            )
                                            val encodedServerMessage = Cbor.encodeToByteArray(serverMessage)
                                            thisConnection.session.send(encodedServerMessage)
                                        }
                                    }

                                }

                                GameStatus.FINISHED -> {
                                    val serverMessage: ServerSocketMessage = ServerSocketMessage.ServerError(
                                        errorMessage = "Game cannot be set to FINISHED by client!",
                                    )
                                    val encodedServerMessage = Cbor.encodeToByteArray(serverMessage)
                                    thisConnection.session.send(encodedServerMessage)
                                }
                            }
                        }

                        is ClientSocketMessage.HostDTO -> {
                            when (game.gameStatus) {
                                //TODO implement support for packets per tick

                                GameStatus.RUNNING -> {
                                    val host = game.gameGraph.getHost(receivedMessage.id)
                                    if (host == null) {
                                        thisConnection.session.send(
                                            Cbor.encodeToByteArray(
                                                ServerSocketMessage.ServerError("There is no host with id of ${receivedMessage.id}.")
                                            )
                                        )
                                    } else {
                                        try {
                                            val allNodes = game.gameGraph.getNodes()
                                            val route =
                                                receivedMessage.packetPath.map { index -> allNodes.firstOrNull { it.index == index } }
                                            host.setRoute(route as List<Node>)
                                            host.setMaxPacketsPerTick(receivedMessage.packetsSentPerTick)
                                        } catch (e: IllegalArgumentException) {
                                            thisConnection.session.send(
                                                Cbor.encodeToByteArray(
                                                    ServerSocketMessage.ServerError(e.message ?: "Unknown error")
                                                )
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    thisConnection.session.send(Cbor.encodeToByteArray(ServerSocketMessage.ServerError("Game is not in running state.")))
                                }
                            }
                        }

                        is ClientSocketMessage.UpgradeDTO -> {
                            when (game.gameStatus) {
                                GameStatus.RUNNING -> {
                                    try {
                                        val deviceIdToUpgrade = receivedMessage.deviceId
                                        val edge =
                                            game.gameGraph.getEdges().firstOrNull { it.index == deviceIdToUpgrade }
                                        val router =
                                            game.gameGraph.getRoutersList()
                                                .firstOrNull { it.index == deviceIdToUpgrade }
                                        if (edge != null) {
                                            edge.upgradeWeight(thisPlayer)
                                        } else if (router != null) {
                                            router.upgradeBuffer(thisPlayer)
                                        } else {
                                            thisConnection.session.send(
                                                Cbor.encodeToByteArray(
                                                    ServerSocketMessage.ServerError("There is no edge or host with id of $deviceIdToUpgrade.")
                                                )
                                            )
                                        }
                                    } catch (e: IllegalStateException) {
                                        thisConnection.session.send(
                                            Cbor.encodeToByteArray(
                                                ServerSocketMessage.ServerError(e.message ?: "Unknown error")
                                            )
                                        )
                                    }
                                }


                                else -> {
                                    thisConnection.session.send(Cbor.encodeToByteArray(ServerSocketMessage.ServerError("Game is not in running state.")))
                                }
                            }
                        }

                        else -> {}
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
