package com.example.net.node

import com.example.net.Packet

class Server (
    index: Int
): Receiving(index) {

    private var packetsReceived = 0 // Variable representing the progress of the game.

    /**
     * Updates the game progress by receiving the packet.
     */
    override fun pushPacket(packet: Packet) {
        packetsReceived++
    }

    /**
     * Override function always returning null: Server does not send packets to any Node.
     */
    override fun getPacket(node: Node): Packet? {
        return null
    }
}