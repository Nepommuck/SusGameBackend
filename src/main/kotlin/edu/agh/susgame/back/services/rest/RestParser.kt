package edu.agh.susgame.back.services.rest

import edu.agh.susgame.back.domain.models.Game
import edu.agh.susgame.back.domain.net.NetGraph
import edu.agh.susgame.dto.rest.model.*
import edu.agh.susgame.dto.socket.ServerSocketMessage


object RestParser {

    fun gameToGameState(game: Game) = ServerSocketMessage.GameState(
        routers = game.netGraph.getRoutersList()
            .map { it.toDTO() },
        server = game.netGraph.getServer().toDTO(),
        hosts = game.netGraph.getHostsList().map { it.toDTO() },
        edges = game.netGraph.getEdges().map { it.toDTO() },
        players = game.getPlayers().toMap().values.map { it.toDTO() },
        gameStatus = game.getGameStatus(),
    )

    fun netGraphToGetGameMapDTO(netGraph: NetGraph): GameMapDTO = GameMapDTO(
        server = GameMapServerDTO(
            id = netGraph.getServer().index,
            coordinates = netGraph.getServer().getCoordinates(),
        ),
        hosts = netGraph.getHostsList().map { host ->
            GameMapHostDTO(
                id = host.index,
                coordinates = host.getCoordinates(),
                playerId = host.getPlayer().index
            )
        },
        routers = netGraph.getRoutersList().map { router ->
            GameMapRouterDTO(
                id = router.index,
                coordinates = router.getCoordinates(),
                bufferSize = router.getBufferSize(),
            )
        },
        edges = netGraph.getEdges().toList().map { edge ->
            val (fromNodeId, toNodeId) = edge.connectedNodesIds
            GameMapEdgeDTO(
                from = fromNodeId,
                to = toNodeId,
                weight = edge.getWeight()
            )
        },
    )
}
