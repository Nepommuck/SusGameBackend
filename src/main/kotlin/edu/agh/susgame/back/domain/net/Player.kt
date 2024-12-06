package edu.agh.susgame.back.domain.net

import edu.agh.susgame.config.MONEY_GAINED_BY_CORRECT_ANSWER
import edu.agh.susgame.config.MONEY_GAIN_PER_ITERATION
import edu.agh.susgame.config.PLAYER_BASE_MONEY
import edu.agh.susgame.dto.common.ColorDTO
import edu.agh.susgame.dto.rest.model.PlayerId
import edu.agh.susgame.dto.rest.model.PlayerNickname
import edu.agh.susgame.dto.rest.model.PlayerREST
import edu.agh.susgame.dto.socket.server.PlayerDTO

class Player(
    val index: Int,
    val name: String,
    var isReady: Boolean = false,
    private var currentMoney: Int = PLAYER_BASE_MONEY,
    var activeQuestionId: Int = -1
) {

    private var color: ULong = 123134432324uL

    fun toREST(): PlayerREST {
        return PlayerREST(
            nickname = PlayerNickname(name),
            id = PlayerId(index),
            color = ColorDTO(color.toString()),
            readiness = isReady,
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
     * Checks if player has enough money.
     */
    private fun canAfford(cost: Int): Boolean = cost <= currentMoney

    /**
     * Checks if player can afford the upgrade and deducts money if it is possible.
     * Retrieves true if the process is success., otherwise - false.
     */
    fun deductMoney(cost: Int): Boolean {
        if (canAfford(cost)) {
            currentMoney -= cost
            return true
        }
        return false
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

    fun setColor(color: ULong) {
        this.color = color
    }

}