package edu.agh.susgame.back.domain.net.node

import edu.agh.susgame.back.domain.net.Edge
import edu.agh.susgame.back.domain.net.Packet
import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.config.ROUTER_DEFAULT_UPGRADE_COST
import edu.agh.susgame.config.nextRouterBufferSize
import edu.agh.susgame.config.nextRouterUpgradeCost
import edu.agh.susgame.dto.socket.server.RouterDTO

/**
 * Represents the router object. Extends Sending.
 *
 * @param index
 * @param bufferSize Size of the buffer in the router.
 */
class Router(
    override val index: Int,
    override val coordinates: Pair<Int, Int>,
    private var bufferSize: Int,
) : Receiving() {

    // Input buffer containing the packets received in a queue for every neighbor.
    private val inputBuffer: HashMap<Node, ArrayDeque<Packet>> = hashMapOf()

    // Buffer containing the waiting packets in a queue for every neighbour.
    private val buffer: HashMap<Node, ArrayDeque<Packet>> = hashMapOf()

    // cost of the next buffer capacity upgrade
    private var upgradeCost: Int = ROUTER_DEFAULT_UPGRADE_COST

    init {
        spaceLeft = bufferSize
    } // How much space is left in the whole buffer

    // Adds a neighbour both to the neighbours list and the buffer
    override fun addNeighbour(node: Node, edge: Edge) {
        neighbors[node] = edge
        inputBuffer[node] = ArrayDeque<Packet>()
        buffer[node] = ArrayDeque<Packet>()
    }

    /**
     * Adds the packets from the inputBuffer to the queues in the buffer and clears the inputBuffer queues.
     */
    override fun updateBuffer() {
        for ((node, inputQueue) in inputBuffer) {
            buffer[node]?.addAll(inputQueue)
            inputQueue.clear()
        }
    }

    /**
     * Performs the buffer operation on the packet.
     * Updates the packet route.
     *
     * @param packet The packet received from the neighbor.
     */
    override fun pushPacket(packet: Packet) {
        val nextNode = packet.popNext() // Get the next node end delete it from packet route
        inputBuffer[nextNode]!!.add(packet) // Add the packet to the buffer directing it to next node. We assume, that the nextNode is always in the neighbors of current node.
        spaceLeft--
    }

    /**
     * Retrieves the first packet directed to the specified node.
     * Updates the `spaceLeft` variable if the packet is sent.
     *
     * @param node Neighbor to get the packet from.
     * @return The first packet from the queue.
     */
    override fun getPacket(node: Node): Packet? = buffer[node]?.removeFirstOrNull()?.also { spaceLeft++ }


    /**
     * Retrieves how much space is left in the router buffer
     */
    fun getSpaceLeft(): Int = spaceLeft

    /**
     * Retrieves the buffer size of the router
     */
    fun getBufferSize(): Int = bufferSize

    /**
     * Retrieves the cost of the next buffer capacity upgrade
     */
    fun getUpgradeCost(): Int = upgradeCost

    /**
     * Retrieves the all the packets in the buffers. Used for testing purposes.
     */
    fun getPackets(): List<Packet> {
        val packets = mutableListOf<Packet>()
        for ((_, queue) in buffer) {
            packets.addAll(queue)
        }
        return packets
    }

    fun toDTO(): RouterDTO {
        return RouterDTO(
            id = index,
            bufferSize = bufferSize,
            spaceLeft = spaceLeft,
            upgradeCost = upgradeCost
        )
    }

    /**
     * Upgrades the buffer capacity and increases the upgrade cost.
     */
    fun upgradeBuffer(player: Player) {
        if (player.getCurrentMoney() < upgradeCost) {
            throw IllegalStateException("Player does not have enough money to upgrade the buffer")
        }
        player.setCurrentMoney(player.getCurrentMoney() - upgradeCost)
        val nextBufferSize = nextRouterBufferSize(bufferSize)
        spaceLeft += nextBufferSize - bufferSize
        bufferSize = nextBufferSize
        upgradeCost = nextRouterUpgradeCost(upgradeCost)
    }
}
