package doctor.listen.socketskotlin.models

import kotlinx.serialization.Serializable


@Serializable
data class AuthRequest(
    val client_id: String,
    val client_secret: String,
    val grant_type: String,
    val doctor: String
)

@Serializable
data class AuthResponse(
    val token: String
)

@Serializable
data class ProcessResponse(
    val summary: String,
    val transcription: String? = null
)

@Serializable
data class Template(
    val guid: String,
    val name: String,
    val template: String,
    val speciality: String,
    val category: String,
    val created: Long
)