package com.forseti.idp

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Phase A/B bridge — verbatim OCR with bounding boxes for sandwich PDF placement.
 */
@Singleton
class OcrLayoutEngine @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): LayoutOcrResult {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        val visionText = suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

        val spans = mutableListOf<LayoutTextSpan>()
        val verbatim = StringBuilder()
        visionText.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    val box: Rect = element.boundingBox ?: return@forEach
                    val text = element.text
                    if (text.isBlank()) return@forEach
                    spans += LayoutTextSpan(
                        text = text,
                        left = box.left.toFloat(),
                        top = box.top.toFloat(),
                        right = box.right.toFloat(),
                        bottom = box.bottom.toFloat()
                    )
                    if (verbatim.isNotEmpty()) verbatim.append(' ')
                    verbatim.append(text)
                }
            }
        }

        return LayoutOcrResult(
            verbatimText = verbatim.toString(),
            spans = spans,
            pageWidth = bitmap.width,
            pageHeight = bitmap.height
        )
    }
}
