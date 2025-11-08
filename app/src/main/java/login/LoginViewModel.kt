package com.example.carbotapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carbotapp.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    fun login(email: String, password: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(
                    io.github.jan.supabase.auth.providers.builtin.Email
                ) {
                    this.email = email
                    this.password = password
                }
                callback(true, "Inicio de sesión exitoso")
            } catch (e: Exception) {
                callback(false, "Error: ${e.message}")
            }
        }
    }
    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val auth = SupabaseClient.client.auth

                // Registrar usuario con Supabase v3
                auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                    this.email = email
                    this.password = password
                }

                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}