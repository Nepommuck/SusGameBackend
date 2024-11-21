package edu.agh.susgame.back

import edu.agh.susgame.back.presentation.HttpErrorResponseBody
import edu.agh.susgame.back.services.plugins.configureRouting
import edu.agh.susgame.back.services.plugins.configureSerialization
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

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
