package com.forseti.idp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.roundToInt

/**
 * Phase A — lightweight offline CV prep (no OpenCV dependency).
 * Grayscale, contrast stretch, and adaptive-style binarization for cleaner OCR.
 */
object ScanPreprocessor {

    private const val MAX_EDGE = 2048

    fun preprocess(source: Bitmap): Bitmap {
        val scaled = downscaleIfNeeded(source, MAX_EDGE)
        val mutable = scaled.copy(Bitmap.Config.ARGB_8888, true)
        if (scaled !== source && scaled !== mutable) scaled.recycle()
        toGrayscale(mutable)
        stretchContrast(mutable)
        binarize(mutable)
        return mutable
    }

    private fun toGrayscale(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    private fun stretchContrast(bitmap: Bitmap) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var min = 255
        var max = 0
        for (p in pixels) {
            val g = Color.red(p)
            if (g < min) min = g
            if (g > max) max = g
        }
        if (max <= min) return
        val scale = 255f / (max - min)
        for (i in pixels.indices) {
            val g = Color.red(pixels[i])
            val stretched = ((g - min) * scale).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(stretched, stretched, stretched)
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /** Sauvola-like local threshold using block mean (fast mobile approximation). */
    private fun binarize(bitmap: Bitmap) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val block = 31
        val half = block / 2
        val gray = IntArray(w * h) { Color.red(pixels[it]) }
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0
                var count = 0
                val x0 = (x - half).coerceAtLeast(0)
                val x1 = (x + half).coerceAtMost(w - 1)
                val y0 = (y - half).coerceAtLeast(0)
                val y1 = (y + half).coerceAtMost(h - 1)
                for (yy in y0..y1) {
                    for (xx in x0..x1) {
                        sum += gray[yy * w + xx]
                        count++
                    }
                }
                val mean = sum / count.coerceAtLeast(1)
                val threshold = (mean * 0.92f).toInt().coerceIn(40, 220)
                val v = if (gray[y * w + x] < threshold) 0 else 255
                pixels[y * w + x] = Color.rgb(v, v, v)
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun downscaleIfNeeded(source: Bitmap, maxEdge: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxEdge && h <= maxEdge) return source
        val scale = maxEdge.toFloat() / maxOf(w, h)
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }
}
