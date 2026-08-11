package com.example.filmflip.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.filmflip.R
import com.example.filmflip.viewmodel.FilmFlipViewModel
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: FilmFlipViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.loadFromGallery(context, it)
            }
        }
    )

    var invertedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also { ia ->
                ia.setAnalyzer(analysisExecutor) { imageProxy: ImageProxy ->
                    try {
                        val buffer = imageProxy.planes[0].buffer
                        val width = imageProxy.width
                        val height = imageProxy.height
                        val pixels = IntArray(width * height)

                        for (i in pixels.indices) {
                            val r = buffer.get().toInt() and 0xFF
                            val g = buffer.get().toInt() and 0xFF
                            val b = buffer.get().toInt() and 0xFF
                            val a = buffer.get().toInt() and 0xFF
                            pixels[i] = (a shl 24) or ((255 - r) shl 16) or ((255 - g) shl 8) or (255 - b)
                        }

                        val sourceBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        val rotatedBitmap = Bitmap.createBitmap(
                            sourceBitmap, 0, 0, width, height, matrix, true
                        )
                        sourceBitmap.recycle()
                        mainHandler.post { invertedBitmap = rotatedBitmap }
                    } finally {
                        imageProxy.close()
                    }
                }
            }
    }

    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()

            previewViewRef.value?.let { pv ->
                preview.setSurfaceProvider(pv.surfaceProvider)
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
            analysisExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera — Negative Mode") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.goToHome() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_home),
                            contentDescription = "Go home"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gallery),
                            contentDescription = "Choose from gallery"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val outputFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val bitmap = loadBitmapFromFile(outputFile)
                                outputFile.delete()
                                if (bitmap != null) {
                                    viewModel.loadFromCamera(bitmap, 90)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("FilmFlip", "Capture error: ${exception.message}", exception)
                            }
                        }
                    )
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_capture),
                    contentDescription = "Take photo"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_START
                    }.also { pv ->
                        previewViewRef.value = pv
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            invertedBitmap?.let { bitmap ->
                AndroidView(
                    factory = {
                        android.widget.ImageView(context).apply {
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    update = { imageView ->
                        imageView.setImageBitmap(bitmap)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            CaptureOverlay()
        }
    }
}

@Composable
private fun CaptureOverlay() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .wrapContentSize(Alignment.TopCenter),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Negative mode active",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun loadBitmapFromFile(file: File): Bitmap? {
    return try {
        android.graphics.BitmapFactory.decodeStream(file.inputStream())
    } catch (e: Exception) {
        Log.e("FilmFlip", "loadBitmapFromFile failed", e)
        null
    }
}
