package com.eitalab.objectdetection.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

data class DetectionResult(
    val label: String,
    val confidence: Float
)

class DetectionOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 56f
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 2f, 2f, Color.BLACK)
    }

    private var results: List<DetectionResult> = emptyList()

    fun setResults(detections: List<DetectionResult>) {
        this.results = detections
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Pega apenas a primeira detecção (a de maior confiança)
        val bestDetection = results.firstOrNull() ?: return

        // Coordenadas fixas (Canto superior esquerdo)
        val startX = 40f
        val startY = 150f

        // Monta o texto e desenha na tela
        val labelText = "${bestDetection.label}: ${(bestDetection.confidence * 100).toInt()}%"
        canvas.drawText(labelText, startX, startY, textPaint)
    }
}