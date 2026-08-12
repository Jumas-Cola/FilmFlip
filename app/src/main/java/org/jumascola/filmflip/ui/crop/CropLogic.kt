package org.jumascola.filmflip.ui.crop

import androidx.compose.ui.geometry.Offset

/**
 * Crop handle layout:
 *  4  1  5
 *  |     |
 *  0     2
 *  |     |
 *  6  3  7
 */
const val HANDLE_TOP_LEFT = 0
const val HANDLE_TOP_EDGE = 1
const val HANDLE_TOP_RIGHT = 2
const val HANDLE_BOTTOM_EDGE = 3
const val HANDLE_LEFT_TOP = 4
const val HANDLE_RIGHT_TOP = 5
const val HANDLE_BOTTOM_LEFT = 6
const val HANDLE_BOTTOM_RIGHT = 7

/**
 * Compute handle positions in canvas pixels.
 */
fun computeHandlePositions(
    left: Float, top: Float, right: Float, bottom: Float,
    canvasWidth: Float, canvasHeight: Float
): Array<Offset> {
    val cx = left * canvasWidth
    val cy = top * canvasHeight
    val cw = (right - left) * canvasWidth
    val ch = (bottom - top) * canvasHeight
    return arrayOf(
        Offset(cx, cy),
        Offset(cx + cw / 2f, cy),
        Offset(cx + cw, cy),
        Offset(cx + cw / 2f, cy + ch),
        Offset(cx, cy),
        Offset(cx + cw, cy),
        Offset(cx, cy + ch),
        Offset(cx + cw, cy + ch)
    )
}

/**
 * Find which handle is at the given point, or null.
 */
fun findHandleAt(
    left: Float, top: Float, right: Float, bottom: Float,
    canvasWidth: Float, canvasHeight: Float,
    hitRadius: Float,
    pointX: Float, pointY: Float
): Int? {
    val positions = computeHandlePositions(left, top, right, bottom, canvasWidth, canvasHeight)
    val r2 = hitRadius * hitRadius
    for (i in positions.indices) {
        val dx = pointX - positions[i].x
        val dy = pointY - positions[i].y
        if (dx * dx + dy * dy <= r2) return i
    }
    return null
}

/**
 * Move a crop handle by (dx, dy) in canvas pixels.
 * Returns updated normalized crop rect.
 */
fun moveCropHandle(
    left: Float, top: Float, right: Float, bottom: Float,
    canvasWidth: Float, canvasHeight: Float,
    handle: Int, dx: Float, dy: Float
): CropDelta {
    val ndx = dx / canvasWidth
    val ndy = dy / canvasHeight
    val min = 0.02f

    return when (handle) {
        HANDLE_TOP_LEFT -> {
            CropDelta(
                (left + ndx).coerceIn(0f, right - min),
                (top + ndy).coerceIn(0f, bottom - min),
                right,
                bottom
            )
        }
        HANDLE_TOP_EDGE -> {
            CropDelta(
                left,
                (top + ndy).coerceIn(0f, bottom - min),
                right,
                bottom
            )
        }
        HANDLE_TOP_RIGHT -> {
            CropDelta(
                left,
                (top + ndy).coerceIn(0f, bottom - min),
                (right + ndx).coerceIn(left + min, 1f),
                bottom
            )
        }
        HANDLE_BOTTOM_EDGE -> {
            CropDelta(
                left,
                top,
                right,
                (bottom + ndy).coerceIn(top + min, 1f)
            )
        }
        HANDLE_LEFT_TOP -> {
            CropDelta(
                (left + ndx).coerceIn(0f, right - min),
                (top + ndy).coerceIn(0f, bottom - min),
                right,
                bottom
            )
        }
        HANDLE_RIGHT_TOP -> {
            CropDelta(
                left,
                (top + ndy).coerceIn(0f, bottom - min),
                (right + ndx).coerceIn(left + min, 1f),
                bottom
            )
        }
        HANDLE_BOTTOM_LEFT -> {
            CropDelta(
                (left + ndx).coerceIn(0f, right - min),
                top,
                right,
                (bottom + ndy).coerceIn(top + min, 1f)
            )
        }
        HANDLE_BOTTOM_RIGHT -> {
            CropDelta(
                left,
                top,
                (right + ndx).coerceIn(left + min, 1f),
                (bottom + ndy).coerceIn(top + min, 1f)
            )
        }
        else -> CropDelta(left, top, right, bottom)
    }
}

data class CropDelta(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)