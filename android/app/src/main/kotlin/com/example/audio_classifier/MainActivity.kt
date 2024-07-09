package com.example.audio_classifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.tensorflow.lite.support.label.Category

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.audio_classifier"
    private lateinit var audioClassificationHelper: AudioClassificationHelper

    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == 200) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun requestAudioPermissions() {
        ActivityCompat.requestPermissions(this, permissions, 200)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAudioPermissions()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "startAudioClassification" -> {
                    startAudioClassification(result)
                }
                "stopAudioClassification" -> {
                    stopAudioClassification(result)
                }
                "updateNumThreads" -> {
                    val numThreads = call.argument<Int>("numThreads")
                    updateNumThreads(numThreads, result)
                }
                "updateNumOfResults" -> {
                    val numOfResults = call.argument<Int>("numOfResults")
                    updateNumOfResults(numOfResults, result)
                }
                "updateDisplayThreshold" -> {
                    val displayThreshold = call.argument<String>("displayThreshold")
                    updateDisplayThreshold(displayThreshold!!.toFloat(), result)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        audioClassificationHelper = AudioClassificationHelper(
            this,
            object : AudioClassificationListener {
                override fun onResult(results: List<Category>, inferenceTime: Long) {
                    val resultString = results.joinToString(", ") { it.label }
                    runOnUiThread {
                        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).invokeMethod("onResult", resultString)
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).invokeMethod("onError", error)
                    }
                }
            }
        )
    }

    private fun startAudioClassification(result: MethodChannel.Result) {
        try {
            audioClassificationHelper.startAudioClassification()
            result.success(null)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start audio classification", e)
            result.error("ERROR", "Failed to start audio classification", e.message)
        }
    }

    private fun stopAudioClassification(result: MethodChannel.Result) {
        try {
            audioClassificationHelper.stopAudioClassification()
            result.success(null)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to stop audio classification", e)
            result.error("ERROR", "Failed to stop audio classification", e.message)
        }
    }

    private fun updateNumThreads(numThreads: Int?, result: MethodChannel.Result) {
        if (numThreads == null) {
            result.error("INVALID_ARGUMENT", "Number of threads is null", null)
            return
        }
        try {
            audioClassificationHelper.updateNumThreads(numThreads)
            result.success(null)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to update number of threads", e)
            result.error("ERROR", "Failed to update number of threads", e.message)
        }
    }

    private fun updateNumOfResults(numOfResults: Int?, result: MethodChannel.Result) {
        if (numOfResults == null) {
            result.error("INVALID_ARGUMENT", "Number of results is null", null)
            return
        }
        try {
            audioClassificationHelper.updateNumOfResults(numOfResults)
            result.success(null)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to update number of results", e)
            result.error("ERROR", "Failed to update number of results", e.message)
        }
    }

    private fun updateDisplayThreshold(displayThreshold: Float?, result: MethodChannel.Result) {
        if (displayThreshold == null) {
            result.error("INVALID_ARGUMENT", "Display threshold is null", null)
            return
        }
        try {
            audioClassificationHelper.updateDisplayThreshold(displayThreshold)
            result.success(null)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to update display threshold", e)
            result.error("ERROR", "Failed to update display threshold", e.message)
        }
    }
}
