package edu.agh.susgame.back.domain.net.node

import edu.agh.susgame.back.domain.net.Edge
import edu.agh.susgame.back.domain.net.Packet
import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.config.CRITICAL_BUFFER_OVERHEAT_LEVEL
import edu.agh.susgame.config.ROUTER_DEFAULT_UPGRADE_COST
import edu.agh.susgame.config.nextRouterBufferSize
import edu.agh.susgame.config.nextRouterUpgradeCost
import edu.agh.susgame.dto.socket.server.RouterDTO
import kotlin.math.max

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

    // Variable defining if router is working. Changes, when the router is overheated.
    private var isWorking: Boolean = true

    // Defines how many iterations are left to reach router overflow level, what leads to overheat.
    private var overheatLevel: Int = 0

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
        controlBufferOverheat()
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
     * Retrieves the first packet directed to the specified node if this router is working.
     * Updates the `spaceLeft` variable if the packet is sent.
     *
     * @param node Neighbor to get the packet from.
     * @return The first packet from the queue.
     */
    override fun getPacket(node: Node): Packet? =
        if (isWorking) buffer[node]?.removeFirstOrNull()?.also { spaceLeft++ } else null

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


    /**
     * Informs whether the buffer is not overheated.
     */
    fun isWorking(): Boolean = isWorking

    fun toDTO(): RouterDTO {
        return RouterDTO(
            id = index,
            bufferSize = bufferSize,
            spaceLeft = spaceLeft,
            upgradeCost = upgradeCost,
            // TODO GAME-117 delete
            overheatLevel = 0,
            isWorking = true,
        )
    }

    /**
     * Upgrades the buffer capacity and increases the upgrade cost.
     */
    fun upgradeBuffer(player: Player) {
        if (!player.deductMoney(upgradeCost)) {
            throw IllegalStateException("Player does not have enough money to upgrade the buffer")
        }

        val nextBufferSize = nextRouterBufferSize(bufferSize)
        spaceLeft += nextBufferSize - bufferSize
        bufferSize = nextBufferSize
        upgradeCost = nextRouterUpgradeCost(upgradeCost)
    }

    /**
     * Performs the action of fixing the buffer.
     *  - All of the mappings in buffer are cleared.
     *  - State changes to working.
     *  - Level of overheat decreases to zero.
     */
    fun fixBuffer() {
        if (!isWorking) {
            clearBuffer()
            isWorking = true
            overheatLevel = 0
        }
    }

    /**
     * Performs the overheating operation:
     *  - if the buffer is full, the overheat level starts incrementing.
     *  - if the buffer is not full, the overheat level decrements to zero.
     *  - if the overheat level reaches the criticalBufferOverflow level, the router stops working.
     */
    private fun controlBufferOverheat() {
        overheatLevel =
            if (spaceLeft == 0) overheatLevel + 1
            else max(overheatLevel - 1, 0)

        if (overheatLevel >= CRITICAL_BUFFER_OVERHEAT_LEVEL) isWorking = false // Router stops working until it is fixed
    }

    fun toDTO(): RouterDTO {
        return RouterDTO(
            id = index,
            bufferSize = bufferSize,
            spaceLeft = spaceLeft,
            upgradeCost = upgradeCost,
            overheatLevel = overheatLevel,
            isWorking = isWorking,
        )
    }

    fun clearBuffer() {
        buffer.forEach { (_, inputQueue) ->
            inputQueue.clear()
        }
        spaceLeft = bufferSize
    }

    fun getOverheatLevel() = overheatLevel

}
