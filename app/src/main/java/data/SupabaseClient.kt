package com.example.carbotapp.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    private const val SUPABASE_URL = "https://hrryltwdyekufeqrzveo.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhycnlsdHdkeWVrdWZlcXJ6dmVvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE5ODQ3MjgsImV4cCI6MjA3NzU2MDcyOH0.V1GO6XeSmPi3zw_yEzyGbfd-ctXQdJJ8OZ0oMuai9b8"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}