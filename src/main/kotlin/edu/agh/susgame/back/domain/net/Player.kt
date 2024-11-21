package edu.agh.susgame.back.domain.net

import edu.agh.susgame.config.MONEY_GAINED_BY_CORRECT_ANSWER
import edu.agh.susgame.config.MONEY_GAIN_PER_ITERATION
import edu.agh.susgame.config.PLAYER_BASE_MONEY
import edu.agh.susgame.dto.rest.model.PlayerId
import edu.agh.susgame.dto.rest.model.PlayerNickname
import edu.agh.susgame.dto.rest.model.PlayerREST
import edu.agh.susgame.dto.socket.server.PlayerDTO
import kotlin.random.Random

class Player(
    val index: Int,
    val name: String,
    var isReady: Boolean = false,
    private val colorHex: Long = Random.nextLong(0, 0xFFFFFF),
    private var currentMoney: Int = PLAYER_BASE_MONEY,
    var activeQuestionId: Int = -1
) {
    fun toREST(): PlayerREST {
        return PlayerREST(
            nickname = PlayerNickname(name),
            id = PlayerId(index),
            colorHex = colorHex
        )
    }

    fun toDTO(): PlayerDTO {
        return PlayerDTO(id = index, playerName = name, currentMoney = currentMoney)
    }

    fun getCurrentMoney(): Int {
        return currentMoney
    }

    fun setCurrentMoney(money: Int) {
        currentMoney = money
    }

    /**
     * Adds money to the player's current balance.
     * Increments the player's current money by a predefined amount defined in the configuration.
     */
    fun addMoneyForCorrectAnswer() {
        currentMoney += MONEY_GAINED_BY_CORRECT_ANSWER
    }

    fun addMoneyPerIteration() {
        currentMoney += MONEY_GAIN_PER_ITERATION
    }

    /**
     * State of player readiness
     */
    fun setReadinessState(state: Boolean) {
        isReady = state
    }

}