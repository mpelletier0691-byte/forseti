package com.forseti.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps Google ML Kit on-device text recognition.
 *
 * The "block" granularity in ML Kit (TextBlock -> Line -> Element) maps nicely
 * to draft fields: each block is usually one logical chunk of text the user
 * can drag onto a form blank.
 */
class OcrAnalyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): List<RecognizedBlock> {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        val visionText = suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        return visionText.textBlocks.mapIndexed { index, block ->
            RecognizedBlock(
                id = index,
                text = block.text,
                confidence = block.lines.map { it.confidence ?: 0f }.average().toFloat()
            )
        }
    }
}

data class RecognizedBlock(
    val id: Int,
    val text: String,
    val confidence: Float
)
