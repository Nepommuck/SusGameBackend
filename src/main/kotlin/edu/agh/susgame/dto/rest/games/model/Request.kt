// WARNING: THIS FILE WAS CLONED AUTOMATICALLY FROM 'SusGameDTO' GITHUB REPOSITORY
// IT SHOULD NOT BE EDITED IN ANY WAY
// IN ORDER TO CHANGE THIS DTO, COMMIT TO 'SusGameDTO' GITHUB REPOSITORY
// IN ORDER TO UPDATE THIS FILE TO NEWEST VERSION, RUN 'scripts/update-DTO.sh'

package edu.agh.susgame.dto.rest.games.model

import edu.agh.susgame.dto.rest.model.LobbyPin
import kotlinx.serialization.Serializable


@Serializable
data class GameCreationRequest(
    val gameName: String,
    val maxNumberOfPlayers: Int,
    val gamePin: LobbyPin?,
)
