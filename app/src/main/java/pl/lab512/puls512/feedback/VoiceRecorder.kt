package pl.lab512.puls512.feedback

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var outputFile: File? = null
        private set

    @Suppress("DEPRECATION")
    fun start(): File {
        val directory = File(context.filesDir, "feedback").apply { mkdirs() }
        val file = File(directory, "uwaga_puls512_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        outputFile = file
        return file
    }

    fun stop(): File? {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        return outputFile
    }

    fun release() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
    }
}
