package org.jumascola.filmflip

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.filmflip.processor.CropRect
import com.example.filmflip.processor.NegativeProcessor
import com.example.filmflip.processor.ProcessingParams
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessorInstrumentedTest {

    private val processor = NegativeProcessor()

    private fun createBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }

    private fun channel(pixel: Int, shift: Int): Int {
        return (pixel shr shift) and 0xFF
    }

    @Test
    fun process_inverts_black_to_white() {
        val source = createBitmap(10, 10, 0xFF000000.toInt())
        val params = ProcessingParams(gamma = 1.0f, contrast = 0f, brightness = 0f, warmth = 0f)
        val result = processor.process(source, params)
        val pixel = result.getPixel(0, 0)
        assertTrue(channel(pixel, 16) > 200)
        assertTrue(channel(pixel, 8) > 200)
        assertTrue(channel(pixel, 0) > 200)
        result.recycle()
    }

    @Test
    fun process_inverts_white_to_black() {
        val source = createBitmap(10, 10, 0xFFFFFFFF.toInt())
        val params = ProcessingParams(gamma = 1.0f, contrast = 0f, brightness = 0f, warmth = 0f)
        val result = processor.process(source, params)
        val pixel = result.getPixel(0, 0)
        assertTrue(channel(pixel, 16) < 55)
        assertTrue(channel(pixel, 8) < 55)
        assertTrue(channel(pixel, 0) < 55)
        result.recycle()
    }

    @Test
    fun process_preserves_dimensions() {
        val source = createBitmap(100, 200, 0xFF808080.toInt())
        val result = processor.process(source, ProcessingParams())
        assertEquals(100, result.width)
        assertEquals(200, result.height)
        result.recycle()
    }

    @Test
    fun higher_gamma_brightens_dark_pixels_more() {
        val source = createBitmap(10, 10, 0xFF404040.toInt())
        val lowGamma = ProcessingParams(gamma = 1.0f, contrast = 0f, brightness = 0f, warmth = 0f)
        val highGamma = ProcessingParams(gamma = 2.0f, contrast = 0f, brightness = 0f, warmth = 0f)
        val resultLow = processor.process(source.copy(Bitmap.Config.ARGB_8888, true), lowGamma)
        val resultHigh = processor.process(source.copy(Bitmap.Config.ARGB_8888, true), highGamma)
        val lowR = channel(resultLow.getPixel(0, 0), 16)
        val highR = channel(resultHigh.getPixel(0, 0), 16)
        assertTrue(highR > lowR)
        resultLow.recycle()
        resultHigh.recycle()
    }

    @Test
    fun warmth_increases_red_and_decreases_blue() {
        val source = createBitmap(10, 10, 0xFF808080.toInt())
        val neutral = ProcessingParams(gamma = 1.0f, contrast = 0f, brightness = 0f, warmth = 0f)
        val warm = ProcessingParams(gamma = 1.0f, contrast = 0f, brightness = 0f, warmth = 0.3f)
        val resultNeutral = processor.process(source.copy(Bitmap.Config.ARGB_8888, true), neutral)
        val resultWarm = processor.process(source.copy(Bitmap.Config.ARGB_8888, true), warm)
        val neutralR = channel(resultNeutral.getPixel(0, 0), 16)
        val neutralB = channel(resultNeutral.getPixel(0, 0), 0)
        val warmR = channel(resultWarm.getPixel(0, 0), 16)
        val warmB = channel(resultWarm.getPixel(0, 0), 0)
        assertTrue(warmR > neutralR)
        assertTrue(warmB < neutralB)
        resultNeutral.recycle()
        resultWarm.recycle()
    }

    @Test
    fun autoWhiteBalance_scales_channels_to_near_255() {
        val source = createBitmap(10, 10, 0xFF_FF_A0_80.toInt())
        val result = processor.autoWhiteBalance(source)
        val pixel = result.getPixel(0, 0)
        assertTrue(channel(pixel, 16) > 240)
        assertTrue(channel(pixel, 8) > 240)
        assertTrue(channel(pixel, 0) > 240)
        result.recycle()
    }

    @Test
    fun rotate_90_swaps_dimensions() {
        val source = createBitmap(10, 20, 0xFF00FF00.toInt())
        val result = processor.rotate(source, 90)
        assertEquals(20, result.width)
        assertEquals(10, result.height)
        result.recycle()
    }

    @Test
    fun rotate_0_returns_same_bitmap() {
        val source = createBitmap(10, 20, 0xFF00FF00.toInt())
        val result = processor.rotate(source, 0)
        assertSame(source, result)
    }

    @Test
    fun rotate_180_preserves_dimensions() {
        val source = createBitmap(10, 20, 0xFF00FF00.toInt())
        val result = processor.rotate(source, 180)
        assertEquals(10, result.width)
        assertEquals(20, result.height)
        result.recycle()
    }

    @Test
    fun crop_halves_the_image() {
        val source = createBitmap(100, 100, 0xFF00FF00.toInt())
        val cropRect = CropRect(left = 0.25f, top = 0.25f, right = 0.75f, bottom = 0.75f)
        val result = processor.crop(source, cropRect)
        assertEquals(50, result.width)
        assertEquals(50, result.height)
        result.recycle()
    }

    @Test
    fun crop_with_zero_width_returns_source() {
        val source = createBitmap(100, 100, 0xFF00FF00.toInt())
        val cropRect = CropRect(left = 0.5f, top = 0f, right = 0.5f, bottom = 1f)
        val result = processor.crop(source, cropRect)
        assertSame(source, result)
    }

    @Test
    fun cropRect_default_is_full_image() {
        val default = CropRect()
        assertEquals(0f, default.left, 0.001f)
        assertEquals(0f, default.top, 0.001f)
        assertEquals(1f, default.right, 0.001f)
        assertEquals(1f, default.bottom, 0.001f)
    }

    @Test
    fun cropRect_equality_works() {
        val a = CropRect()
        val b = CropRect()
        val c = CropRect(left = 0.1f)
        assertTrue(a == b)
        assertFalse(a == c)
    }

    @Test
    fun full_pipeline_produces_valid_bitmap() {
        val source = createBitmap(50, 50, 0xFF402060.toInt())
        var result = processor.autoWhiteBalance(source)
        result = processor.process(result, ProcessingParams(gamma = 1.4f, contrast = 0.15f, brightness = 0f, warmth = 0.1f))
        result = processor.rotate(result, 90)
        result = processor.crop(result, CropRect(left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f))
        assertFalse(result.isRecycled)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
        result.recycle()
    }

    @Test
    fun crop_rect_0_1_0_1_0_9_0_9_on_1000x1000_gives_800x800() {
        val source = createBitmap(1000, 1000, 0xFF00FF00.toInt())
        val cropRect = CropRect(left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f)
        val result = processor.crop(source, cropRect)
        assertEquals(800, result.width)
        assertEquals(800, result.height)
        result.recycle()
    }

    @Test
    fun crop_rect_0_25_0_25_0_75_0_75_on_1000x1000_gives_500x500() {
        val source = createBitmap(1000, 1000, 0xFF00FF00.toInt())
        val cropRect = CropRect(left = 0.25f, top = 0.25f, right = 0.75f, bottom = 0.75f)
        val result = processor.crop(source, cropRect)
        assertEquals(500, result.width)
        assertEquals(500, result.height)
        result.recycle()
    }

    @Test
    fun crop_rect_on_2000x1500_produces_correct_dimensions() {
        val source = createBitmap(2000, 1500, 0xFF00FF00.toInt())
        val cropRect = CropRect(left = 0.1f, top = 0.2f, right = 0.9f, bottom = 0.8f)
        val result = processor.crop(source, cropRect)
        assertEquals(1600, result.width)  // 0.8 * 2000
        assertEquals(900, result.height) // 0.6 * 1500
        result.recycle()
    }

    @Test
    fun double_crop_applies_correctly() {
        // First crop: 1000x1000 -> 500x500 (center)
        val source = createBitmap(1000, 1000, 0xFF00FF00.toInt())
        val crop1 = CropRect(left = 0.25f, top = 0.25f, right = 0.75f, bottom = 0.75f)
        val afterFirst = processor.crop(source, crop1)
        assertEquals(500, afterFirst.width)
        assertEquals(500, afterFirst.height)

        // Second crop: 500x500 -> 250x250 (center of already-cropped)
        val crop2 = CropRect(left = 0.25f, top = 0.25f, right = 0.75f, bottom = 0.75f)
        val afterSecond = processor.crop(afterFirst, crop2)
        assertEquals(250, afterSecond.width)
        assertEquals(250, afterSecond.height)
        afterSecond.recycle()
    }

    @Test
    fun crop_preserves_pixel_colors() {
        // Create bitmap with a red quadrant at top-left
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        for (x in 0 until 50) {
            for (y in 0 until 50) {
                source.setPixel(x, y, 0xFFFF0000.toInt()) // red
            }
        }
        for (x in 50 until 100) {
            for (y in 0 until 100) {
                source.setPixel(x, y, 0xFF0000FF.toInt()) // blue
            }
        }
        for (x in 0 until 100) {
            for (y in 50 until 100) {
                source.setPixel(x, y, 0xFF0000FF.toInt()) // blue
            }
        }

        // Crop to top-left quadrant
        val cropRect = CropRect(left = 0f, top = 0f, right = 0.5f, bottom = 0.5f)
        val result = processor.crop(source, cropRect)
        assertEquals(50, result.width)
        assertEquals(50, result.height)
        // All pixels should be red
        for (x in 0 until 50) {
            for (y in 0 until 50) {
                val pixel = result.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val b = pixel and 0xFF
                assertEquals(255, r)
                assertEquals(0, b)
            }
        }
        result.recycle()
    }

    @Test
    fun default_crop_rect_returns_full_image() {
        val source = createBitmap(200, 300, 0xFF00FF00.toInt())
        val result = processor.crop(source, CropRect())
        assertEquals(200, result.width)
        assertEquals(300, result.height)
        result.recycle()
    }
}
