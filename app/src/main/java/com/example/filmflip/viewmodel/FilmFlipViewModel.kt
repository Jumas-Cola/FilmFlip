package com.example.filmflip.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import kotlin.jvm.JvmName
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmflip.processor.CropRect
import com.example.filmflip.processor.NegativeProcessor
import com.example.filmflip.processor.ProcessingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class AppScreen {
    object Home : AppScreen()
    object Camera : AppScreen()
    object Edit : AppScreen()
    object Backlight : AppScreen()
}

class FilmFlipViewModel : ViewModel() {

    private val processor = NegativeProcessor()
    private var debounceJob: Job? = null
    private val DEBOUNCE_DELAY = 300L

    private val bitmapMutex = Mutex()
    private var generation = 0

    var currentScreen by mutableStateOf<AppScreen>(AppScreen.Home)
        private set

    var originalBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var processedBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var saveResult by mutableStateOf<SaveResult?>(null)
        private set

    enum class SaveResult { Idle, Saving, Success, Error }

    @set:JvmName("setGammaDelegate")
    var gamma by mutableFloatStateOf(1.4f)
        private set

    @set:JvmName("setContrastDelegate")
    var contrast by mutableFloatStateOf(0.15f)
        private set

    @set:JvmName("setBrightnessDelegate")
    var brightness by mutableFloatStateOf(0f)
        private set

    @set:JvmName("setWarmthDelegate")
    var warmth by mutableFloatStateOf(0.1f)
        private set

    var rotation by mutableStateOf(0)
        private set

    var cropRect by mutableStateOf(CropRect())
        private set

    var isCropping by mutableStateOf(false)
        private set

    fun startCropping() {
        isCropping = true
    }

    fun stopCropping() {
        isCropping = false
    }

    fun rotateLeft() {
        rotation = (rotation - 90 + 360) % 360
        reprocess()
    }

    fun rotateRight() {
        rotation = (rotation + 90) % 360
        reprocess()
    }

    fun resetRotation() {
        rotation = 0
        reprocess()
    }

    fun updateCropRect(rect: CropRect) {
        cropRect = rect
    }

    fun applyCrop() {
        isCropping = false
        debounceJob?.cancel()
        val rect = cropRect
        val rot = rotation
        viewModelScope.launch {
            val (source, gen) = bitmapMutex.withLock {
                val s = originalBitmap ?: return@launch
                generation++
                s to generation
            }
            val baked = withContext(Dispatchers.IO) {
                val copy = source.copy(Bitmap.Config.ARGB_8888, true)
                var result = copy
                if (rot != 0) {
                    result = processor.rotate(result, rot)
                }
                if (!rect.equals(CropRect())) {
                    result = processor.crop(result, rect)
                }
                if (result !== copy) {
                    copy.recycle()
                }
                result
            }
            bitmapMutex.withLock {
                if (generation != gen) {
                    baked.recycle()
                    return@launch
                }
                source.recycle()
                processedBitmap?.recycle()
                originalBitmap = baked
                processedBitmap = null
                rotation = 0
                cropRect = CropRect()
            }
            processImage()
        }
    }

    fun resetCrop() {
        cropRect = CropRect()
        isCropping = false
        reprocess()
    }

    fun setGamma(value: Float) {
        gamma = value
        debouncedReprocess()
    }

    fun setContrast(value: Float) {
        contrast = value
        debouncedReprocess()
    }

    fun setBrightness(value: Float) {
        brightness = value
        debouncedReprocess()
    }

    fun setWarmth(value: Float) {
        warmth = value
        debouncedReprocess()
    }

    fun resetParameters() {
        debounceJob?.cancel()
        gamma = 1.4f
        contrast = 0.15f
        brightness = 0f
        warmth = 0.1f
        rotation = 0
        cropRect = CropRect()
        isCropping = false
        reprocess()
    }

    fun loadFromCamera(bitmap: Bitmap) {
        viewModelScope.launch {
            isProcessing = true
            val decoded = withContext(Dispatchers.IO) {
                decodeSampledBitmap(bitmap)
            }
            bitmapMutex.withLock {
                originalBitmap?.recycle()
                processedBitmap?.recycle()
                originalBitmap = decoded
                processedBitmap = null
                rotation = 0
                cropRect = CropRect()
                isCropping = false
                saveResult = null
            }
            processImage()
            currentScreen = AppScreen.Edit
        }
    }

    fun loadFromGallery(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            isProcessing = true
            val decoded = withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val orientation = getImageOrientation(context, uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        if (bitmap != null && orientation != 0) {
                            val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        } else {
                            bitmap?.copy(Bitmap.Config.ARGB_8888, true)
                        }
                    } else null
                } catch (e: Exception) {
                    Log.e("FilmFlip", "Gallery load failed", e)
                    null
                }
            }
            if (decoded != null) {
                bitmapMutex.withLock {
                    originalBitmap?.recycle()
                    processedBitmap?.recycle()
                    originalBitmap = decoded
                    processedBitmap = null
                    rotation = 0
                    cropRect = CropRect()
                    isCropping = false
                    saveResult = null
                }
                processImage()
                currentScreen = AppScreen.Edit
            } else {
                isProcessing = false
            }
        }
    }

    fun goToHome() {
        viewModelScope.launch {
            debounceJob?.cancel()
            bitmapMutex.withLock {
                originalBitmap?.recycle()
                processedBitmap?.recycle()
                originalBitmap = null
                processedBitmap = null
                saveResult = null
                generation++
            }
            resetParameters()
            currentScreen = AppScreen.Home
        }
    }

    fun goToCamera() {
        currentScreen = AppScreen.Camera
    }

    fun goToBacklight() {
        currentScreen = AppScreen.Backlight
    }

    fun saveToGallery(context: Context) {
        viewModelScope.launch {
            saveResult = SaveResult.Saving
            try {
                withContext(Dispatchers.IO) {
                    val bitmap = processedBitmap ?: return@withContext
                    val uri = MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        "FilmFlip_${System.currentTimeMillis()}",
                        "Processed from negative"
                    )
                    Log.d("FilmFlip", "Saved to: $uri")
                }
                saveResult = SaveResult.Success
            } catch (e: Exception) {
                Log.e("FilmFlip", "Save failed", e)
                saveResult = SaveResult.Error
            }
        }
    }

    private fun processImage() {
        debounceJob?.cancel()
        viewModelScope.launch {
            val capture = bitmapMutex.withLock {
                val s = originalBitmap ?: return@withLock null
                generation++
                Capture(s, gamma, contrast, brightness, warmth, rotation, cropRect, generation)
            } ?: return@launch
            isProcessing = true
            val copy = capture.source.copy(Bitmap.Config.ARGB_8888, true)
            val result = withContext(Dispatchers.IO) {
                var processed = processor.autoWhiteBalance(copy)
                processed = processor.process(processed, ProcessingParams(capture.g, capture.c, capture.b, capture.w))
                if (capture.r != 0) {
                    processed = processor.rotate(processed, capture.r)
                }
                if (!capture.cr.equals(CropRect())) {
                    processed = processor.crop(processed, capture.cr)
                }
                processed
            }
            bitmapMutex.withLock {
                if (generation != capture.gen) {
                    result.recycle()
                    copy.recycle()
                    isProcessing = false
                    return@launch
                }
                if (result !== copy) {
                    copy.recycle()
                }
                processedBitmap?.recycle()
                processedBitmap = result
            }
            isProcessing = false
        }
    }

    private data class Capture(
        val source: Bitmap, val g: Float, val c: Float,
        val b: Float, val w: Float, val r: Int,
        val cr: CropRect, val gen: Int
    )

    private fun reprocess() {
        if (originalBitmap != null) {
            processImage()
        }
    }

    private fun debouncedReprocess() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            reprocess()
        }
    }

    private fun decodeSampledBitmap(bitmap: Bitmap): Bitmap {
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun getImageOrientation(context: Context, uri: android.net.Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = android.media.ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            Log.e("FilmFlip", "EXIF read failed", e)
            0
        }
    }
}
