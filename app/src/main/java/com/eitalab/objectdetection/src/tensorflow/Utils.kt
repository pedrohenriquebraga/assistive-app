package com.eitalab.objectdetection.src.tensorflow

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Utils {
     fun capturedImageToBitmap(imageProxy: ImageProxy, tf: TensorflowController): Bitmap {
        val bitmap = imageProxy.toBitmap()
        val rotatedBitmap = tf.rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
        imageProxy.close()
        return rotatedBitmap
    }

    fun cropCenter(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val startX = (width - targetWidth) / 2
        val startY = (height - targetHeight) / 2

        val x = if (startX < 0) 0 else startX
        val y = if (startY < 0) 0 else startY

        val cropWidth = if (targetWidth > width) width else targetWidth
        val cropHeight = if (targetHeight > height) height else targetHeight

        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
    }

    fun getModelFileFromAssets(fileName: String, filesDir: File, assets: AssetManager): File {
        val file = File(filesDir, fileName)
        if (file.exists()) {
            return file
        }

        try {
            assets.open(fileName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Error copying model from assets", e)
        }
        return file
    }
}