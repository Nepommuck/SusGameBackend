package parsing_test

import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.back.domain.build.GameConfigParser
import edu.agh.susgame.config.EDGE_DEFAULT_UPGRADE_COST
import edu.agh.susgame.config.GAME_DEFAULT_PACKETS_DELIVERED_GOAL
import edu.agh.susgame.config.GAME_TIME_DEFAULT
import kotlin.test.Test
import kotlin.test.assertEquals

class NetGraphParsingTest {
    private val parser = GameConfigParser()

    @Test
    fun `can parse graph from the file`() {
        val player0 = Player(0, "P0")
        val player1 = Player(1, "P1")
        val player2 = Player(2, "P2")

        //val filePath = "game_files/test/game1.json"
        val filePath = "src/main/resources/game_files/test/game1.json"
        val players = listOf(player0, player1, player2)

        val (graph, gameTime, gameGoal) = parser.parseFromFile(filePath, players)

        assertEquals(GAME_DEFAULT_PACKETS_DELIVERED_GOAL, gameGoal)
        assertEquals(GAME_TIME_DEFAULT, gameTime)
        // Number of elements.
        assertEquals(6, graph.getNodes().size)
        assertEquals(6, graph.getEdges().size)
    }

    @Test
    fun `test parsing non default game goal and game time`() {
        val player0 = Player(0, "P0")
        val player1 = Player(1, "P1")
        val player2 = Player(2, "P2")

        //val filePath = "game_files/test/game1.json"
        val filePath = "src/main/resources/game_files/test/game2.json"
        val players = listOf(player0, player1, player2)

        val (_, gameTime, gameGoal) = parser.parseFromFile(filePath, players)

        assertEquals(1, gameTime)
        assertEquals(2, gameGoal)

    }
}