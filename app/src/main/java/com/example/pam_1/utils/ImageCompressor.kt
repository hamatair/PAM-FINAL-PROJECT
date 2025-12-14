package com.example.pam_1.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object ImageCompressor {

    private const val MAX_WIDTH = 1920
    private const val MAX_HEIGHT = 1920
    private const val JPEG_QUALITY = 80
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB

    /**
     * Compress an image from URI
     * @param context Android context
     * @param uri Image URI
     * @return Compressed image as ByteArray
     */
    fun compressImage(context: Context, uri: Uri): ByteArray? {
        try {
            // Read image from URI
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Get orientation and rotate if needed
            val rotatedBitmap = rotateImageIfRequired(context, originalBitmap, uri)

            // Resize to max dimensions
            val resizedBitmap = resizeBitmap(rotatedBitmap, MAX_WIDTH, MAX_HEIGHT)

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

            // Clean up
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()

            return outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Compress and save to temporary file
     * @param context Android context
     * @param uri Image URI
     * @return Temporary file with compressed image
     */
    fun compressImageToFile(context: Context, uri: Uri): File? {
        try {
            val compressedData = compressImage(context, uri) ?: return null

            // Create temp file
            val tempFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)

            // Write compressed data
            FileOutputStream(tempFile).use { it.write(compressedData) }

            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /** Resize bitmap to fit within max dimensions */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate scale factor
        val scale = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

        // If already small enough, return original
        if (scale >= 1f) {
            return bitmap
        }

        // Calculate new dimensions
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        // Resize
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /** Rotate image based on EXIF orientation */
    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation =
                    exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                    )

            val rotation =
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> return bitmap
                    }

            val matrix = Matrix()
            matrix.postRotate(rotation)

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            return bitmap
        }
    }
}
