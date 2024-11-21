package edu.agh.susgame.back.domain.net.node

import edu.agh.susgame.back.domain.net.Edge
import edu.agh.susgame.back.domain.net.Packet
import edu.agh.susgame.dto.rest.model.Coordinates


/**
 * Abstract class representing the node in the net.
 *
 * @param index Index of the node. Supposed to be unique.
 */
abstract class Node {
    /**
     * Index of the node. Supposed to be unique.
     */
    internal abstract val index: Int
    abstract val coordinates: Pair<Int, Int>

    // LinkedHashMap for a fixed order for use in Round Robin algorithm
    internal val neighbors: LinkedHashMap<Node, Edge> = linkedMapOf()

    fun getCoordinates(): Coordinates =
        coordinates.let { (x, y) -> Coordinates(x, y) }

    /**
     * Adds a new neighbor with the edge.
     *
     * @param node The new neighbor.
     * @param edge Edge connecting this node and the other one.
     */
    open fun addNeighbour(node: Node, edge: Edge) {
        neighbors[node] = edge
    }

    /**
     * Retrieves the neighbors of this node.
     *
     * @return HashSet of the nodes
     */
    fun getNeighborsSet(): HashSet<Node> {
        return HashSet(neighbors.keys)
    }

    /**
     * Retrieves the number of neighbors of this node.
     *
     * @return Size of neighbors list.
     */
    fun countNeighbours(): Int = neighbors.size

    /**
     * Abstract function accepting the packets from neighbors.
     */
    abstract fun collectPackets()

    /**
     * Abstract function for updating router buffers after BFS algorithm.
     */
    abstract fun updateBuffer()

    /**
     * Abstract function returning the packet directed to specified node.
     *
     * @param node Node asking for the packets.
     * @return Packet directed to `node` or null if no packets are directed to `node`.
     */
    abstract fun getPacket(node: Node): Packet?

    // Functions like hashCode() and equals() are overridden here
    override fun hashCode(): Int {
        return index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false
        return this.index == other.index
    }

}