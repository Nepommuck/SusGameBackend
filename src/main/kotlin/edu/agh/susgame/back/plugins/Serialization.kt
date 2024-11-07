package edu.agh.susgame.back.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.cbor.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.*

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureSerialization() {
    install(ContentNegotiation) {
    cbor(Cbor {
        ignoreUnknownKeys = true
    })
}
}
