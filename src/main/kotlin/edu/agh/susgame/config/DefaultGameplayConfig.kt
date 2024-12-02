package edu.agh.susgame.config

import kotlin.math.ceil

//every time is in milliseconds

// base engine settings
const val BFS_FREQUENCY = 1000L
const val CLIENT_REFRESH_FREQUENCY = 1000L

// gameplay constants
const val MONEY_GAINED_BY_CORRECT_ANSWER = 1000
const val MONEY_GAIN_PER_ITERATION = 100

const val ROUTER_UPGRADE_COST_COEFF = 0.2
const val ROUTER_UPGRADE_BUFFER_SIZE_COEFF = 0.1
const val ROUTER_DEFAULT_UPGRADE_COST = 100
fun nextRouterUpgradeCost(upgradeCost: Int) = upgradeCost + ceil(upgradeCost * ROUTER_UPGRADE_COST_COEFF).toInt()
fun nextRouterBufferSize(bufferSize: Int) = bufferSize + ceil(bufferSize * ROUTER_UPGRADE_BUFFER_SIZE_COEFF).toInt()

const val EDGE_UPGRADE_COST_COEFF = 0.2
const val EDGE_UPGRADE_WEIGHT_COEFF = 0.1
const val EDGE_DEFAULT_UPGRADE_COST = 100
fun nextEdgeUpgradeCost(upgradeCost: Int) = upgradeCost + ceil(EDGE_UPGRADE_COST_COEFF * upgradeCost).toInt()
fun nextEdgeWeight(weight: Int) = weight + ceil(EDGE_UPGRADE_WEIGHT_COEFF * weight).toInt()

const val PLAYER_BASE_MONEY = 0
const val PLAYER_MAX_PACKETS_PER_TICK = 20
const val PLAYER_DEFAULT_PACKETS_PER_TICK = 0

const val GAME_DEFAULT_PACKETS_DELIVERED_GOAL = 1000
const val GAME_TIME_DEFAULT = 600000
const val GAME_QUESTION_SENDING_INTERVAL = 30000L

const val CRITICAL_BUFFER_OVERHEAT_LEVEL = 10