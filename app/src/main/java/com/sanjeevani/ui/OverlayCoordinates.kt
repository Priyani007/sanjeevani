package com.sanjeevani.ui

/** Input landmarks are already rotated upright, unmirrored back-camera pixels.
 * Matches PreviewView FIT_CENTER; HUD coordinates must not use this transform. */
object OverlayCoordinates {
    fun fit(x: Float, y: Float, imageWidth: Int, imageHeight: Int, width: Int, height: Int): Pair<Float, Float> {
        val iw = imageWidth.coerceAtLeast(1)
        val ih = imageHeight.coerceAtLeast(1)
        val scale = minOf(width.toFloat()/iw, height.toFloat()/ih)
        return (width-iw*scale)/2 + x*iw*scale to (height-ih*scale)/2 + y*ih*scale
    }
}
