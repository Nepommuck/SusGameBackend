package edu.agh.susgame.back.domain.net

import edu.agh.susgame.back.domain.net.node.Node
import edu.agh.susgame.back.domain.net.node.Server

class Packet(
    private val player: Player,
    private val nodeList: List<Node>
) {
    private val route: ArrayDeque<Node> = ArrayDeque(nodeList)

    /**
     * Retrieves and deletes the first element in the route.
     * If the packet is currently directed to the server, the node is not deleted - Server is always supposed to be the final node.
     */
    fun popNext(): Node? {
        return if (next() is Server) {
            route.firstOrNull()
        } else {
            route.removeFirstOrNull()
        }
    }

    fun next(): Node? = route.firstOrNull()

    fun getPlayer(): Player = player

}