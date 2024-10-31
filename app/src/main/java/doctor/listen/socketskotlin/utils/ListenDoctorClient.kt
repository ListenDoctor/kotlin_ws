package doctor.listen.socketskotlin.utils


import android.util.Log
import doctor.listen.socketskotlin.models.AuthRequest
import doctor.listen.socketskotlin.models.AuthResponse
import doctor.listen.socketskotlin.models.ProcessResponse
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging


class ListenDoctorClient(
    private val baseUrl: String = "https://api-beta.listen.doctor/v1"
) {
    private val httpClient = HttpClient(Android) {

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("ListenDoctorClient", message)
                }
            }
            level = LogLevel.ALL
        }

    }

    private var apiKey: String? = null
    private var authToken: String? = null
    private var socket: Socket? = null
    private var socketListeners: MutableList<(String) -> Unit> = mutableListOf()

    fun initialize(apiKey: String) {
        this.apiKey = apiKey
    }

    suspend fun authenticate(clientId: String, clientSecret: String, doctorId: String) {
        val authRequest = AuthRequest(
            client_id = clientId,
            client_secret = clientSecret,
            grant_type = "client_credentials",
            doctor = doctorId
        )

        val response = httpClient.post("$baseUrl/iam") {
            headers {
                append(
                    "x-api-key",
                    apiKey ?: throw IllegalStateException("API Key not initialized")
                )
                contentType(ContentType.Application.Json)
            }
            setBody(authRequest)
        }
        if (response.status.value == 200) {
            val authResponse = response.body<AuthResponse>()
            authToken = authResponse.token
            Log.d("socketskotlin", authToken.toString());
        }
        if (response.status.value == 401) {
            //Toast.makeText(this, "Unauthorized", Toast.LENGTH_LONG).show()
            Log.d("socketskotlin", "Unauthorized");
        }
    }

    fun connectSocket() {
        val options = IO.Options().apply {
            auth = mapOf(
                "token" to (authToken ?: throw IllegalStateException("Not authenticated")),
                "x-api-key" to (apiKey ?: throw IllegalStateException("API Key not initialized"))
            )
        }

        socket = IO.socket(baseUrl, options).apply {
            on(Socket.EVENT_CONNECT) {
                notifyListeners("Socket connected")
            }

            on(Socket.EVENT_DISCONNECT) {
                notifyListeners("Socket disconnected")
            }

            on("transcription_progress") { args ->
                notifyListeners("Transcription progress: ${args[0]}")
            }

            on("summary_progress") { args ->
                notifyListeners("Summary progress: ${args[0]}")
            }

            connect()
        }
    }

    fun joinRoom(roomName: String) {
        socket?.emit("join", roomName)
    }

    suspend fun processAudio(
        file: File,
        prompt: String,
        language: String,
        speciality: Int,
        category: String,
        datetime: String
    ): ProcessResponse = withContext(Dispatchers.IO) {
        val response = httpClient.submitFormWithBinaryData(
            url = "$baseUrl/process/audio",
            formData = formData {
                append("file", file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "audio/wav")
                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                })
                append("prompt", prompt)
                append("language", language)
                append("speciality", speciality.toString())
                append("category", category)
                append("datetime", datetime)
            }
        ) {
            headers {
                append("x-api-key", apiKey ?: throw IllegalStateException("API Key not initialized"))
                append("Authorization", "Bearer ${authToken ?: throw IllegalStateException("Not authenticated")}")
            }
        }

        response.body()
    }

    fun addSocketListener(listener: (String) -> Unit) {
        socketListeners.add(listener)
    }

    private fun notifyListeners(message: String) {
        socketListeners.forEach { it(message) }
    }

    fun disconnect() {
        socket?.disconnect()
        httpClient.close()
        socketListeners.clear()
    }
}