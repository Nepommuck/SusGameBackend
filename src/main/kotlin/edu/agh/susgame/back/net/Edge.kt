package edu.agh.susgame.back.net

import edu.agh.susgame.dto.socket.server.EdgeDTO
import edu.agh.susgame.config.*

/**
 * Represents an edge in a graph.
 *
 * @param index The index of the edge (Supposed to be unique).
 * @param weight The weight of the edge.
 */

class Edge(
    val index: Int,
    private var weight: Int,
    var transportedPacketsThisTick: Int = 0,
    private var upgradeCost: Int = EDGE_DEFAULT_UPGRADE_COST
) {

    fun getWeight() = weight

    fun toDTO() = EdgeDTO(index, upgradeCost, transportedPacketsThisTick)

    fun getUpgradeCost() = upgradeCost

    /**
     * Upgrades the weight of the edge.
     * Increases the weight and upgrade cost of the edge by a predefined coefficients.
     */
    fun upgradeWeight(player: Player) {
        if (player.getCurrentMoney() < upgradeCost) {
            throw IllegalStateException("Player does not have enough money to upgrade the edge")
        }
        player.setCurrentMoney(player.getCurrentMoney() - upgradeCost)
        weight = nextEdgeWeight(weight)
        upgradeCost = nextEdgeUpgradeCost(upgradeCost)
    }
}

