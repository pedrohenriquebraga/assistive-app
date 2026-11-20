package com.eitalab.objectdetection.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

object ObjectLabelTranslator {
    private val translations = mapOf(
        "bed" to "Cama",
        "cat" to "Gato",
        "chair" to "Cadeira",
        "couch" to "Sofá",
        "dog" to "Cachorro",
        "door" to "Porta",
        "person" to "Pessoa",
        "stair" to "Escada",
        "table" to "Mesa",
        "tv" to "Televisão",
        "vehicle" to "Veículo"
    )

    fun translate(label: String): String {
        val clean = label.lowercase().trim()
        return translations[clean] ?: label.replaceFirstChar { it.uppercase() }
    }
}

data class DetectionResult(
    val label: String,
    val confidence: Float
) {
    val localizedLabel: String
        get() = ObjectLabelTranslator.translate(label)

    val confidencePercent: Int
        get() = (confidence * 100).toInt()
}

class DetectionOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var results: List<DetectionResult> = emptyList()

    fun setResults(detections: List<DetectionResult>) {
        this.results = detections
        invalidate()
    }

    fun clear() {
        this.results = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // O pill de texto sobreposto foi removido para manter a visualização limpa,
        // concentrando os dados no card flutuante inferior.
    }
}
