package pl.edu.pja.s27599.digitaldiary.utils


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(): String? {
        if (!hasRecordAudioPermission()) {
            return null
        }


        outputFile = File(context.externalCacheDir, "audio_${System.currentTimeMillis()}.mp3")

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile?.absolutePath)
            try {
                prepare()
                start()
                return outputFile?.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                stopRecording()
                return null
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                stopRecording()
                return null
            }
        }
    }

    fun stopRecording() {
        recorder?.apply {
            try {
                stop()
                release()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } finally {
                recorder = null
            }
        }
    }

    fun getRecordedFilePath(): String? {
        return outputFile?.absolutePath
    }
}
