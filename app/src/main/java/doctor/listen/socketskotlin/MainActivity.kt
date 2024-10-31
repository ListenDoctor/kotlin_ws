package doctor.listen.socketskotlin

import android.os.Bundle


import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import doctor.listen.socketskotlin.databinding.ActivityMainBinding
import doctor.listen.socketskotlin.utils.ListenDoctorClient
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val listenDoctorClient = ListenDoctorClient()
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var doctor_uuid: String = UUID.randomUUID().toString().replace("-", "")
    private var room: String = UUID.randomUUID().toString()


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // Permissions granted
        } else {
            Toast.makeText(this, "Permissions required for recording", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.doctorIdInput.setText(doctor_uuid)
        setupButtons()
        checkPermissions()
    }

    private fun setupButtons() {
        binding.getTokenButton.setOnClickListener {
            val apiKey = binding.apiKeyInput.text.toString()
            val clientId = binding.clientIdInput.text.toString()
            val clientSecret = binding.clientSecretInput.text.toString()
            val doctorId = binding.doctorIdInput.text.toString()

            if (apiKey.isBlank() || clientId.isBlank() || clientSecret.isBlank() || doctorId.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    Log.d("socketskotlin", apiKey)
                    Log.d("socketskotlin", clientId)
                    Log.d("socketskotlin", clientSecret)
                    Log.d("socketskotlin", doctorId)
                    listenDoctorClient.initialize(apiKey)
                    listenDoctorClient.authenticate(clientId, clientSecret, doctorId)
                    updateStatus("Token obtained successfully")
                } catch (e: Exception) {
                    updateStatus("Error: ${e.message}")
                }
            }
        }

        binding.connectSocketButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    listenDoctorClient.connectSocket()
                    updateStatus("Socket connected")
                } catch (e: Exception) {
                    updateStatus("Socket error: ${e.message}")
                }
            }
        }

        binding.joinRoomButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    listenDoctorClient.joinRoom(room)
                    updateStatus("Joined room")
                } catch (e: Exception) {
                    updateStatus("Room error: ${e.message}")
                }
            }
        }

        binding.startRecordingButton.setOnClickListener {
            startRecording()
        }

        binding.stopRecordingButton.setOnClickListener {
            stopRecording()
        }

        binding.sendAudioButton.setOnClickListener {
            audioFile?.let { file ->
                lifecycleScope.launch {

                    val fecha = LocalDateTime.now()
                    val locale = Locale.getDefault()
                    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", locale)

                    try {
                        val response = listenDoctorClient.processAudio(
                            file = file,
                            prompt = "default-prompt",
                            language = "ES",
                            speciality = 10,
                            category = "V",
                            datetime = fecha.format(formatter)
                        )
                        updateStatus("Audio processed: ${response.summary}")
                    } catch (e: Exception) {
                        updateStatus("Processing error: ${e.message}")
                    }
                }
            } ?: run {
                Toast.makeText(this, "No audio recorded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        if (isRecording) return

        audioFile = File(externalCacheDir, "audio_record.wav")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)

            try {
                prepare()
                start()
                isRecording = true
                updateStatus("Recording started")
            } catch (e: Exception) {
                updateStatus("Recording error: ${e.message}")
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        mediaRecorder?.apply {
            try {
                stop()
                release()
                updateStatus("Recording stopped")
            } catch (e: Exception) {
                updateStatus("Stop recording error: ${e.message}")
            }
        }
        mediaRecorder = null
        isRecording = false
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            setupAudioRecorder()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun setupAudioRecorder() {
        // Configuración inicial del grabador de audio
        binding.startRecordingButton.isEnabled = true
        binding.stopRecordingButton.isEnabled = false
        binding.sendAudioButton.isEnabled = false
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            binding.statusText.text = message
            println("Status: $message") // Para debugging
        }
    }

    private fun updateButtonStates(isRecording: Boolean) {
        binding.startRecordingButton.isEnabled = !isRecording
        binding.stopRecordingButton.isEnabled = isRecording
        binding.sendAudioButton.isEnabled = !isRecording && audioFile != null
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        listenDoctorClient.disconnect()
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = 1
        private const val ENCODING_BIT_RATE = 128000
    }
}


/*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SocketsKotlinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SocketsKotlinTheme {
        Greeting("Android")
    }
}

 */