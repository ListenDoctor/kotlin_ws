package doctor.listen.socketskotlin.viewmodels


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import doctor.listen.socketskotlin.utils.ListenDoctorClient
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _recordingState = MutableLiveData<RecordingState>()
    val recordingState: LiveData<RecordingState> = _recordingState

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val listenDoctorClient = ListenDoctorClient()

    fun authenticate(apiKey: String, clientId: String, clientSecret: String, doctorId: String) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                listenDoctorClient.initialize(apiKey)
                listenDoctorClient.authenticate(clientId, clientSecret, doctorId)
                _connectionState.value = ConnectionState.CONNECTED
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.DISCONNECTED
                _errorMessage.value = "Authentication error: ${e.message}"
            }
        }
    }

    fun connectSocket() {
        viewModelScope.launch {
            try {
                listenDoctorClient.connectSocket()
                _connectionState.value = ConnectionState.SOCKET_CONNECTED
            } catch (e: Exception) {
                _errorMessage.value = "Socket connection error: ${e.message}"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            listenDoctorClient.disconnect()
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun setRecordingState(state: RecordingState) {
        _recordingState.value = state
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        SOCKET_CONNECTED
    }

    enum class RecordingState {
        IDLE,
        RECORDING,
        RECORDED,
        PROCESSING
    }
}