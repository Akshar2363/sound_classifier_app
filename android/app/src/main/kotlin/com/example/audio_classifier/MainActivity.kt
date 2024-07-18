package com.example.audio_classifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.tensorflow.lite.support.label.Category

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.audio_classifier"
    private lateinit var audioClassificationHelper: AudioClassificationHelper

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAudioPermissions()
    }

    private fun requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            initializeAudioClassificationHelper()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) {
            finish()
        } else {
            initializeAudioClassificationHelper()
        }
    }

    private fun initializeAudioClassificationHelper() {
        audioClassificationHelper = AudioClassificationHelper(
            this,
            object : AudioClassificationListener {
                override fun onResult(results: List<Category>, inferenceTime: Long) {
                    val resultData = results.mapIndexed { index, category ->
                        mapOf(
                            "label" to category.label,
                            "index" to index,
                            "score" to category.score
                        )
                    }
                    runOnUiThread {
                        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL).invokeMethod("onResult", resultData)
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL).invokeMethod("onError", error)
                    }
                }
            }
        )
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
    }

    private fun startAudioClassification(result: MethodChannel.Result) {
        try {
            if (::audioClassificationHelper.isInitialized) {
                audioClassificationHelper.startAudioClassification()
                result.success(null)
            } else {
                result.error("ERROR", "AudioClassificationHelper is not initialized", null)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start audio classification", e)
            result.error("ERROR", "Failed to start audio classification", e.message)
        }
    }

    private fun stopAudioClassification(result: MethodChannel.Result) {
        try {
            if (::audioClassificationHelper.isInitialized) {
                audioClassificationHelper.stopAudioClassification()
                result.success(null)
            } else {
                result.error("ERROR", "AudioClassificationHelper is not initialized", null)
            }
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
            if (::audioClassificationHelper.isInitialized) {
                audioClassificationHelper.updateNumThreads(numThreads)
                result.success(null)
            } else {
                result.error("ERROR", "AudioClassificationHelper is not initialized", null)
            }
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
            if (::audioClassificationHelper.isInitialized) {
                audioClassificationHelper.updateNumOfResults(numOfResults)
                result.success(null)
            } else {
                result.error("ERROR", "AudioClassificationHelper is not initialized", null)
            }
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
            if (::audioClassificationHelper.isInitialized) {
                audioClassificationHelper.updateDisplayThreshold(displayThreshold)
                result.success(null)
            } else {
                result.error("ERROR", "AudioClassificationHelper is not initialized", null)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to update display threshold", e)
            result.error("ERROR", "Failed to update display threshold", e.message)
        }
    }
}
