package io.github.sebastiancastellanos123.llavero.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * Vista previa de la cámara trasera que avisa cada vez que lee un QR distinto.
 * Los cuadros se analizan en memoria y se descartan: no se guarda ninguna imagen.
 */
@Composable
fun QrCameraPreview(
    onQrFound: (String) -> Unit,
    onCameraError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQrFound by rememberUpdatedState(onQrFound)
    val currentOnError by rememberUpdatedState(onCameraError)
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            try {
                provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    // Si el análisis va lento, se salta cuadros en vez de acumularlos.
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor, QrAnalyzer { text ->
                    mainExecutor.execute { currentOnQrFound(text) }
                })
                provider?.unbindAll()
                provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                currentOnError()
            }
        }, mainExecutor)

        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private class QrAnalyzer(private val onFound: (String) -> Unit) : ImageAnalysis.Analyzer {
    // Mientras el mismo QR siga frente a la cámara, se avisa una sola vez.
    private var lastText: String? = null

    override fun analyze(image: ImageProxy) {
        image.use {
            val plane = it.planes[0] // Y = brillo; basta para leer un QR
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val text = QrDecoder.decodeLuminance(data, plane.rowStride, it.width, it.height)
            data.fill(0) // el cuadro contiene el QR, y el QR contiene el secreto
            if (text != null && text != lastText) {
                lastText = text
                onFound(text)
            }
        }
    }
}
