package net_test

import edu.agh.susgame.back.domain.net.BFS
import edu.agh.susgame.back.domain.net.NetGraph
import edu.agh.susgame.back.domain.net.Packet
import edu.agh.susgame.back.domain.net.Player
import kotlin.test.Test
import kotlin.test.assertEquals

class BFSTest : TestUtils {

    private val player0 = Player(0, "player0")
    private val player1 = Player(1, "player1")
    private val player2 = Player(2, "player2")
    private val player3 = Player(3, "player3")

    @Test
    fun `BFS one direction test scenario`() {
        // Graph initialisation
        val graph = NetGraph()

        // Nodes
        //hosts
        val host0 = newTestHost(0, player0)
        val host1 = newTestHost(1, player1)
        val host2 = newTestHost(2, player2)
        val host3 = newTestHost(3, player3)
        val router4 = newTestRouter(4, 6) // router
        val router5 = newTestRouter(5, 5) // router
        val server6 = newTestServer(6) // server

        // Edges
        val edge0 = newTestEdge(0, 4)
        val edge1 = newTestEdge(1, 2)
        val edge2 = newTestEdge(2, 10)
        val edge3 = newTestEdge(3, 10)
        val edge4 = newTestEdge(4, 5)
        val edge5 = newTestEdge(5, 4)
        val edge6 = newTestEdge(6, 10)


        // Add elements to the structure
        graph.addNode(host0)
        graph.addNode(host1)
        graph.addNode(host2)
        graph.addNode(host3)
        graph.addNode(router4)
        graph.addNode(router5)
        graph.addNode(server6)

        graph.addEdge(host0, router4, edge0) // host0 - router4
        graph.addEdge(host1, router4, edge1) // host1 - router4
        graph.addEdge(host2, router4, edge6) // host2 - router4
        graph.addEdge(router4, router5, edge2) // router4 - router5
        graph.addEdge(host3, router5, edge3) // host3 - router5
        graph.addEdge(router4, server6, edge4) // router3 - server5
        graph.addEdge(router5, server6, edge5) // router4 - server5

        // create the nodeList for packet route from hosts to the server
        val nodeList0 = listOf(router4, router5, server6)
        host0.setRoute(nodeList0)

        val nodeList1 = listOf(router4, server6)
        host1.setRoute(nodeList1)

        val nodeList2 = listOf(router4, router5, server6)
        host2.setRoute(nodeList2)

        val nodeList3 = listOf(router5, server6)
        host3.setRoute(nodeList3)

        // --------------------------------------

        val engine = BFS(graph, server6)

        // ITERATION 1
        engine.run()

        // server
        assertEquals(0, server6.getPacketsReceived())

        // routers
        assertEquals(0, router4.getSpaceLeft())
        assertEquals(0, router5.getSpaceLeft())

        //hosts
        assertEquals(2, host0.getNumPacketsSent())
        assertEquals(2, host1.getNumPacketsSent())
        assertEquals(2, host2.getNumPacketsSent())

        assertEquals(5, host3.getNumPacketsSent())


        // ITERATION 2
        engine.run()

        // server
        assertEquals(6, server6.getPacketsReceived())

        //routers
        assertEquals(2, router4.getSpaceLeft())
        assertEquals(0, router5.getSpaceLeft())

        // hosts
        assertEquals(3, host0.getNumPacketsSent())
        assertEquals(3, host1.getNumPacketsSent())
        assertEquals(2, host2.getNumPacketsSent())

        assertEquals(7, host3.getNumPacketsSent())
    }

    @Test
    fun `BFS two direction edges test scenario`() {
        val graph = NetGraph()
        //This is the same graph as in collectPacketsFromMultiplePlayersTest
        // Nodes
        //hosts
        val host0 = newTestHost(0, player0)
        val host1 = newTestHost(1, player1)
        val host2 = newTestHost(2, player2)
        val router3 = newTestRouter(3, 6) // router
        val router4 = newTestRouter(4, 5) // router
        val server5 = newTestServer(5) // server

        // Edges
        val edge0 = newTestEdge(6, 4)
        val edge1 = newTestEdge(7, 2)
        val edge2 = newTestEdge(8, 5) // change compared to collectPacketsFromMultiplePlayersTest
        val edge3 = newTestEdge(9, 10)
        val edge4 = newTestEdge(10, 5)
        val edge5 = newTestEdge(11, 4)

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

        val nodeList1 = listOf(router3, router4, server5)
        host1.setRoute(nodeList1)

        val nodeList2 = listOf(router4, router3, server5)
        host2.setRoute(nodeList2)


        val engine = BFS(graph, server5)

        fun getPlayerPacketsMap(packetList: List<Packet>): Map<Player, Int> {
            val playerPacketsMap = mutableMapOf<Player, Int>()
            for (packet in packetList) {
                val player = packet.getPlayer()
                if (playerPacketsMap.containsKey(player)) {
                    playerPacketsMap[player] = playerPacketsMap[player]!! + 1
                } else {
                    playerPacketsMap[player] = 1
                }
            }
            return playerPacketsMap
        }

        // ITERATION 1
        engine.run()

        // server
        assertEquals(0, server5.getPacketsReceived())

        // routers
        assertEquals(0, router3.getSpaceLeft())
        var router3Packets = getPlayerPacketsMap(router3.getPackets())
        assertEquals(4, router3Packets[player0])
        assertEquals(2, router3Packets[player1])

        assertEquals(0, router4.getSpaceLeft())
        var router4Packets = getPlayerPacketsMap(router4.getPackets())
        assertEquals(5, router4Packets[player2])

        //hosts
        assertEquals(4, host0.getNumPacketsSent())
        assertEquals(2, host1.getNumPacketsSent())
        assertEquals(5, host2.getNumPacketsSent())


        // ITERATION 2
        engine.run()

        // server
        assertEquals(0, server5.getPacketsReceived())

        // routers
        assertEquals(0, router3.getSpaceLeft())
        router3Packets = getPlayerPacketsMap(router3.getPackets())
        assertEquals(2, router3Packets[player2])

        assertEquals(0, router4.getSpaceLeft())
        router4Packets = getPlayerPacketsMap(router4.getPackets())
        assertEquals(2, router4Packets[player0])
        assertEquals(1, router4Packets[player1])
        //przy dzieleniu przez 2 obiekt o mniejszym indeksie powinien dostac o 1 wiecej jezeli bandwith nieparzysty
        // pakiety z routera powinny miec pierwszenstwo przed pakietami z hostow zeby tak jak jest w tym grafie hosty nie zrobily korku na routerze

        //hosts
        assertEquals(4, host0.getNumPacketsSent())
        assertEquals(2, host1.getNumPacketsSent())
        assertEquals(5, host2.getNumPacketsSent())
    }

}