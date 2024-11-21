package gameplay

import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.back.domain.net.build.GraphParser
import edu.agh.susgame.config.PLAYER_MAX_PACKETS_PER_TICK
import net_test.TestUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MaxPacketsPerTickTest : TestUtils {
    private val parser = GraphParser()

    @Test
    fun test() {
        val player0 = Player(0, "P0")
        val player1 = Player(1, "P1")
        val filePath = "src/main/resources/graph_files/test/test_upgrade.json"
        val players = listOf(player0, player1)
        val graph = parser.parseFromFile(filePath, players)

        val host = newTestHost(0, player0)
        assertFailsWith<IllegalArgumentException> { host.setMaxPacketsPerTick(-1) }
        assertFailsWith<IllegalArgumentException> { host.setMaxPacketsPerTick(PLAYER_MAX_PACKETS_PER_TICK + 1) }
        host.setMaxPacketsPerTick(1)
        assertEquals(1, host.getMaxPacketsPerTick())
        host.setMaxPacketsPerTick(2)
        assertEquals(2, host.getMaxPacketsPerTick())
        val route = graph.getNodes().toList()
        host.setRoute(route)

        for (i in 0 until host.getMaxPacketsPerTick()) {
            assertNotNull(host.getPacket(route.first()))
        }
        assertNull(host.getPacket(route.first()))
        host.resetPacketsSentThisTick()
        for (i in 0 until host.getMaxPacketsPerTick()) {
            assertNotNull(host.getPacket(route.first()))
        }
        assertNull(host.getPacket(route.first()))


    }
}