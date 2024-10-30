package edu.agh.susgame.back.net

import edu.agh.susgame.back.net.node.Host
import edu.agh.susgame.back.net.node.Node
import edu.agh.susgame.back.net.node.Router
import edu.agh.susgame.back.net.node.Server

/**
 * Represents the structure of the net as an undirected graph
 */
// IMPORTANT It is important for UpgradeDTO for each pair of node and edge to different index
class NetGraph {

    // Structure of the graph
    private val structure: HashMap<Node, HashMap<Node, Edge>> = HashMap()

    // Mutable list of edges
    private val edges: HashSet<Edge> = HashSet()

    // Map of hosts for DTO purposes
    private val hosts = HashMap<Int, Host>()

    // Map of routers for DTO purposes
    private val routers = HashMap<Int, Router>()

    // Map of servers for DTO purposes
    private val servers = HashMap<Int, Server>()


    /**
     * Resets the packet counters for all edges.
     * Sets the `transportedPacketsThisTurn` property of each edge to 0.
     */
    fun resetEdges() {
        edges.forEach { it.transportedPacketsThisTurn = 0 }
    }

    /**
     * Adds money to each host in the network.
     * Iterates through all hosts and calls the `addMoney` method on each host.
     */
    fun addMoney() {
        hosts.forEach { (_, host) ->
            host.addMoney()
        }
    }

    /**
     * Adds a new node to the graph.
     *
     * @param node The node to add to the graph.
     */
    fun addNode(node: Node) {
        structure[node] = HashMap()

        when (node) {
            is Host -> hosts[node.index] = node
            is Router -> routers[node.index] = node
            is Server -> servers[node.index] = node
            else -> {
                throw IllegalArgumentException("Node type not recognized $node")
            }
        }
    }

    fun getHost(index: Int) = hosts[index]
    fun getRouter(index: Int) = routers[index]
    fun getServer(index: Int) = servers[index]

    fun getHostsList() = hosts.values.toList()
    fun getRoutersList() = routers.values.toList()
    fun getServersList() = servers.values.toList()

    /**
     * Connects two nodes in the graph with an edge.
     * Adds the edge to the edge HashSet.
     * Adds new neighbours to the nodes.
     *
     * @param startNode The starting node of the edge.
     * @param endNode The ending node of the edge.
     * @param edge The edge to connect the nodes with.
     */
    fun addEdge(startNode: Node, endNode: Node, edge: Edge) {
        // Connect the nodes
        structure[startNode]!![endNode] = edge
        structure[endNode]!![startNode] = edge

        // Add the edge to the edge HashSet
        edges.add(edge)

        // Add the neighbours
        startNode.addNeighbour(endNode, edge)
        endNode.addNeighbour(startNode, edge)
    }

    /**
     * Retrieves the neighbors of a given node in graph.
     *
     * @param node The node to retrieve neighbours of.
     * @return The HashSet of the neighbours. Null if node does not exist.
     */
    fun getNeighbours(node: Node): HashSet<Node>? {
        return structure[node]?.keys?.let { HashSet(it) }
    }

    /**
     * Retrieves the edge between two nodes
     *
     * @param startNode Staring node of the edge.
     * @param endNode Ending node of the edge.
     * @return The edge between nodes. Null if edge does not exist.
     */
    fun getEdge(startNode: Node, endNode: Node): Edge? {
        return structure[startNode]?.get(endNode)
    }

    /**
     * Retrieves all the nodes from the graph.
     *
     * @return HashSet of all nodes.
     */
    fun getNodes(): HashSet<Node> = HashSet(structure.keys)

    /**
     * Retrieves all the edges from the graph.
     *
     * @return HashSet of all edges.
     */
    fun getEdges(): HashSet<Edge> = edges

    /**
     * Checks if two nodes are neighbors in NetGraph structure.
     *
     * @param node1 First node.
     * @param node2 Second node.
     * @return Boolean value if the second node is in the neighbor list of the first node.
     */
    fun areNeighbors(node1: Node, node2: Node): Boolean {
        val neighbors = getNeighbours(node1)
        return neighbors?.contains(node2) ?: false
    }

    /**
     * Updates the buffers of all routers.
     */
    fun updateBuffers() {
        val nodes = getNodes()

        nodes.forEach { node -> node.updateBuffer() }
    }

}