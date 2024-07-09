package com.example.audio_classifier

import android.content.Context
import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.core.BaseOptions
import java.nio.BufferOverflowException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

interface AudioClassificationListener {
    fun onError(error: String)
    fun onResult(results: List<Category>, inferenceTime: Long)
}

class AudioClassificationHelper(
    val context: Context,
    val listener: AudioClassificationListener,
    var currentModel: String = YAMNET_MODEL,
    var classificationThreshold: Float = DISPLAY_THRESHOLD,
    var overlap: Float = DEFAULT_OVERLAP_VALUE,
    var numOfResults: Int = DEFAULT_NUM_OF_RESULTS,
    var currentDelegate: Int = DELEGATE_CPU,
    var numThreads: Int = 2
) {
    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var recorder: AudioRecord
    private lateinit var executor: ScheduledThreadPoolExecutor

    private val classifyRunnable = Runnable {
        classifyAudio()
    }

    init {
        try {
            initClassifier()
        } catch (e: Exception) {
            listener.onError("Failed to initialize classifier: ${e.message}")
            Log.e("AudioClassificationHelper", "Failed to initialize classifier", e)
        }
    }

    private fun initClassifier() {
        Log.d("AudioClassificationHelper", "Initializing classifier...")

        // Check if the model file exists in assets
        try {
            context.assets.open(currentModel).close()
            Log.d("AudioClassificationHelper", "Model file $currentModel found in assets")
        } catch (e: Exception) {
            listener.onError("Model file $currentModel not found in assets: ${e.message}")
            Log.e("AudioClassificationHelper", "Model file $currentModel not found in assets", e)
            return
        }

        // Set general detection options, e.g., number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // Use the specified hardware for running the model. Default to CPU.
        when (currentDelegate) {
            DELEGATE_CPU -> Log.d("AudioClassificationHelper", "Using CPU for inference")
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
                Log.d("AudioClassificationHelper", "Using NNAPI for inference")
            }
        }

        // Configures a set of parameters for the classifier and what results will be returned.
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(classificationThreshold)
            .setMaxResults(numOfResults)
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            // Create the classifier and required supporting objects
            Log.d("AudioClassificationHelper", "Loading model from file and options")
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel, options)
            tensorAudio = classifier.createInputTensorAudio()
            recorder = classifier.createAudioRecord()
            startAudioClassification()
            Log.d("AudioClassificationHelper", "Classifier and Recorder initialized successfully")

            // Print the classifier settings
            printClassifierSettings()
        } catch (e: IllegalStateException) {
            listener.onError("Audio Classifier failed to initialize. See error logs for details: ${e.message}")
            Log.e("AudioClassificationHelper", "TFLite failed to load with error: ${e.message}", e)
            return
        } catch (e: Exception) {
            listener.onError("Unexpected error during classifier initialization: ${e.message}")
            Log.e("AudioClassificationHelper", "Unexpected error: ${e.message}", e)
            return
        }
    }

    private fun printClassifierSettings() {
        Log.d("AudioClassificationHelper", "Number of Threads: $numThreads")
        Log.d("AudioClassificationHelper", "Number of Results: $numOfResults")
        Log.d("AudioClassificationHelper", "Classification Threshold: $classificationThreshold")
    }

    fun startAudioClassification() {
        try {
            if (!::recorder.isInitialized) {
                listener.onError("Recorder is not initialized")
                Log.e("AudioClassificationHelper", "Recorder is not initialized")
                return
            }

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                listener.onError("Recorder is not properly initialized")
                Log.e("AudioClassificationHelper", "Recorder is not properly initialized")
                return
            }

            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                return
            }

            recorder.startRecording()
            executor = ScheduledThreadPoolExecutor(1)

            val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) /
                    classifier.requiredTensorAudioFormat.sampleRate) * 1000

            val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()

            executor.scheduleWithFixedDelay(
                classifyRunnable,
                0,
                interval,
                TimeUnit.MILLISECONDS
            )
            Log.d("AudioClassificationHelper", "Audio classification started")
        } catch (e: Exception) {
            listener.onError("Failed to start audio classification: ${e.message}")
            Log.e("AudioClassificationHelper", "Failed to start audio classification", e)
        }
    }

    private fun classifyAudio() {
        try {
            tensorAudio.load(recorder)
            var inferenceTime = SystemClock.uptimeMillis()
            val output = classifier.classify(tensorAudio)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime
            listener.onResult(output[0].categories, inferenceTime)
        } catch (e: BufferOverflowException) {
            listener.onError("Buffer overflow during audio classification: ${e.message}")
            Log.e("AudioClassificationHelper", "Buffer overflow during classification", e)
            resetAudioClassification()
        } catch (e: IllegalStateException) {
            listener.onError("Error during audio classification: ${e.message}")
            Log.e("AudioClassificationHelper", "Error during classification", e)
            resetAudioClassification()
        } catch (e: Exception) {
            listener.onError("Unexpected error during audio classification: ${e.message}")
            Log.e("AudioClassificationHelper", "Unexpected error: ${e.message}", e)
        }
    }

    private fun resetAudioClassification() {
        stopAudioClassification()
        startAudioClassification()
    }

    fun stopAudioClassification() {
        try {
            if (::recorder.isInitialized && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
                Log.d("AudioClassificationHelper", "Recorder stopped successfully")
            }
            if (::executor.isInitialized) {
                executor.shutdownNow()
                Log.d("AudioClassificationHelper", "Audio classification executor stopped")
            }
        } catch (e: Exception) {
            listener.onError("Failed to stop audio classification: ${e.message}")
            Log.e("AudioClassificationHelper", "Failed to stop audio classification", e)
        }
    }

    fun updateNumThreads(numThreads: Int) {
        this.numThreads = numThreads
        initClassifier()
    }

    fun updateNumOfResults(numOfResults: Int) {
        this.numOfResults = numOfResults
        initClassifier()
    }

    fun updateDisplayThreshold(displayThreshold: Float) {
        this.classificationThreshold = displayThreshold
        initClassifier()
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_NNAPI = 1
        const val DISPLAY_THRESHOLD = 0.3f
        const val DEFAULT_NUM_OF_RESULTS = 2
        const val DEFAULT_OVERLAP_VALUE = 0.5f
        const val YAMNET_MODEL = "yamnet.tflite"
        const val SPEECH_COMMAND_MODEL = "speech.tflite"
    }
}
