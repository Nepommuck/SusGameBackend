package edu.agh.susgame.back.net

import edu.agh.susgame.dto.socket.server.EdgeDTO
import kotlin.random.Random

/**
 * Represents an edge in a graph.
 *
 * @param index The index of the edge (Supposed to be unique).
 * @param weight The weight of the edge.
 */

class Edge (
    private val index: Int,
    private var weight: Int
) {

    public fun getWeight() = weight
    //TODO Change the hardcoded value
    //TODO Implement in NetGraph to log amount of packets transported each turn
    public fun toDTO() = EdgeDTO(index, 2137, Random.nextInt(1, 11))
}