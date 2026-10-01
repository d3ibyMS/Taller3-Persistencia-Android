package com.example.appb

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

class SeguridadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguridad)

        // Configuración visual para alinear la barra de estado con el diseño de la App
        supportActionBar?.hide()
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(
            this, R.color.blue_primary_dark
        )
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val etNota = findViewById<EditText>(R.id.etNotaSecreta)
        val btnCifrado = findViewById<Button>(R.id.btnGuardarCifrado)
        val btnVulnerable = findViewById<Button>(R.id.btnGuardarVulnerable)
        val tvResultado = findViewById<TextView>(R.id.tvResultadoSeguridad)

        btnCifrado.setOnClickListener {
            val texto = etNota.text.toString()
            if (texto.isNotEmpty()) {
                guardarNotaCifrada(texto)
                tvResultado.text = "Guardado de forma segura en: \ndata/data/com.example.appb/shared_prefs/secret_prefs.xml"
            }
        }

        btnVulnerable.setOnClickListener {
            val texto = etNota.text.toString()
            if (texto.isNotEmpty()) {
                guardarNotaVulnerable(texto)
                tvResultado.text = "Guardado de forma VULNERABLE en: \n${getExternalFilesDir(null)?.absolutePath}/nota_vulnerable.txt"
            }
        }
    }

    // 1. Almacenamiento Interno Privado con Cifrado (Cumple requerimiento Punto 4.1)
    private fun guardarNotaCifrada(nota: String) {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "secret_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            sharedPreferences.edit().putString("nota_confidencial", nota).apply()
            Toast.makeText(this, "Nota cifrada correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cifrar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 2. Almacenamiento Externo sin cifrar (Cumple requerimiento Punto 4.2)
    private fun guardarNotaVulnerable(nota: String) {
        try {
            // Guardamos en la ruta externa de la app (simulando la SD) en texto plano
            val archivo = File(getExternalFilesDir(null), "nota_vulnerable.txt")
            archivo.writeText(nota)
            Toast.makeText(this, "Nota vulnerable guardada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar externo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}