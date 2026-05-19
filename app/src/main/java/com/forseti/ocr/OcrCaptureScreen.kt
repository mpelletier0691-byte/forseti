package com.forseti.ocr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.forseti.ui.theme.ForsetiColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CameraX-based capture screen. After the shutter, the page bitmap is recognized
 * with [OcrAnalyzer] and the resulting blocks are returned via [onBlocks].
 *
 * The host (e.g. DraftFillScreen) decides what to do with them.
 */
@Composable
fun OcrCaptureScreen(
    onBlocks: (List<RecognizedBlock>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val analyzer = remember { OcrAnalyzer() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose { /* unbind handled by lifecycle */ }
    }

    Box(modifier = Modifier.fillMaxSize().background(ForsetiColors.SplashBlack)) {
        if (hasPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        captureAndRecognize(context, imageCapture, analyzer) { blocks ->
                            onBlocks(blocks)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForsetiColors.RuneGold,
                        contentColor = ForsetiColors.SplashBlack
                    )
                ) {
                    Icon(Icons.Outlined.Camera, null)
                    Spacer(Modifier.height(8.dp))
                    Text("Capture & Recognize")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Frame the entire page in even light, then tap Capture.",
                    color = ForsetiColors.AshGrey,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Camera permission is required to use OCR capture.",
                    color = ForsetiColors.AshWhite,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Grant permission") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = ForsetiColors.Stone)) { Text("Cancel") }
            }
        }
    }
}

private val OcrScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun captureAndRecognize(
    context: android.content.Context,
    imageCapture: ImageCapture,
    analyzer: OcrAnalyzer,
    onResult: (List<RecognizedBlock>) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = jpegImageProxyToBitmap(image)
                val rotation = image.imageInfo.rotationDegrees
                image.close()
                OcrScope.launch {
                    val blocks = runCatching { analyzer.recognize(bitmap, rotation) }.getOrDefault(emptyList())
                    withContext(Dispatchers.Main) { onResult(blocks) }
                }
            }
            override fun onError(exception: ImageCaptureException) { onResult(emptyList()) }
        }
    )
}

/**
 * Decodes a JPEG-format ImageProxy into a Bitmap. We bypass CameraX's built-in
 * `toBitmap()` because that one only handles YUV/RGBA, and the default
 * ImageCapture output format is JPEG.
 */
private fun jpegImageProxyToBitmap(image: ImageProxy): android.graphics.Bitmap {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
