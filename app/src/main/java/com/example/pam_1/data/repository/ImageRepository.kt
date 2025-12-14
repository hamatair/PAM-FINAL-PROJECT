package com.example.pam_1.data.model.repository

import android.content.Context
import android.net.Uri
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.utils.ImageCompressor
import io.github.jan.supabase.storage.storage
import java.util.UUID

class ImageRepository {

    private val client = SupabaseClient.client

    suspend fun uploadTugasImage(
        context: Context,
        imageUri: Uri
    ): String {

        // 1. Compress image (PNG/JPG/JPEG semua aman)
        val compressedBytes =
            ImageCompressor.compressImage(context, imageUri)
                ?: throw Exception("Gagal compress image")

        // 2. Nama file unik
        val fileName = "tugas/${UUID.randomUUID()}.jpg"

        // 3. Upload (TANPA contentType)
        client.storage
            .from("tugas") // ⚠️ bucket HARUS konsisten
            .upload(
                path = fileName,
                data = compressedBytes
            ) {
                upsert = false
            }

        // 4. Public URL
        return client.storage
            .from("tugas-images")
            .publicUrl(fileName)
    }
}
