package com.eitalab.objectdetection.src.tensorflow

import android.graphics.Bitmap
import android.util.Log
import com.eitalab.objectdetection.ui.DetectionResult
import org.tensorflow.lite.InterpreterApi as TfInterpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.DataType
import java.io.File

class TensorflowController(private var modelFile: File) {

    private lateinit var interpreter: TfInterpreter
    private var started = false
    private var inputSize = 300
    private var numClasses = 11

    private val classes = listOf(
        "bed",
        "cat",
        "chair",
        "couch",
        "dog",
        "door",
        "person",
        "stair",
        "table",
        "tv",
        "vehicle"
    )

    private val minConf = 0.1f

    init {
        initTensorflow()
    }

    private fun initTensorflow() {
        val options = TfInterpreter.Options().apply {
            setRuntime(TfInterpreter.Options.TfLiteRuntime.PREFER_SYSTEM_OVER_APPLICATION)
            setNumThreads(4)
        }
        try {
            interpreter = TfInterpreter.create(modelFile, options)
            started = true

            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            if (inputShape.size >= 2) {
                inputSize = inputShape[1]
            }

            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            if (outputShape.size >= 2) {
                numClasses = outputShape[1]
            }

            Log.i("TensorflowController", "Modelo carregado com sucesso. Input shape: ${inputShape.contentToString()}, Output shape: ${outputShape.contentToString()}")
        } catch (e: Exception) {
            Log.e("TensorflowController", "Erro ao iniciar Tensorflow: ${e.message}")
        }
    }

    fun detect(imgInput: Bitmap): MutableList<DetectionResult>? {
        if (!started) return null

        val tensorImage = processImage(imgInput)
        val outputProbability = Array(1) { FloatArray(numClasses) }

        try {
            interpreter.run(tensorImage.buffer, outputProbability)
        } catch (e: Exception) {
            Log.e("TensorflowController", "Erro na inferência: ${e.message}")
            return null
        }

        val probabilities = outputProbability[0]
        val detections = mutableListOf<DetectionResult>()

        for (i in probabilities.indices) {
            val score = probabilities[i]
            if (score >= minConf) {
                val label = classes.getOrNull(i) ?: "Classe $i"
                detections.add(DetectionResult(label, score))
            }
        }

        detections.sortByDescending { data -> data.confidence }

        return if (detections.isNotEmpty()) {
            mutableListOf(detections.first())
        } else {
            mutableListOf()
        }
    }

    private fun processImage(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        return imageProcessor.process(tensorImage)
    }

    fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
