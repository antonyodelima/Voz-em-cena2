package com.sagun12.vozemcena.video

import android.graphics.Bitmap

object YuvConverter {

    /**
     * Converts a Bitmap to NV12 format (YUV420 semi-planar with interleaved UV)
     * suitable for Android MediaCodec COLOR_FormatYUV420SemiPlanar.
     */
    fun fromBitmapToNV12(bitmap: Bitmap, outputWidth: Int, outputHeight: Int): ByteArray {
        val yuvSize = outputWidth * outputHeight * 3 / 2
        val yuv = ByteArray(yuvSize)

        val argb = IntArray(outputWidth * outputHeight)
        bitmap.getPixels(argb, 0, outputWidth, 0, 0, outputWidth, outputHeight)

        var yIndex = 0
        var uvIndex = outputWidth * outputHeight

        var a: Int
        var r: Int
        var g: Int
        var b: Int
        var y: Int
        var u: Int
        var v: Int

        var index = 0
        for (j in 0 until outputHeight) {
            for (i in 0 until outputWidth) {
                val color = argb[index++]
                r = (color shr 16) and 0xff
                g = (color shr 8) and 0xff
                b = color and 0xff

                // Standard BT.601 RGB to YUV formula
                y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = (y.coerceIn(0, 255)).toByte()

                // 2x2 subsampling for NV12 (U and V interleaved)
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = (u.coerceIn(0, 255)).toByte()
                    yuv[uvIndex++] = (v.coerceIn(0, 255)).toByte()
                }
            }
        }
        return yuv
    }
}
