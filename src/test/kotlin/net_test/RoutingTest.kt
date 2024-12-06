package net_test

import edu.agh.susgame.back.domain.net.NetGraph
import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.back.domain.net.node.Router
import edu.agh.susgame.config.CRITICAL_BUFFER_OVERHEAT_LEVEL
import junit.framework.TestCase.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RoutingTest : TestUtils {

    /**
     * Tests if the packet is properly created.
     */

    private val player0 = Player(0, "player0")
    private val player1 = Player(1, "player1")
    private val player2 = Player(2, "player2")

    @Test
    fun `are the packets created`() {

        // Nodes
        val host0 = newTestHost(0, player0) //host
        val router1 = newTestRouter(1, 5) // router
        val server2 = newTestServer(2) // server

        // create the nodeList for packet route ant set the route
        val nodeList = listOf(router1, server2)
        host0.setRoute(nodeList)

        // Try to get a packets from host
        val packet0 = host0.getPacket(router1)
        val packet1 = host0.getPacket(server2)

        // Host routes the packet to `router0`, therefore the remaining route should contain only `server2` object.
        assertEquals(packet0?.next(), server2)
        assertEquals(packet1?.next(), null)

        // Update packet route
        packet0?.popNext()
        assertEquals(packet0?.next(), server2)
    }

    @Test
    fun `are the packets collected from the one player`() {
        // Graph initialisation
        val graph = NetGraph()

        // Nodes
        val host0 = newTestHost(0, player0) //host
        val router1 = newTestRouter(1, 5) // router
        val router2 = newTestRouter(2, 5) // router
        val server3 = newTestServer(3) // server

        // Edges
        val edge0 = newTestEdge(0, 10)
        val edge1 = newTestEdge(1, 5)
        val edge2 = newTestEdge(2, 2)
        val edge3 = newTestEdge(3, 2)


        // Add elements to the structure
        graph.addNode(host0)
        graph.addNode(router1)
        graph.addNode(router2)
        graph.addNode(server3)

        graph.addEdge(host0, router1, edge0) // host - router1
        graph.addEdge(host0, router2, edge1) // host - router2
        graph.addEdge(router1, router2, edge2) // router2 - router1
        graph.addEdge(router2, server3, edge3) // router2 - server

        // create the nodeList for packet route from host to the server
        val nodeList = listOf(router1, router2, server3)
        host0.setRoute(nodeList)

        //---------------------------------


        // Check if the node collects the packets properly

        // STEP 1
        router2.collectPackets()
        // Space left is supposed to be equal to 5 because there are no packets directed to `router2`.
        assertEquals(5, router2.getSpaceLeft())

        router1.collectPackets()
        // Space left is supposed to be equal to 0 because weight of `edge0` is 10 and bufferSize of `router1` is 5.
        // The host sent 5 packets to router1.
        assertEquals(0, router1.getSpaceLeft())
        assertEquals(5, host0.getNumPacketsSent())

        router1.updateBuffer()

        // STEP 2
        router2.collectPackets()
        // spaceLeft in `router2` is supposed to be equal to 3 because weight of `edge0` is 2 and bufferSize of `router1` is 5.
        // spaceLeft in `router1` is supposed to be equal to 3 because `router2` takes 2 packets.
        assertEquals(3, router2.getSpaceLeft())
        assertEquals(2, router1.getSpaceLeft())

        router2.updateBuffer()

        // STEP 3
        server3.collectPackets()
        // Server is supposed to accept 2 packets because weight of `edge3` is equal to 2.
        // `router2` is supposed to have the space left equal to 5 - all its packets are sent to the server.
        assertEquals(5, router2.getSpaceLeft())
        assertEquals(2, server3.getPacketsReceived())

    }

    @Test
    fun `are the packets collected from multiple players`() {
        // Graph initialisation
        val graph = NetGraph()

        // Nodes
        //hosts
        val host0 = newTestHost(0, player0)
        val host1 = newTestHost(1, player1)
        val host2 = newTestHost(2, player2)
        val router3 = newTestRouter(1, 6) // router
        val router4 = newTestRouter(2, 5) // router
        val server5 = newTestServer(3) // server

        // Edges
        val edge0 = newTestEdge(0, 4)
        val edge1 = newTestEdge(1, 2)
        val edge2 = newTestEdge(2, 10)
        val edge3 = newTestEdge(3, 10)
        val edge4 = newTestEdge(4, 5)
        val edge5 = newTestEdge(5, 4)


        // Add elements to the structure
        graph.addNode(host0)
        graph.addNode(host1)
        graph.addNode(host2)
        graph.addNode(router3)
        graph.addNode(router4)
        graph.addNode(server5)

        graph.addEdge(host0, router3, edge0) // host0 - router3
        graph.addEdge(host1, router3, edge1) // host - router3
        graph.addEdge(router3, router4, edge2) // router3 - router4
        graph.addEdge(host2, router4, edge3) // router2 - router4
        graph.addEdge(router3, server5, edge4) // router3 - server5
        graph.addEdge(router4, server5, edge5) // router4 - server5

        // create the nodeList for packet route from hosts to the server
        val nodeList0 = listOf(router3, router4, server5)
        host0.setRoute(nodeList0)

        val nodeList1 = listOf(router3, server5)
        host1.setRoute(nodeList1)

        val nodeList2 = listOf(router4, server5)
        host2.setRoute(nodeList2)

        //---------------------------------

        // STEP 1
        router3.collectPackets()
        // Router should take 4 packets from `host0` and 2 packets from `host1`
        assertEquals(0, router3.getSpaceLeft())
        assertEquals(4, host0.getNumPacketsSent())
        assertEquals(2, host1.getNumPacketsSent())

        router3.updateBuffer()

        // STEP 2
        router4.collectPackets()
        // As `router3` is added to `router4` neighborList before `host2`, Round Robin starts from `router3`.
        // `router4` should take 3 packets from `router3` and 2 packets from `host2`.
        assertEquals(0, router4.getSpaceLeft())
        assertEquals(3, router3.getSpaceLeft())
        assertEquals(2, host2.getNumPacketsSent())

        router4.updateBuffer()

        // STEP 3
        server5.collectPackets()
        // Server should take all 2 `host1` packets from `router3` and 4 packets from `router4` (2x `host0` and 2x `host2`).
        // `router4`  and `router3` should both have one `host0` packet left.
        assertEquals(4, router4.getSpaceLeft())
        assertEquals(5, router3.getSpaceLeft())
        assertEquals(6, server5.getPacketsReceived())
    }

    fun mockRouterForOverflowTests(): Router {
        val graph = NetGraph()

        val host0 = newTestHost(0, player0)
        val router1 = newTestRouter(1, 1)
        val server2 = newTestServer(2)

        val edge0 = newTestEdge(0, 1000)
        val edge1 = newTestEdge(1, 1000)


        graph.addNode(host0)
        graph.addNode(router1)
        graph.addNode(server2)

        graph.addEdge(host0, router1, edge0)
        graph.addEdge(router1, server2, edge1)

        val nodeList0 = listOf(router1, server2)
        host0.setRoute(nodeList0)

        return router1
    }

    @Test
    fun `is the router turned off after an overheat`() {
        //Given
        val router = mockRouterForOverflowTests()

        // When
        // CRITICAL_BUFFER_OVERHEAT_LEVEL iterations leading to buffer overflow
        repeat(CRITICAL_BUFFER_OVERHEAT_LEVEL) {
            router.collectPackets()
            router.updateBuffer()
        }

        // Expect
        assertFalse(router.isWorking())
    }

    @Test
    fun `does the overflow level decrease when the buffer is not full`() {
        // Given
        val router = mockRouterForOverflowTests()

        // When
        // CRITICAL_BUFFER_OVERHEAT_LEVEL iterations leading to buffer overflow
        repeat(CRITICAL_BUFFER_OVERHEAT_LEVEL / 2) {
            router.collectPackets()
            router.updateBuffer()
        }

        router.clearBuffer()

        // Buffer is updated but the packets are not collected. The overheat level should be equal to 0.
        repeat(CRITICAL_BUFFER_OVERHEAT_LEVEL) {
            router.updateBuffer()
        }

        // Expect
        assertTrue(router.isWorking())
        assertEquals(0, router.getOverheatLevel())

    }

    @Test
    fun `is the router fixed after an overheat`() {
        //Given
        val router = mockRouterForOverflowTests()

        // When
        // CRITICAL_BUFFER_OVERHEAT_LEVEL iterations leading to buffer overflow
        repeat(CRITICAL_BUFFER_OVERHEAT_LEVEL) {
            router.collectPackets()
            router.updateBuffer()
        }

        router.fixBuffer()

        // Expect
        assertTrue(router.isWorking())
        assertEquals(router.getOverheatLevel(), 0)
        assertEquals(router.spaceLeft, router.getBufferSize())
    }

    @Test
    fun `server can collect packets from router after it is fixed`() {
        //Given
        val router = mockRouterForOverflowTests()

        // When
        // CRITICAL_BUFFER_OVERHEAT_LEVEL iterations leading to buffer overflow
        repeat(CRITICAL_BUFFER_OVERHEAT_LEVEL) {
            router.collectPackets()
            router.updateBuffer()
        }

        // Expect
        assertFalse(router.isWorking())
    }
}


