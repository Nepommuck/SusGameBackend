package parsing_test

import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.back.domain.net.build.GraphParser
import kotlin.test.Test
import kotlin.test.assertEquals

class NetGraphParsingTest {
    private val parser = GraphParser()

    @Test
    fun `can parse graph from the file`() {
        val player0 = Player(0, "P0")
        val player1 = Player(1, "P1")
        val player2 = Player(2, "P2")

        //val filePath = "graph_files/test/graph1.json"
        val filePath = "src/main/resources/graph_files/test/graph1.json"
        val players = listOf(player0, player1, player2)

        val graph = parser.parseFromFile(filePath, players)

        // Number of elements.
        assertEquals(6, graph.getNodes().size)
        assertEquals(6, graph.getEdges().size)
    }
}