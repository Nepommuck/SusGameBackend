package net_test

import edu.agh.susgame.back.domain.net.NetGraph
import edu.agh.susgame.back.domain.net.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class GraphTest : TestUtils {

    /**
     * Tests if the graph is properly initialized.
     */
    @Test
    fun `relations between structure elements`() {
        val graph = NetGraph()

        // Create graph elements
        val player0 = Player(0, "Player0")

        // nodes
        // host
        val node0 = newTestHost(0, player0)
        //routers
        val node1 = newTestRouter(1, 5)
        val node2 = newTestRouter(2, 5)
        // server
        val node3 = newTestServer(3)


        // edges
        val edge0 = newTestEdge(0, 5)
        val edge1 = newTestEdge(1, 5)
        val edge2 = newTestEdge(2, 5)
        val edge3 = newTestEdge(3, 5)

        // Add elements to the structure
        graph.addNode(node1)
        graph.addNode(node2)
        graph.addNode(node3)
        graph.addNode(node0)

        graph.addEdge(node0, node1, edge0) // host - router1
        graph.addEdge(node0, node2, edge1) // host - router2
        graph.addEdge(node1, node2, edge2) // router2 - router1
        graph.addEdge(node2, node3, edge3) // router2 - server

        // Test nodes
        assertTrue(graph.areNeighbors(node0, node1))
        assertTrue(graph.areNeighbors(node0, node2))
        assertTrue(graph.areNeighbors(node1, node2))
        assertTrue(graph.areNeighbors(node2, node3))

        assertFalse(graph.areNeighbors(node3, node0))

        // Test edges
        assertEquals(graph.getEdge(node1, node0), edge0)
    }
}