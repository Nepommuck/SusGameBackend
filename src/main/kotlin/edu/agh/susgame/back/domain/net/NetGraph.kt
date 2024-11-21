package edu.agh.susgame.back.domain.net

import edu.agh.susgame.back.domain.net.node.Host
import edu.agh.susgame.back.domain.net.node.Node
import edu.agh.susgame.back.domain.net.node.Router
import edu.agh.susgame.back.domain.net.node.Server

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
    private var server: Server? = null


    /**
     * Resets the packet counters for all edges.
     * Sets the `transportedPacketsThisTurn` property of each edge to 0.
     */
    fun resetEdges() {
        edges.forEach { it.transportedPacketsThisTick = 0 }
    }

    fun resetPacketsSentByHostsThisTick() {
        val hosts = getHostsList()
        hosts.forEach { host -> host.resetPacketsSentThisTick() }
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
            is Server -> {
                if (server != null) {
                    throw IllegalArgumentException("Server already exists in the graph")
                }
                server = node
            }

            else -> {
                throw IllegalArgumentException("Node type not recognized $node")
            }
        }
    }

    fun getHost(index: Int): Host? = hosts[index]
    fun getRouter(index: Int): Router? = routers[index]
    fun getServer(): Server = server!!

    fun getHostsList(): List<Host> = hosts.values.toList()
    fun getRoutersList(): List<Router> = routers.values.toList()


    fun getTotalPacketsDelivered() = server!!.getPacketsReceived()

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
    fun getNeighbours(node: Node): HashSet<Node>? =
        structure[node]?.keys?.let { HashSet(it) }

    /**
     * Retrieves the edge between two nodes
     *
     * @param startNode Staring node of the edge.
     * @param endNode Ending node of the edge.
     * @return The edge between nodes. Null if edge does not exist.
     */
    fun getEdge(startNode: Node, endNode: Node): Edge? =
        structure[startNode]?.get(endNode)

    /**
     * Retrieves the edge with a given id (also called index)
     *
     * @param edgeId ID of an edge
     * @return The edge with given id. Null if edge does not exist.
     */
    fun getEdgeById(edgeId: Int): Edge? =
        getEdges().firstOrNull { it.index == edgeId }

    /**
     * Retrieves all the edges from the graph.
     *
     * @return HashSet of all edges.
     */
    fun getEdges(): HashSet<Edge> = edges

    /**
     * Retrieves all the nodes from the graph.
     *
     * @return HashSet of all nodes.
     */
    fun getNodes(): HashSet<Node> = HashSet(structure.keys)

    /**
     * Retrieves the node with given id (also called index)
     *
     * @param nodeId ID of an edge
     * @return The node with given id. Null if node does not exist.
     */
    fun getNodeById(nodeId: Int): Node? =
        getNodes().firstOrNull { it.index == nodeId }

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