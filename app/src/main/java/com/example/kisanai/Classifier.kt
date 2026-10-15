package com.example.kisanai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class Classifier(context: Context) {

    private var diseaseInterpreter: Interpreter? = null
    private var leafInterpreter: Interpreter? = null
    private var loadError: String? = null
    
    private var inputSize = 224

    private val diseaseLabels = listOf(
        "Bacterial Blight",
        "Corn___Common_Rust",
        "Corn___Gray_Leaf_Spot",
        "Corn___Healthy",
        "Corn___Northern_Leaf_Blight",
        "Healthy",
        "Potato___Early_Blight",
        "Potato___Healthy",
        "Potato___Late_Blight",
        "Red Rot",
        "Rice___Brown_Spot",
        "Rice___Healthy",
        "Rice___Leaf_Blast",
        "Rice___Neck_Blast",
        "Wheat___Brown_Rust",
        "Wheat___Healthy",
        "Wheat___Yellow_Rust",
    )

    init {
        try {
            // Load Disease Model
            val diseaseModel = FileUtil.loadMappedFile(context, "DenseNet121_Final.tflite")
            diseaseInterpreter = Interpreter(diseaseModel, Interpreter.Options())

            // Load Leaf/Non-leaf Model
            val leafModel = FileUtil.loadMappedFile(context, "Leaf_NonLeaf.tflite")
            leafInterpreter = Interpreter(leafModel, Interpreter.Options())

            Log.d("Classifier", "Both models loaded successfully")
        } catch (e: Exception) {
            loadError = e.message
            Log.e("Classifier", "Error loading models", e)
        }
    }

    data class Recognition(
        val cropName: String,
        val disease: String,
        val confidence: Float,
        val isLeaf: Boolean = true,
        val isError: Boolean = false,
        val debugInfo: String = "",
        val rawLabel: String = ""
    )

    fun recognizeImage(bitmap: Bitmap): Recognition {
        // 1. Check if it's a leaf
        if ((leafInterpreter == null) || (diseaseInterpreter == null)) {
            val errorMsg = loadError ?: "Models not loaded"
            return Recognition("Error", errorMsg, 0f, isLeaf = false, isError = true)
        }

        val leafResult = checkIsLeaf(bitmap)
        val debugText = "L:${String.format("%.2f", leafResult.second)} R:${leafResult.third}"
        
        if (!leafResult.first) {
            return Recognition(
                cropName = "N/A",
                disease = "Non-leaf detected",
                confidence = leafResult.second,
                isLeaf = false,
                debugInfo = debugText
            )
        }

        // 2. If leaf, run disease recognition
        val diseaseResult = runDiseaseInference(bitmap)
        return diseaseResult.copy(debugInfo = debugText)
    }

    private fun checkIsLeaf(bitmap: Bitmap): Triple<Boolean, Float, String> {
        // Strategy: No normalization (0-255). Many Keras TFLite models include scaling internally.
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val inputDataType = leafInterpreter?.getInputTensor(0)?.dataType()
        var tensorImage = TensorImage(inputDataType)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputShape = leafInterpreter?.getOutputTensor(0)?.shape()
        val outputDataType = leafInterpreter?.getOutputTensor(0)?.dataType()
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType)
        
        leafInterpreter?.run(tensorImage.buffer, outputBuffer.buffer)

        val confidences = outputBuffer.floatArray
        val rawValues = confidences.joinToString(",") { String.format("%.2f", it) }
        
        Log.d("Classifier", "Leaf check raw: $rawValues")

        // We check which index is actually higher. 
        // If it still fails, the rawValues debug info on screen will tell us how to fix it.
        return if (confidences.size >= 2) {
            val idx0 = confidences[0]
            val idx1 = confidences[1]
            
            // Assuming Index 0 is Leaf, Index 1 is Non-leaf
            val isLeaf = idx0 > idx1 
            val confidence = if (isLeaf) idx0 else idx1
            Triple(isLeaf, confidence, rawValues)
        } else {
            val isLeaf = confidences[0] > 0.5f
            Triple(isLeaf, confidences[0], rawValues)
        }
    }

    private fun runDiseaseInference(bitmap: Bitmap): Recognition {
        // Trying No Normalization here too as a first step
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val inputDataType = diseaseInterpreter?.getInputTensor(0)?.dataType()
        var tensorImage = TensorImage(inputDataType)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputShape = diseaseInterpreter?.getOutputTensor(0)?.shape()
        val outputDataType = diseaseInterpreter?.getOutputTensor(0)?.dataType()
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType)
        
        diseaseInterpreter?.run(tensorImage.buffer, outputBuffer.buffer)

        val confidences = outputBuffer.floatArray
        Log.d("Classifier", "Disease inference raw max: ${confidences.maxOrNull()}")
        
        var maxIdx = -1
        var maxConf = -1f
        for (i in confidences.indices) {
            if (confidences[i] > maxConf) {
                maxConf = confidences[i]
                maxIdx = i
            }
        }

        val fullLabel = if (maxIdx != -1 && maxIdx < diseaseLabels.size) diseaseLabels[maxIdx] else "Unknown"
        val (crop, disease) = parseLabel(fullLabel)

        return Recognition(crop, disease, maxConf, true, rawLabel = fullLabel)
    }

    private fun parseLabel(label: String): Pair<String, String> {
        return when {
            label.contains("___") -> {
                val parts = label.split("___")
                parts[0] to parts[1].replace("_", " ")
            }
            label == "Bacterial Blight" -> "Rice" to "Bacterial Blight"
            label == "Red Rot" -> "Sugarcane" to "Red Rot"
            label == "Healthy" -> "Sugarcane" to "Healthy"
            else -> "Unknown" to label
        }
    }

    fun close() {
        diseaseInterpreter?.close()
        leafInterpreter?.close()
    }
}
