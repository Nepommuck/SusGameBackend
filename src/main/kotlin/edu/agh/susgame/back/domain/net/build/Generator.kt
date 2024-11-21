package edu.agh.susgame.back.domain.net.build

import edu.agh.susgame.back.domain.net.NetGraph
import edu.agh.susgame.back.domain.net.Player
import java.io.File
import kotlin.random.Random

object Generator {
    fun getGraph(players: List<Player>): NetGraph {
        val directoryPath = "src/main/resources/graph_files/${players.size}"
        val directory = File(directoryPath)
        val files = directory.listFiles { _, name -> name.endsWith(".json") }

        val randomFile = if (files != null && files.isNotEmpty()) {
            files[Random.nextInt(files.size)]
        } else {
            throw IllegalArgumentException("No graph files found in directory: $directoryPath")
        }

        return GraphParser().parseFromFile(randomFile.absolutePath, players)
    }
}