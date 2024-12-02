package net_test.parsing_test

import edu.agh.susgame.back.domain.models.Game
import edu.agh.susgame.back.domain.net.Edge
import edu.agh.susgame.back.domain.net.NetGraph
import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.back.domain.net.node.Host
import edu.agh.susgame.back.domain.net.node.Router
import edu.agh.susgame.back.domain.net.node.Server
import edu.agh.susgame.back.services.rest.RestParser
import edu.agh.susgame.dto.rest.model.Coordinates
import net_test.TestUtils
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import edu.agh.susgame.dto.rest.model.GameMapEdgeDTO as EdgeDTO
import edu.agh.susgame.dto.rest.model.GameMapHostDTO as HostDTO
import edu.agh.susgame.dto.rest.model.GameMapRouterDTO as RouterDTO
import edu.agh.susgame.dto.rest.model.GameMapServerDTO as ServerDTO

class RestParserTest : TestUtils {
    @Test
    fun `parsing net graph to GameMapApiResult`() {
        val graph = NetGraph()
        val game = Game("name", 0, 4)

        // Create graph elements
        val player0 = Player(0, "Player0")

        val coordinates1 = Coordinates(2, 1)
        val coordinates2 = Coordinates(3, 7)
        val coordinates3 = Coordinates(0xACAB, 0xACAB)
        val coordinates4 = Coordinates(966, 1410)

        // nodes
        // host
        val node1 = Host(0, asPair(coordinates1), player0)
        //routers
        val node2 = Router(1, asPair(coordinates2), 5)
        val node3 = Router(2, asPair(coordinates3), 2)
        // server
        val node4 = Server(3, asPair(coordinates4))

        // Add elements to the structure
        graph.addNode(node1)
        graph.addNode(node2)
        graph.addNode(node3)
        graph.addNode(node4)

        // edges
        val edge0 = Edge(0, 32, connectedNodesIds = Pair(0, 1))
        val edge1 = Edge(1, 64, connectedNodesIds = Pair(0, 2))
        val edge2 = Edge(2, 128, connectedNodesIds = Pair(1, 2))
        val edge3 = Edge(3, 256, connectedNodesIds = Pair(2, 3))

        graph.addEdge(node1, node2, edge0) // host - router1
        graph.addEdge(node1, node3, edge1) // host - router2
        graph.addEdge(node2, node3, edge2) // router2 - router1
        graph.addEdge(node3, node4, edge3) // router2 - server

        val result = RestParser.netGraphToGetGameMapDTO(game, graph)

        assertEquals(result.hosts.size, 1)
        assertContains(result.hosts, HostDTO(id = 0, coordinates = coordinates1, playerId = player0.index))

        assertEquals(result.routers.size, 2)
        assertContains(result.routers, RouterDTO(id = 1, coordinates = coordinates2, bufferSize = 5))
        assertContains(result.routers, RouterDTO(id = 2, coordinates = coordinates3, bufferSize = 2))

        assertEquals(result.server, ServerDTO(id = 3, coordinates = coordinates4))

        assertEquals(result.edges.size, 4)
        assertContains(result.edges, EdgeDTO(from = 0, to = 1, weight = 32))
        assertContains(result.edges, EdgeDTO(from = 0, to = 2, weight = 64))
        assertContains(result.edges, EdgeDTO(from = 1, to = 2, weight = 128))
        assertContains(result.edges, EdgeDTO(from = 2, to = 3, weight = 256))
    }

    private fun asPair(coordinates: Coordinates): Pair<Int, Int> = Pair(coordinates.x, coordinates.y)
}
