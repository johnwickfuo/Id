package com.africodeai.id

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        jackson { }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.localizedMessage ?: "Internal error"))
            )
        }
    }

    routing {
        post("/generate") {
            val params = call.receive<LicenseRequest>()
            val license = generateLicense(params.state, params.firstName, params.lastName)
            call.respond(LicenseResponse(license))
        }

        post("/validate") {
            val params = call.receive<ValidationRequest>()
            val isValid = validateLicenseNumber(params.state, params.licenseNumber)
            call.respond(mapOf("valid" to isValid))
        }
    }
}

data class LicenseRequest(val state: String, val firstName: String, val lastName: String)
data class LicenseResponse(val licenseNumber: String)
data class ValidationRequest(val state: String, val licenseNumber: String)

fun generateLicense(state: String, firstName: String, lastName: String): String {
    val firstInitial = firstName.firstOrNull()?.uppercaseChar() ?: 'X'
    val lastInitial = lastName.firstOrNull()?.uppercaseChar() ?: 'X'
    val randomDigits = (100000..999999).random().toString()
    return "$state-$firstInitial$lastInitial-$randomDigits"
}

// Per-state license-number format patterns. Keyed by the two-letter state code.
private val statePatterns: Map<String, Regex> = mapOf(
    "AL" to """^AL[A-Z]{3}\d{2}\d{6}$""",
    "AK" to """^AK[A-Z]{5}\d{4}$""",
    "AZ" to """^AZ[A-Z]\d{2}\d{6}$""",
    "AR" to """^AR[A-Z]{4}\d{2}\d{6}$""",
    "CA" to """^CA[A-Z]{5}\d{2}\d{6}$""",
    "CO" to """^CO[A-Z]{3}\d{4}\d{6}$""",
    "CT" to """^CT[A-Z]{3}\d{2}\d{6}$""",
    "DE" to """^DE[A-Z]{3}\d{4}\d{6}$""",
    "FL" to """^FL[A-Z]{5}\d{2}\d{6}$""",
    "GA" to """^GA[A-Z]{6}\d{4}\d{6}$""",
    "HI" to """^HI[A-Z]{3}\d{2}\d{6}$""",
    "ID" to """^ID[A-Z]{4}\d{4}\d{6}$""",
    "IL" to """^IL[A-Z]{5}\d{4}\d{6}$""",
    "IN" to """^IN[A-Z]{4}\d{2}\d{6}$""",
    "IA" to """^IA[A-Z]{3}\d{4}\d{6}$""",
    "KS" to """^KS[A-Z]{5}\d{4}\d{6}$""",
    "KY" to """^KY[A-Z]{4}\d{4}\d{6}$""",
    "LA" to """^LA[A-Z]{4}\d{2}\d{6}$""",
    "ME" to """^ME[A-Z]{4}\d{4}\d{6}$""",
    "MD" to """^MD[A-Z]{4}\d{2}\d{6}$""",
    "MA" to """^MA[A-Z]{3}\d{4}\d{6}$""",
    "MI" to """^MI[A-Z]{5}\d{2}\d{6}$""",
    "MN" to """^MN[A-Z]{4}\d{4}\d{6}$""",
    "MS" to """^MS[A-Z]{4}\d{4}\d{6}$""",
    "MO" to """^MO[A-Z]{5}\d{2}\d{6}$""",
    "MT" to """^MT[A-Z]{4}\d{4}\d{6}$""",
    "NE" to """^NE[A-Z]{4}\d{2}\d{6}$""",
    "NV" to """^NV[A-Z]{4}\d{4}\d{6}$""",
    "NH" to """^NH[A-Z]{4}\d{2}\d{6}$""",
    "NJ" to """^NJ[A-Z]{5}\d{4}\d{6}$""",
    "NM" to """^NM[A-Z]{4}\d{4}\d{6}$""",
    "NY" to """^NY[A-Z]{3}\d{2}\d{6}$""",
    "NC" to """^NC[A-Z]{4}\d{2}\d{6}$""",
    "ND" to """^ND[A-Z]{4}\d{4}\d{6}$""",
    "OH" to """^OH[A-Z]{4}\d{2}\d{6}$""",
    "OK" to """^OK[A-Z]{4}\d{4}\d{6}$""",
    "OR" to """^OR[A-Z]{4}\d{2}\d{6}$""",
    "PA" to """^PA[A-Z]{4}\d{4}\d{6}$""",
    "RI" to """^RI[A-Z]{3}\d{4}\d{6}$""",
    "SC" to """^SC[A-Z]{5}\d{2}\d{6}$""",
    "SD" to """^SD[A-Z]{4}\d{4}\d{6}$""",
    "TN" to """^TN[A-Z]{4}\d{2}\d{6}$""",
    "TX" to """^TX[A-Z]{5}\d{2}\d{6}$""",
    "UT" to """^UT[A-Z]{4}\d{4}\d{6}$""",
    "VT" to """^VT[A-Z]{4}\d{4}\d{6}$""",
    "VA" to """^VA[A-Z]{5}\d{4}\d{6}$""",
    "WA" to """^WA[A-Z]{4}\d{4}\d{6}$""",
    "WV" to """^WV[A-Z]{4}\d{2}\d{6}$""",
    "WI" to """^WI[A-Z]{5}\d{4}\d{6}$""",
    "WY" to """^WY[A-Z]{4}\d{4}\d{6}$""",
    "DC" to """^DC[A-Z]{4}\d{2}\d{6}$"""
).mapValues { (_, pattern) -> pattern.toRegex() }

fun validateLicenseNumber(state: String, licenseNumber: String): Boolean {
    val regex = statePatterns[state.uppercase()] ?: return false
    return regex.matches(licenseNumber)
}
