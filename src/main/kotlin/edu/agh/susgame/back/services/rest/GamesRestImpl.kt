package edu.agh.susgame.back.services.rest

import edu.agh.susgame.back.domain.models.Game
import edu.agh.susgame.dto.rest.games.GamesRest
import edu.agh.susgame.dto.rest.games.model.CreateGameApiResult
import edu.agh.susgame.dto.rest.games.model.GetAllGamesApiResult
import edu.agh.susgame.dto.rest.games.model.GetGameApiResult
import edu.agh.susgame.dto.rest.games.model.GetGameMapApiResult
import edu.agh.susgame.dto.rest.model.LobbyId
import edu.agh.susgame.dto.rest.model.LobbyPin
import edu.agh.susgame.dto.socket.common.GameStatus
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class GamesRestImpl : GamesRest {
    private val gameStorage = GameStorage(
        games = listOf(
            Game(
                name = "Gra Michała",
                id = 0,
                maxNumberOfPlayers = 6,
            ),
            Game(
                name = "Gra Jurka",
                id = 1,
                maxNumberOfPlayers = 5,
            ),
            Game(
                name = "Gra zabezpieczona pinem",
                id = 2,
                maxNumberOfPlayers = 6,
                gamePin = "pin",
            ),
        )
    )

    private val nextGameId: AtomicInteger = AtomicInteger(gameStorage.size())

    override fun getAllGames(): CompletableFuture<GetAllGamesApiResult> = CompletableFuture.supplyAsync {
        GetAllGamesApiResult.Success(gameStorage.getLobbyRows())
    }

    override fun getGameDetails(
        gameId: LobbyId,
        gamePin: LobbyPin?
    ): CompletableFuture<GetGameApiResult> = CompletableFuture.supplyAsync {
        gameStorage.findGameById(gameId.value)?.let { game ->
            try {
                game.checkPinMatch(gamePin?.value)
            } catch (e: IllegalArgumentException) {
                return@supplyAsync GetGameApiResult.InvalidPin
            }
            GetGameApiResult.Success(game.getLobbyDetails())
        } ?: GetGameApiResult.DoesNotExist
    }

    override fun createGame(
        gameName: String,
        maxNumberOfPlayers: Int,
        gamePin: LobbyPin?
    ): CompletableFuture<CreateGameApiResult> = CompletableFuture.supplyAsync {
        gameStorage.findGameByName(gameName)?.let {
            CreateGameApiResult.NameAlreadyExists
        } ?: run {
            val newGame = Game(gameName, nextGameId.getAndIncrement(), maxNumberOfPlayers, gamePin = gamePin?.value)
            gameStorage.add(newGame)
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
