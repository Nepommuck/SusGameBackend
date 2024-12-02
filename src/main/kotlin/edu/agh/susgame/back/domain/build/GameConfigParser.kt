package edu.agh.susgame.back.domain.build

import edu.agh.susgame.back.domain.net.Edge
import edu.agh.susgame.back.domain.net.NetGraph
import edu.agh.susgame.back.domain.net.Player
import edu.agh.susgame.back.domain.net.node.Host
import edu.agh.susgame.back.domain.net.node.Node
import edu.agh.susgame.back.domain.net.node.Router
import edu.agh.susgame.back.domain.net.node.Server
import edu.agh.susgame.config.GAME_DEFAULT_PACKETS_DELIVERED_GOAL
import edu.agh.susgame.config.GAME_TIME_DEFAULT
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class NodeJson(val type: String, val coordinates: Coordinates, val bufferSize: Int? = null)

@Serializable
data class Coordinates(
    val x: Int,
    val y: Int
)

@Serializable
data class EdgeJson(val weight: Int, val from: Int, val to: Int)

@Serializable
data class GraphJson(
    val gameTime: Int = GAME_TIME_DEFAULT,
    val gameGoal: Int = GAME_DEFAULT_PACKETS_DELIVERED_GOAL,
    val nodes: List<NodeJson>,
    val edges: List<EdgeJson>
)

data class GameConfig(
    val netGraph: NetGraph,
    val gameLength: Int,
    val gameGoal: Int,
)

class GameConfigParser {
    /**
     * Parses a JSON file containing graph data and creates a NetGraph object.
     *
     * @param filePath The relative path to the JSON file within the `resources` directory.
     *                 For example, if the file is located at `src/main/resources/graph.json`,
     *                 we should pass "/graph.json".
     * @param players  A list of Player objects. Each Host node in the graph will be assigned
     *                 to exactly one player from this list in the order they appear. The number of players
     *                 must be at least equal to the number of Host nodes. (for now)
     * @return A NetGraph object constructed from the parsed JSON data.
     * @throws IllegalArgumentException If the file cannot be found or read, if there are not
     *                                  enough players for the Host nodes, or if the JSON data is invalid.
     */
    fun parseFromFile(filePath: String, players: List<Player>): GameConfig {
        // This does not work on linux
        //val fileUrl = javaClass.getResource(filePath)
        //    ?: throw IllegalArgumentException("File not found at path: $filePath")
        //val jsonString = File(fileUrl.toURI()).readText()
        val jsonString = File(filePath).readText()
        return parseGraph(jsonString, players)
    }

    private fun parseGraph(jsonString: String, players: List<Player>): GameConfig {
        val json = Json { ignoreUnknownKeys = true }
        val gameData = json.decodeFromString<GraphJson>(jsonString)

        val graph = NetGraph()
        val nodeMap = mutableMapOf<Int, Node>() // Map registering nodes under dynamically added indexes.
        // Edges connections are associated with the order of the
        // nodes in the json file.

        var playerIndex = 0 // Index of the player in the input player list used for associating every player
        // with exactly one host.

        // Create Nodes and add to the NetGraph
        // IMPORTANT It is important for UpgradeDTO for each pair of node and edge to different index
        var nodeIndex = 0
        gameData.nodes.forEachIndexed { _, nodeJson ->
            val coordinates = Pair(nodeJson.coordinates.x, nodeJson.coordinates.y)
            val node = when (nodeJson.type) {
                "Host" -> {
                    val player = players.getOrNull(playerIndex)
                        ?: throw IllegalArgumentException("Not enough players for hosts")
                    playerIndex++
                    Host(nodeIndex, coordinates, player)
                }

                "Router" -> Router(nodeIndex, coordinates, nodeJson.bufferSize!!)
                "Server" -> Server(nodeIndex, coordinates)
                else -> throw IllegalArgumentException("Unknown node type")
            }
            nodeMap[nodeIndex] = node
            nodeIndex++
            graph.addNode(node)
        }

        // Create Edges and connect Nodes
        var edgeIndex = nodeIndex
        gameData.edges.forEachIndexed { _, edgeJson ->
            val fromNode = nodeMap[edgeJson.from] ?: throw IllegalArgumentException("Node not found")
            val toNode = nodeMap[edgeJson.to] ?: throw IllegalArgumentException("Node not found")
            val edge = Edge(edgeIndex, edgeJson.weight, connectedNodesIds = Pair(edgeJson.from, edgeJson.to))
            graph.addEdge(fromNode, toNode, edge)
            edgeIndex++
        }

        return GameConfig(graph, gameData.gameTime, gameData.gameGoal)
    }
}
