// File: com.example.pam_1.utils/FileUtils.kt

package com.example.pam_1.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtils {

    /** Convert Uri to File for uploading This creates a temporary file from the Uri */
    suspend fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            withContext(Dispatchers.IO) {
                val inputStream =
                        context.contentResolver.openInputStream(uri) ?: return@withContext null
                val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")

                tempFile.outputStream().use { output -> inputStream.copyTo(output) }
                inputStream.close()

                tempFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ✅ UBAH MENJADI suspend fun (operasi jaringan harus di background thread)
    // ✅ Tambahkan parameter 'mimeType' untuk fleksibilitas
    suspend fun downloadImage(
            context: Context,
            url: String,
            mimeType: String = "image/jpeg"
    ): Boolean {
        // Tentukan ekstensi file berdasarkan mimeType
        val extension =
                when {
                    mimeType.contains("png") -> ".png"
                    // Tambahkan logika lain jika perlu (e.g. gif, webp)
                    else -> ".jpg" // Default ke JPG
                }

        // Buat nama file unik
        val fileName = "profile_${UUID.randomUUID()}$extension"

        // --- 1. Ambil bytes dari URL (harus di Dispatchers.IO) ---
        val bytes =
                try {
                    withContext(Dispatchers.IO) {
                        // Hapus query parameter cache busting sebelum membaca bytes
                        val cleanUrl = url.substringBefore('?')
                        URL(cleanUrl).readBytes()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }

        // --- 2. Simpan ke MediaStore ---
        return try {
            val contentValues =
                    ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        // ✅ PERBAIKI: MIME Type harus spesifik, bukan gabungan
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)

                        // ✅ TENTUKAN LOKASI: Simpan di folder Pictures/NamaApp
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                    MediaStore.Images.Media.RELATIVE_PATH,
                                    "${Environment.DIRECTORY_PICTURES}/PAM_ProfileApp"
                            )
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

            val uri =
                    context.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                    )
                            ?: return false

            // Tulis bytes ke OutputStream (harus di Dispatchers.IO)
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { output -> output.write(bytes) }
            }

            // Finalisasi (hapus status pending)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
