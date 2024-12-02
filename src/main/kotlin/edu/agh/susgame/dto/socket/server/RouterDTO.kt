// WARNING: THIS FILE WAS CLONED AUTOMATICALLY FROM 'SusGameDTO' GITHUB REPOSITORY
// IT SHOULD NOT BE EDITED IN ANY WAY
// IN ORDER TO CHANGE THIS DTO, COMMIT TO 'SusGameDTO' GITHUB REPOSITORY
// IN ORDER TO UPDATE THIS FILE TO NEWEST VERSION, RUN 'scripts/update-DTO.sh'

package edu.agh.susgame.dto.socket.server

import kotlinx.serialization.Serializable

@Serializable
data class RouterDTO(
    val id: Int,
    val bufferSize: Int,
    val spaceLeft: Int,
    val upgradeCost: Int,
    val overheatLevel: Int,
    val isWorking: Boolean
)
