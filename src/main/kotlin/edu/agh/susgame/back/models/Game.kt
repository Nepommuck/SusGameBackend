package edu.agh.susgame.back.models

import edu.agh.susgame.back.net.BFS
import edu.agh.susgame.back.net.Generator
import edu.agh.susgame.back.socket.GamesWebSocketConnection
import edu.agh.susgame.back.net.NetGraph
import edu.agh.susgame.back.net.Player
import edu.agh.susgame.back.rest.games.RestParser
import edu.agh.susgame.config.*
import edu.agh.susgame.dto.rest.model.*
import edu.agh.susgame.dto.socket.ClientSocketMessage
import edu.agh.susgame.dto.socket.ServerSocketMessage
import edu.agh.susgame.dto.socket.common.GameStatus
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class Game(
    val name: String,
    val id: Int,
    val maxNumberOfPlayers: Int,
    val gamePin: String? = null,
    private val gameLength: Long = GAME_TIME_DEFAULT,
    private val gameGoal: Int = GAME_DEFAULT_PACKETS_DELIVERED_GOAL,
    ) {

    @Volatile
    var gameStatus: GameStatus = GameStatus.WAITING

    private val playerMap: MutableMap<GamesWebSocketConnection, Player> = ConcurrentHashMap()
    private var nextPlayerIdx : AtomicInteger = AtomicInteger(0)

    var netGraph: NetGraph? = null
    var bfs: BFS? = null
    private var startTime: Long = -1

    fun addPlayer(connection: GamesWebSocketConnection, newPlayer: Player) {
        if (playerMap.values.any { it.name == newPlayer.name }) {
            throw IllegalArgumentException("Player with name $newPlayer.name already exists")
        }
        playerMap[connection] = newPlayer
    }

    fun removePlayer(playerName: String) {
        playerMap.entries.removeIf { it.value.name == playerName }
    }

    fun getDataToReturn(): Lobby {
        return Lobby(
            id = LobbyId(id),
            name = name,
            maxNumOfPlayers = maxNumberOfPlayers,
            // TODO GAME-74 Remove this hardcoded value
            gameTime = 10,
            playersWaiting = playerMap.values.map { it.toREST() },
        )
    }

    fun getPlayers(): Map<GamesWebSocketConnection, Player> = playerMap.toMap()

    fun getNextPlayerIdx(): Int = nextPlayerIdx.getAndIncrement()

    fun addMoneyPerIterationForAllPlayers() {
        playerMap.values.forEach { it.addMoneyPerIteration() }
    }

    fun areAllPlayersReady() = playerMap.values.all { it.isReady() }

    /*
    * Starts the game by generating the graph, setting the start time and changing the game status to running
     */
    fun startGame(graph: NetGraph? = null) {
        gameStatus = GameStatus.RUNNING
        netGraph = graph ?: Generator.getGraph(playerMap.values.toList())
        bfs = BFS(net = netGraph!!, root = netGraph!!.getServer())
        startTime = System.currentTimeMillis()
    }

    /**
     * Ends the game if certain conditions are met.
     *
     * The game will end if:
     * - The total packets delivered to the server(s) are greater than or equal to the game goal.
     * - The current time exceeds the game length since the start time.
     *
     * The game status will be updated to either FINISHED_WON or FINISHED_LOST based on the conditions.
     */
    fun endGameIfPossible() {
        gameStatus = when {
            netGraph!!.getTotalPacketsDelivered() >= gameGoal -> {
                GameStatus.FINISHED_WON
            }

            System.currentTimeMillis() - startTime > gameLength -> {
                GameStatus.FINISHED_LOST
            }

            else -> {
                gameStatus
            }
        }
    }

    fun getRandomQuestion(): Pair<Int, QuizQuestion> {
        val randomIndex = QuizQuestions.indices.random()
        return Pair(randomIndex, QuizQuestions[randomIndex])
    }

    fun getQuestionById(questionId: Int): QuizQuestion {
        return QuizQuestions[questionId]
    }

    /**
     * ##################################################
     * HANDLERS
     * ##################################################
     *
     * Lobby handlers
     */

    suspend fun handlePlayerJoiningRequest(thisConnection: GamesWebSocketConnection, thisPlayer: Player) {
        playerMap
            .filter { it.key != thisConnection }
            .forEach { (connection, _) ->
                connection.sendServerSocketMessage(
                    ServerSocketMessage.PlayerJoiningResponse(playerId = thisPlayer.index, playerName = thisPlayer.name)
                )
            }
    }

    suspend fun handlePlayerChangeReadinessRequest(thisConnection: GamesWebSocketConnection, thisPlayer: Player, receivedMessage: ClientSocketMessage.PlayerChangeReadinessRequest) {
        val readinessState: Boolean = receivedMessage.state
        thisPlayer.setReadinessState(readinessState)
        playerMap
            .filter { it.key != thisConnection }
            .forEach { (connection, _) ->
                connection.sendServerSocketMessage(
                    ServerSocketMessage.PlayerChangeReadinessResponse(playerId = thisPlayer.index, state = readinessState)
                )
            }
    }

    suspend fun handlePlayerLeavingRequest(thisConnection: GamesWebSocketConnection, thisPlayer: Player){
        playerMap
            .filter { it.key != thisConnection }
            .forEach { (connection, _) ->
                connection.sendServerSocketMessage(
                    ServerSocketMessage.PlayerLeavingResponse(playerId = thisPlayer.index)
                )
            }
    }

    /**
     * Game handlers
     */

    suspend fun handleChatMessage(thisConnection: GamesWebSocketConnection, thisPlayer: Player, receivedMessage: ClientSocketMessage.ChatMessage) {
        playerMap
            .filter { it.key != thisConnection }
            .forEach { (connection, _) ->
                connection.sendServerSocketMessage(
                    ServerSocketMessage.ChatMessage( authorNickname = thisPlayer.name, message = receivedMessage.message,)
                )
            }
    }

    suspend fun handleGameState(receivedGameStatus: ClientSocketMessage.GameState, webSocket: WebSocketSession) {
        if (!receivedGameStatus.gameStatus.equals(GameStatus.RUNNING)) {
            sendErrorMessage("Invalid game status sent by client")
            return
        }

        if (!gameStatus.equals(GameStatus.WAITING)) {
            sendErrorMessage("Invalid game status on server")
            return
        }

        if (areAllPlayersReady()) {
            gameStatus = GameStatus.RUNNING
            startGame()

            broadcastStateThread(webSocket)
            runEngineIterationThread(webSocket)
            quizQuestionsThread(webSocket)
        } else {
            sendErrorMessage("Not all players are ready")
        }



    }

    suspend fun handleHostDTO(thisConnection: GamesWebSocketConnection, receivedMessage: ClientSocketMessage.HostDTO) {
        if (!gameStatus.equals(GameStatus.RUNNING)) {
            sendErrorMessage("Invalid game status on server: Game is not running")
            return
        }
        val host = netGraph!!.getHost(receivedMessage.id)

        if (host == null) {
            sendErrorMessage("There is no host with id of ${receivedMessage.id}")
            return
        }

       try {
            val route = receivedMessage.packetPath.flatMap { nodeId ->
                when (val node = netGraph!!.getNodeById(nodeId)) {
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

    suspend fun handleUpgradeDTO(thisConnection: GamesWebSocketConnection, receivedMessage: ClientSocketMessage.UpgradeDTO, thisPlayer: Player) {
        if (!gameStatus.equals(GameStatus.RUNNING)) {
            sendErrorMessage("Invalid game status on server: Game is not running")
            return
        }
        try {
            val deviceIdToUpgrade = receivedMessage.deviceId

            val edge = netGraph!!.getEdgeById(deviceIdToUpgrade)
            val router = netGraph!!.getRouter(deviceIdToUpgrade)

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
    }

    /**
     * Other messages
     */
    suspend fun sendErrorMessage(errorMessage: String) {
        playerMap
            .forEach { (connection, _) ->
                connection.sendServerSocketMessage(
                    ServerSocketMessage.ServerError(errorMessage=errorMessage)
                )
            }
    }

    /**
     * ##################################################
     * THREADS
     * ##################################################
     */

    private fun broadcastStateThread(webSocket: WebSocketSession) {
        webSocket.launch {
            while (gameStatus == GameStatus.RUNNING) {
                endGameIfPossible()
                playerMap.forEach { (connection, _) ->
                    connection.sendServerSocketMessage(
                        RestParser.gameToGameState(this@Game)
                    )
                }
                delay(CLIENT_REFRESH_FREQUENCY)  // delay should be used from kotlinx.coroutines
            }
        }
    }

    private fun runEngineIterationThread(webSocket: WebSocketSession) {
        webSocket.launch {
            while (gameStatus == GameStatus.RUNNING) {
                delay(BFS_FREQUENCY)
                addMoneyPerIterationForAllPlayers()
                bfs!!.run()
            }
        }
    }

    private fun quizQuestionsThread(webSocket: WebSocketSession) {
        webSocket.launch {
            while (gameStatus == GameStatus.RUNNING) {
                playerMap.forEach { (connection, player) ->
                    val (questionId, question) = getRandomQuestion()
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
                delay(GAME_QUESTION_SENDING_INTERVAL)
            }
        }
    }

}
