package doctor.listen.socketskotlin.utils

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    fun startRecording(): File? {
        if (isRecording) return null

        audioFile = createAudioFile()
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1) // Mono
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(audioFile?.absolutePath)

            try {
                prepare()
                start()
                isRecording = true
            } catch (e: IOException) {
                release()
                throw e
            }
        }

        return audioFile
    }

    fun stopRecording(): File? {
        if (!isRecording) return null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            return audioFile
        } catch (e: IOException) {
            audioFile?.delete()
            throw e
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }

    private fun createAudioFile(): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "audio_record_$timestamp.m4a"
        return File(context.externalCacheDir, fileName)
    }

    fun isRecording() = isRecording

    fun getRecordedFile() = audioFile

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
    }
}