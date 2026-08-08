package com.example.filmflip.processor

import android.graphics.Bitmap
import kotlin.math.roundToInt

data class ProcessingParams(
    val gamma: Float = 1.4f,
    val contrast: Float = 1.2f,
    val brightness: Float = 0f,
    val warmth: Float = 0f,
)

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

class NegativeProcessor {

    fun process(source: Bitmap, params: ProcessingParams): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val gammaInv = 1f / params.gamma.coerceAtLeast(0.1f)
        val contrastFactor = ((259f * (params.contrast * 255f + 255f)) / (255f * (259f - params.contrast * 255f))).coerceIn(-259f, 259f)

        for (i in pixels.indices) {
            var r = (pixels[i] shr 16 and 0xFF).toFloat()
            var g = (pixels[i] shr 8 and 0xFF).toFloat()
            var b = (pixels[i] and 0xFF).toFloat()

            // Invert (negative to positive)
            r = 255f - r
            g = 255f - g
            b = 255f - b

            // Gamma correction
            r = java.lang.Math.pow((r / 255f).toDouble(), gammaInv.toDouble()).toFloat() * 255f
            g = java.lang.Math.pow((g / 255f).toDouble(), gammaInv.toDouble()).toFloat() * 255f
            b = java.lang.Math.pow((b / 255f).toDouble(), gammaInv.toDouble()).toFloat() * 255f

            // Contrast
            r = contrastFactor * (r - 128f) + 128f + params.brightness
            g = contrastFactor * (g - 128f) + 128f + params.brightness
            b = contrastFactor * (b - 128f) + 128f + params.brightness

            // Warmth (positive = warmer, negative = cooler)
            val warmthAdj = params.warmth * 30f
            r += warmthAdj
            b -= warmthAdj

            pixels[i] = -0x1000000 or
                ((r.coerceIn(0f, 255f).toInt() and 0xFF) shl 16) or
                ((g.coerceIn(0f, 255f).toInt() and 0xFF) shl 8) or
                (b.coerceIn(0f, 255f).toInt() and 0xFF)
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        source.recycle()
        return result
    }

    fun rotate(source: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return source
        val (newWidth, newHeight) = if (degrees == 90 || degrees == 270) {
            source.height to source.width
        } else {
            source.width to source.height
        }
        val rotated = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(rotated)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        canvas.translate(newWidth / 2f, newHeight / 2f)
        canvas.rotate(degrees.toFloat())
        canvas.translate(-source.width / 2f, -source.height / 2f)
        canvas.drawBitmap(source, 0f, 0f, paint)
        source.recycle()
        return rotated
    }

    fun crop(source: Bitmap, cropRect: CropRect): Bitmap {
        val x = (cropRect.left * source.width).roundToInt()
        val y = (cropRect.top * source.height).roundToInt()
        val rightX = (cropRect.right * source.width).roundToInt()
        val bottomY = (cropRect.bottom * source.height).roundToInt()
        val w = rightX - x
        val h = bottomY - y
        if (w <= 0 || h <= 0) return source
        val cropped = Bitmap.createBitmap(source, x, y, w, h)
        source.recycle()
        return cropped
    }

    fun autoWhiteBalance(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Find the brightest pixel to use as white reference
        var maxBrightness = -1f
        var maxIdx = 0
        for (i in pixels.indices) {
            val r = pixels[i] shr 16 and 0xFF
            val g = pixels[i] shr 8 and 0xFF
            val b = pixels[i] and 0xFF
            val brightness = 0.299f * r + 0.587f * g + 0.114f * b
            if (brightness > maxBrightness) {
                maxBrightness = brightness
                maxIdx = i
            }
        }

        val refR = pixels[maxIdx] shr 16 and 0xFF
        val refG = pixels[maxIdx] shr 8 and 0xFF
        val refB = pixels[maxIdx] and 0xFF

        if (refR == 0 || refG == 0 || refB == 0) return source

        val scaleR = 255f / refR
        val scaleG = 255f / refG
        val scaleB = 255f / refB

        for (i in pixels.indices) {
            var r = pixels[i] shr 16 and 0xFF
            var g = pixels[i] shr 8 and 0xFF
            var b = pixels[i] and 0xFF

            r = (r * scaleR).coerceIn(0f, 255f).toInt()
            g = (g * scaleG).coerceIn(0f, 255f).toInt()
            b = (b * scaleB).coerceIn(0f, 255f).toInt()

            pixels[i] = -0x1000000 or
                (r shl 16) or
                (g shl 8) or
                b
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        source.recycle()
        return result
    }
}
