package edu.agh.susgame.back.services.rest

import edu.agh.susgame.back.domain.models.Game
import edu.agh.susgame.dto.rest.model.Lobby
import edu.agh.susgame.dto.socket.common.GameStatus
import java.util.concurrent.ConcurrentHashMap

class GameStorage(games: List<Game> = emptyList()) {
    private val gameMap: ConcurrentHashMap<Int, Game> = ConcurrentHashMap<Int, Game>().apply {
        putAll(games.associateBy { it.id })
    }

    fun add(game: Game) {
        gameMap[game.id] = game
    }

    fun remove(id: Int) {
        gameMap.remove(id)
    }

    fun findGameById(id: Int): Game? = gameMap[id]

    fun findGameByName(gameName: String): Game? = gameMap.values.find { it.name == gameName }

    fun getReturnableData(): List<Lobby> = gameMap.values.filter { it.getGameStatus() == GameStatus.WAITING }.map { it.getDataToReturn() }

    fun size(): Int = gameMap.size

    fun exists(id: Int): Boolean = gameMap.containsKey(id)
}
