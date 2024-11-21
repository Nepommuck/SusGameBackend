package net_test

import edu.agh.susgame.back.domain.net.Edge
import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.back.domain.net.node.Host
import edu.agh.susgame.back.domain.net.node.Router
import edu.agh.susgame.back.domain.net.node.Server

interface TestUtils {
    fun newTestHost(id: Int, player: Player): Host = Host(
        index = id,
        player = player,
        coordinates = MockCoordinates,
    )

    fun newTestRouter(id: Int, bufferSize: Int): Router = Router(
        index = id,
        bufferSize = bufferSize,
        coordinates = MockCoordinates,
    )

    fun newTestServer(id: Int): Server = Server(
        index = id,
        coordinates = MockCoordinates,
    )

    fun newTestEdge(id: Int, weight: Int): Edge = Edge(
        index = id,
        weight = weight,
        connectedNodesIds = MockConnectedEdges,
    )

    companion object {
        private val MockCoordinates = Pair(0, 0)
        private val MockConnectedEdges = Pair(0, 0)
    }
}
