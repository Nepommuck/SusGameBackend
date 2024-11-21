package edu.agh.susgame.back.domain.net

import edu.agh.susgame.back.domain.net.node.Node
import edu.agh.susgame.back.domain.net.node.Receiving


/**
 * The object performing BFS algorithm on the NetGraph.
 *
 * @param net The graph structure.
 * @param root Server node from the BFS tree starts from.
 */
class BFS(
    private val net: NetGraph,
    private val root: Node
) {

    // HashSet of all nodes
    private val nodes: HashSet<Node> = net.getNodes()

    fun run() {
        // Reset edge packet counters
        net.resetEdges()

        // Reset packets send per tick counter
        net.resetPacketsSentByHostsThisTick()

        // Visited
        val visited: HashMap<Node, Boolean> = nodes.associateWith { false } as HashMap<Node, Boolean>

        //queue
        val queue = ArrayDeque<Node>()
        queue.add(root)

        while (!queue.isEmpty()) {

            val currentNode = queue.removeFirst()
            visited[currentNode] = true

            if (currentNode !is Receiving) continue

            currentNode.collectPackets() // Collect packets from all neighbors

            // Perform rest of BFS algorithm
            val neighbours = net.getNeighbours(currentNode)

            if (neighbours != null) for (neighbour in neighbours) {
                if (!visited[neighbour]!!) queue.add(neighbour)
            }

        }

        net.updateBuffers()


    }


}