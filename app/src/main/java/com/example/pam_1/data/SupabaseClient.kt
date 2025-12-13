package com.example.pam_1.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth // Import baru (bukan gotrue)
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    // Ganti dengan URL dan Key projek kamu
    private const val SUPABASE_URL = "https://jhrbjirccxuhtzygwzgx.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_VXT_JRlKZbC8589ztmNGfg_D68XKKBG"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage) // Persiapan untuk storage
    }
}