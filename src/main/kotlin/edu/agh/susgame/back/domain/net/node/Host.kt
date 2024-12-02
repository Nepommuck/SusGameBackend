package edu.agh.susgame.back.domain.net.node

import edu.agh.susgame.back.domain.net.Packet
import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.config.PLAYER_DEFAULT_PACKETS_PER_TICK
import edu.agh.susgame.config.PLAYER_MAX_PACKETS_PER_TICK
import edu.agh.susgame.dto.socket.server.HostDTO

class Host(
    override val index: Int,
    override val coordinates: Pair<Int, Int>,
    private val player: Player,
    private var maxPacketsPerTick: Int = PLAYER_DEFAULT_PACKETS_PER_TICK,
) : Node() {

    private var route: List<Node>? = null
    private var firstNode: Node? = null // Node that the player is sending packets to

    private var numPacketsSent: Int = 0 // How many packets has the host already sent.
    private var packetsSentThisTick: Int = 0 // How many packets has the host sent in this time unit.

    fun getPlayer(): Player = player
    fun getMaxPacketsPerTick(): Int = maxPacketsPerTick
    fun setMaxPacketsPerTick(packetsPerTick: Int) {
        if (packetsPerTick < 0) {
            throw IllegalArgumentException("Packets per tick cannot be negative")
        } else if (packetsPerTick > PLAYER_MAX_PACKETS_PER_TICK) {
            throw IllegalArgumentException("Packets per tick cannot be greater than $PLAYER_MAX_PACKETS_PER_TICK")
        }
        this.maxPacketsPerTick = packetsPerTick
    }

    fun resetPacketsSentThisTick() {
        packetsSentThisTick = 0
    }

    /**
     * Stets a new route for generated packets.
     * Updates the packet route and the first node of the route.
     *
     * @param nodeList List of subsequent nodes starting from the first router after the player and ending with the server object.
     */
    fun setRoute(nodeList: List<Node>) {
        if (nodeList.isNotEmpty()) {
            firstNode = nodeList[0]
            route = nodeList.subList(1, nodeList.size)
        } else {
            firstNode = null
            route = emptyList()
        }
    }


    /**
     * Host does not collect the packets - does nothing.
     */
    override fun collectPackets() {}

    /**
     * Host does not have any buffers - does nothing.
     */
    override fun updateBuffer() {}

    /**
     * Creates a new packet if the node asking for the packet is the first in the route.
     *
     * @param node Node asking for the packet.
     * @return A new packet or null if the node is not first in the route or the route is empty.
     */
    override fun getPacket(node: Node): Packet? {
        return if (firstNode == node && maxPacketsPerTick > packetsSentThisTick) {
            packetsSentThisTick++
            Packet(player, route!!).also { numPacketsSent++ }
        } else {
            null
        }
    }

    /**
     * Retrieves the number of packets the host has already sent.
     *
     * @return The number of packets sent from the start of simulation.
     */
    fun getNumPacketsSent(): Int = numPacketsSent

    fun toDTO(): HostDTO {
        val nonNullRoute = route ?: emptyList()
        return HostDTO(
            id = index,
            packetRoute = buildList {
                firstNode?.let { add(it.index) }
                addAll(nonNullRoute.map { it.index })
            },
            packetsSentPerTick = maxPacketsPerTick
        )
    }

}
