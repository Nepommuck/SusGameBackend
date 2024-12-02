package edu.agh.susgame.back.services.rest

import edu.agh.susgame.back.domain.models.Game
import edu.agh.susgame.dto.rest.games.GamesRest
import edu.agh.susgame.dto.rest.games.model.CreateGameApiResult
import edu.agh.susgame.dto.rest.games.model.GetAllGamesApiResult
import edu.agh.susgame.dto.rest.games.model.GetGameApiResult
import edu.agh.susgame.dto.rest.games.model.GetGameMapApiResult
import edu.agh.susgame.dto.rest.model.LobbyId
import edu.agh.susgame.dto.socket.common.GameStatus
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class GamesRestImpl : GamesRest {
    private val gameStorage = GameStorage(
        games = listOf(
            Game(
                name = "Gra do testowania v0.1 engine",
                id = 0,
                maxNumberOfPlayers = 4,
            ),
            Game(
                name = "Gra nr 1",
                id = 1,
                maxNumberOfPlayers = 5,
            ),
            Game(
                name = "Gra inna",
                id = 2,
                maxNumberOfPlayers = 6,
                gamePin = "pin",
            ),
            Game(
                name = "Gra III",
                id = 3,
                maxNumberOfPlayers = 3,
            ),
        )
    )

    private val nextGameId: AtomicInteger = AtomicInteger(gameStorage.size())

    override fun getAllGames(): CompletableFuture<GetAllGamesApiResult> = CompletableFuture.supplyAsync {
        GetAllGamesApiResult.Success(gameStorage.getReturnableData())
    }

    override fun getGame(gameId: LobbyId): CompletableFuture<GetGameApiResult> = CompletableFuture.supplyAsync {
        gameStorage.findGameById(gameId.value)?.let { game ->
            GetGameApiResult.Success(game.getDataToReturn())
        } ?: GetGameApiResult.DoesNotExist
    }

    override fun createGame(
        gameName: String,
        maxNumberOfPlayers: Int,
        gamePin: String?
    ): CompletableFuture<CreateGameApiResult> = CompletableFuture.supplyAsync {
        gameStorage.findGameByName(gameName)?.let {
            CreateGameApiResult.NameAlreadyExists
        } ?: run {
            val newGame = Game(gameName, nextGameId.getAndIncrement(), maxNumberOfPlayers, gamePin)
            gameStorage.add(newGame)
            // TODO GAME-74 Suggestion: Propagate the usage of `LobbyId` on backend
            CreateGameApiResult.Success(createdLobbyId = LobbyId(newGame.id))
        }
    }

    override fun getGameMap(gameId: LobbyId): CompletableFuture<GetGameMapApiResult> = CompletableFuture.supplyAsync {
        val game = gameStorage.findGameById(gameId.value) ?: return@supplyAsync GetGameMapApiResult.GameDoesNotExist

        if (game.getGameStatus() != GameStatus.RUNNING) return@supplyAsync GetGameMapApiResult.GameNotYetStarted

        val netGraph = game.netGraph

        return@supplyAsync GetGameMapApiResult.Success(
            gameMap = RestParser.netGraphToGetGameMapDTO(game, netGraph),
        )
    }

    fun findGameById(gameId: Int): Game? = gameStorage.findGameById(gameId)

    sealed class DeleteGameResult {
        data object Success : DeleteGameResult()
        data object GameDoesNotExist : DeleteGameResult()
    }

    // This method is not a part of contract (frontend doesn't use it), but it can stay for now
    fun deleteGame(gameId: Int): DeleteGameResult {

        if (!gameStorage.exists(gameId)) {
            return DeleteGameResult.GameDoesNotExist
        } else {
            gameStorage.remove(gameId)
            return DeleteGameResult.Success
        }
    }
}
