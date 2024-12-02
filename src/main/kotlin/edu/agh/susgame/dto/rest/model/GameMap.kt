// WARNING: THIS FILE WAS CLONED AUTOMATICALLY FROM 'SusGameDTO' GITHUB REPOSITORY
// IT SHOULD NOT BE EDITED IN ANY WAY
// IN ORDER TO CHANGE THIS DTO, COMMIT TO 'SusGameDTO' GITHUB REPOSITORY
// IN ORDER TO UPDATE THIS FILE TO NEWEST VERSION, RUN 'scripts/update-DTO.sh'

package edu.agh.susgame.dto.rest.model

import kotlinx.serialization.Serializable


@Serializable
data class GameMapDTO(
    val server: GameMapServerDTO,
    val hosts: List<GameMapHostDTO>,
    val routers: List<GameMapRouterDTO>,
    val edges: List<GameMapEdgeDTO>,
    /**
     * Number of packets that server must receive to win.
     */
    val gameGoal: Int,
    /**
     * The number of iterations required for a router with a full buffer to overheat.
     */
    val criticalBufferOverheatLevel: Int,
)

@Serializable
data class Coordinates(val x: Int, val y: Int)


@Serializable
data class GameMapServerDTO(
    val id: Int,
    val coordinates: Coordinates,
)

@Serializable
data class GameMapHostDTO(
    val playerId: Int,
    val id: Int,
    val coordinates: Coordinates,
)

@Serializable
data class GameMapRouterDTO(
    val id: Int,
    val coordinates: Coordinates,
    val bufferSize: Int,
)

@Serializable
data class GameMapEdgeDTO(val from: Int, val to: Int, val weight: Int)
