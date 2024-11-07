package edu.agh.susgame.back

import edu.agh.susgame.back.plugins.configureRouting
import edu.agh.susgame.back.plugins.configureSerialization
import edu.agh.susgame.back.rest.games.HttpErrorResponseBody
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    install(StatusPages) {
        exception<NumberFormatException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                HttpErrorResponseBody("${cause.message}. The input param is not a valid number")
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                HttpErrorResponseBody("${cause.message}. The input param is not valid")
            )
        }
    }
    configureSerialization()
    configureRouting()
}
