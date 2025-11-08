package com.example.carbotapp.ui.login

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.carbotapp.R
import com.example.carbotapp.databinding.ActivityRegisterBinding


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val backButton = findViewById<Button>(R.id.btnBackToLogin)

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.signUp(email, password) { success, error ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Usuario registrado. Verifica tu correo.", Toast.LENGTH_LONG).show()
                        finish() // vuelve al login
                    } else {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        backButton?.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }
}