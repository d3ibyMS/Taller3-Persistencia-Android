package com.udenar.persistencia.punto1

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * TALLER 3 - PUNTO 1: Persistencia del Estado Temporal del Ciclo de Vida
 *
 * Esta actividad implementa un formulario de registro MULTI-PASO con un
 * temporizador activo, SIN usar arquitectura ViewModel (a propósito, tal
 * como lo exige el enunciado), para demostrar el manejo manual y explícito
 * de los callbacks del ciclo de vida de Android relacionados con la
 * persistencia TEMPORAL del estado de la interfaz:
 *
 *   - onSaveInstanceState(Bundle)     -> guarda estado antes de destruir la Activity
 *   - onRestoreInstanceState(Bundle)  -> restaura el estado guardado
 *   - onPause()                       -> guarda un "borrador rápido" (edición activa)
 *   - onStop()                        -> guarda el borrador completo (persistente, en disco)
 *
 * IMPORTANTE - Diferencia conceptual clave que pide el punto 1.4:
 *
 *   1) El Bundle de onSaveInstanceState/onRestoreInstanceState es memoria
 *      TEMPORAL, gestionada por el sistema operativo. Sobrevive a cambios
 *      de configuración (rotación) y a la destrucción/recreación del
 *      proceso EN SEGUNDO PLANO (ej. "No conservar actividades"), pero
 *      NO sobrevive a que el usuario cierre la app explícitamente desde
 *      "Recientes" (swipe) ni a un reinicio del dispositivo. Además tiene
 *      un límite práctico de tamaño (se transporta vía Binder IPC), por lo
 *      que NUNCA debe usarse para colecciones grandes, bitmaps, resultados
 *      de red, etc.
 *
 *   2) Los SharedPreferences que se escriben en onPause()/onStop() SÍ son
 *      almacenamiento persistente en disco: sobreviven a que el proceso
 *      muera por completo y a que el usuario cierre la app. Por eso se
 *      usan aquí como una capa adicional de seguridad tipo "borrador",
 *      independiente del Bundle del ciclo de vida.
 */
class MainActivity : AppCompatActivity() {

    // ---------- Referencias de vistas (sin ViewBinding, findViewById puro) ----------
    private lateinit var tvStepIndicator: android.widget.TextView
    private lateinit var tvTimer: android.widget.TextView
    private lateinit var btnTimerToggle: android.widget.Button
    private lateinit var layoutPaso1: android.widget.LinearLayout
    private lateinit var layoutPaso2: android.widget.LinearLayout
    private lateinit var layoutPaso3: android.widget.LinearLayout
    private lateinit var btnAnterior: android.widget.Button
    private lateinit var btnSiguiente: android.widget.Button
    private lateinit var btnFinalizar: android.widget.Button
    private lateinit var tvDebugInfo: android.widget.TextView

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etComentarios: EditText

    // ---------- Estado "en memoria" de la pantalla (esto es lo que se pierde
    // si no se persiste manualmente ante un cambio de configuración o
    // destrucción de proceso) ----------
    private var currentStep = 0                 // 0, 1, 2  -> Paso 1, 2, 3
    private var elapsedSeconds = 0               // conteo del temporizador
    private var isTimerRunning = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    // ---------- Persistencia liviana tipo "borrador" (SharedPreferences) ----------
    private val PREFS_NAME = "borrador_formulario_punto1"

    companion object {
        private const val TAG = "PersistenciaPunto1"

        // Claves usadas tanto en el Bundle de instancia como en SharedPreferences,
        // para mantener un único vocabulario de estado.
        private const val KEY_STEP = "key_step"
        private const val KEY_SECONDS = "key_seconds"
        private const val KEY_RUNNING = "key_running"
        private const val KEY_FOCUS_ID = "key_focus_id"
        private const val KEY_CURSOR_POS = "key_cursor_pos"
        private const val KEY_NOMBRE = "key_nombre"
        private const val KEY_APELLIDO = "key_apellido"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_TELEFONO = "key_telefono"
        private const val KEY_DIRECCION = "key_direccion"
        private const val KEY_COMENTARIOS = "key_comentarios"
    }

    // =====================================================================
    // onCreate: aquí se crean las vistas. Si el proceso murió del todo (no
    // hay Bundle porque el sistema no llegó a llamar onSaveInstanceState o
    // el usuario cerró la app manualmente), buscamos un borrador persistente
    // en SharedPreferences y le preguntamos al usuario si quiere recuperarlo.
    // =====================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupTimerRunnable()
        setupListeners()

        if (savedInstanceState == null) {
            // Proceso nuevo / primera vez que se abre la Activity en esta sesión.
            // NOTA: la restauración por rotación/"No conservar actividades" NO
            // pasa por aquí con datos útiles; esa la maneja onRestoreInstanceState.
            Log.d(TAG, "onCreate SIN savedInstanceState -> proceso nuevo")
            checkForDraftAndOfferRestore()
        } else {
            Log.d(TAG, "onCreate CON savedInstanceState -> Android restaurará vía onRestoreInstanceState")
        }

        updateStepUI()
        updateTimerText()
        updateDebugInfo("onCreate")
    }

    private fun bindViews() {
        tvStepIndicator = findViewById(R.id.tvStepIndicator)
        tvTimer = findViewById(R.id.tvTimer)
        btnTimerToggle = findViewById(R.id.btnTimerToggle)
        layoutPaso1 = findViewById(R.id.layoutPaso1)
        layoutPaso2 = findViewById(R.id.layoutPaso2)
        layoutPaso3 = findViewById(R.id.layoutPaso3)
        btnAnterior = findViewById(R.id.btnAnterior)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        tvDebugInfo = findViewById(R.id.tvDebugInfo)

        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etDireccion = findViewById(R.id.etDireccion)
        etComentarios = findViewById(R.id.etComentarios)
    }

    private fun setupListeners() {
        btnSiguiente.setOnClickListener {
            if (currentStep < 2) {
                currentStep++
                updateStepUI()
            }
        }
        btnAnterior.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                updateStepUI()
            }
        }
        btnFinalizar.setOnClickListener {
            // Aquí, en un caso real, el registro FINAL sí debería guardarse en
            // almacenamiento persistente definitivo (Room/SQLite, Punto 2 del
            // taller), porque ya no es un "borrador temporal" sino el dato
            // definitivo del usuario. En este Punto 1 solo limpiamos el borrador.
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
            android.widget.Toast.makeText(
                this,
                "Formulario finalizado. Borrador temporal eliminado.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        btnTimerToggle.setOnClickListener {
            if (isTimerRunning) stopTimer() else startTimer()
        }
    }

    // =====================================================================
    // 1) CAPTURA DEL ESTADO -> onSaveInstanceState
    //    El sistema llama a este método ANTES de destruir la Activity por
    //    un cambio de configuración (rotación) o por necesidad de memoria
    //    mientras está en background. Aquí guardamos en el Bundle:
    //      - el texto de TODOS los campos editables
    //      - el paso actual del formulario
    //      - el estado del temporizador (segundos y si está corriendo)
    //      - el foco actual del cursor Y su posición dentro del texto
    // =====================================================================
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState -> guardando estado temporal en Bundle")

        outState.putInt(KEY_STEP, currentStep)
        outState.putInt(KEY_SECONDS, elapsedSeconds)
        outState.putBoolean(KEY_RUNNING, isTimerRunning)

        outState.putString(KEY_NOMBRE, etNombre.text.toString())
        outState.putString(KEY_APELLIDO, etApellido.text.toString())
        outState.putString(KEY_EMAIL, etEmail.text.toString())
        outState.putString(KEY_TELEFONO, etTelefono.text.toString())
        outState.putString(KEY_DIRECCION, etDireccion.text.toString())
        outState.putString(KEY_COMENTARIOS, etComentarios.text.toString())

        // Foco del cursor: guardamos el id de la vista enfocada y, si es un
        // EditText, la posición exacta del cursor dentro del texto.
        val focused = currentFocus
        outState.putInt(KEY_FOCUS_ID, focused?.id ?: View.NO_ID)
        if (focused is EditText) {
            outState.putInt(KEY_CURSOR_POS, focused.selectionStart)
        }

        updateDebugInfo("onSaveInstanceState")
    }

    // =====================================================================
    // 2) RECUPERACIÓN EXPLÍCITA -> onRestoreInstanceState
    //    Se ejecuta automáticamente DESPUÉS de onStart() cuando la Activity
    //    se recrea a partir de un Bundle guardado previamente. Es el lugar
    //    recomendado (junto con onCreate) para restaurar estado de UI,
    //    porque aquí ya existen todas las vistas infladas.
    // =====================================================================
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState -> restaurando estado temporal desde Bundle")

        currentStep = savedInstanceState.getInt(KEY_STEP, 0)
        elapsedSeconds = savedInstanceState.getInt(KEY_SECONDS, 0)
        isTimerRunning = savedInstanceState.getBoolean(KEY_RUNNING, false)

        etNombre.setText(savedInstanceState.getString(KEY_NOMBRE, ""))
        etApellido.setText(savedInstanceState.getString(KEY_APELLIDO, ""))
        etEmail.setText(savedInstanceState.getString(KEY_EMAIL, ""))
        etTelefono.setText(savedInstanceState.getString(KEY_TELEFONO, ""))
        etDireccion.setText(savedInstanceState.getString(KEY_DIRECCION, ""))
        etComentarios.setText(savedInstanceState.getString(KEY_COMENTARIOS, ""))

        updateStepUI()
        updateTimerText()

        // Si el temporizador estaba corriendo antes de destruirse la Activity,
        // lo reanudamos automáticamente para que el usuario no note el corte.
        if (isTimerRunning) startTimer()

        // Restaurar foco + posición del cursor. Se hace con post{} porque el
        // layout puede no estar completamente medido/dibujado todavía.
        val focusId = savedInstanceState.getInt(KEY_FOCUS_ID, View.NO_ID)
        val cursorPos = savedInstanceState.getInt(KEY_CURSOR_POS, -1)
        if (focusId != View.NO_ID) {
            val viewToFocus = findViewById<View>(focusId)
            viewToFocus?.post {
                viewToFocus.requestFocus()
                if (viewToFocus is EditText && cursorPos in 0..viewToFocus.text.length) {
                    viewToFocus.setSelection(cursorPos)
                }
            }
        }

        updateDebugInfo("onRestoreInstanceState")
    }

    // =====================================================================
    // 3) BORRADORES RÁPIDOS -> onPause() y onStop()
    //
    //    Estos NO reemplazan al Bundle: son una capa adicional que persiste
    //    en disco (SharedPreferences) para cubrir el escenario en el que el
    //    proceso NO vuelve a arrancar con un Bundle disponible (por ejemplo,
    //    el usuario cierra la app desde "Recientes" o el sistema la mata
    //    mucho tiempo después). Así el usuario puede recuperar su progreso
    //    la próxima vez que abra la app desde cero.
    //
    //    onPause() -> se llama SIEMPRE que la Activity deja de estar en
    //    primer plano (incluso si solo aparece un diálogo encima). Aquí
    //    guardamos un borrador "parcial", muy barato, pensado para
    //    interrupciones breves.
    //
    //    onStop() -> se llama cuando la Activity deja de ser VISIBLE del
    //    todo. Aquí guardamos el borrador COMPLETO, ya que es la última
    //    oportunidad confiable antes de que el proceso pueda ser matado.
    // =====================================================================
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause -> guardando borrador rápido (edición activa)")
        saveDraftToPrefs()
        updateDebugInfo("onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop -> guardando borrador completo antes de pasar a segundo plano")
        saveDraftToPrefs()
        updateDebugInfo("onStop")
    }

    private fun saveDraftToPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_STEP, currentStep)
            putInt(KEY_SECONDS, elapsedSeconds)
            putBoolean(KEY_RUNNING, isTimerRunning)
            putString(KEY_NOMBRE, etNombre.text.toString())
            putString(KEY_APELLIDO, etApellido.text.toString())
            putString(KEY_EMAIL, etEmail.text.toString())
            putString(KEY_TELEFONO, etTelefono.text.toString())
            putString(KEY_DIRECCION, etDireccion.text.toString())
            putString(KEY_COMENTARIOS, etComentarios.text.toString())
            putLong("timestamp", System.currentTimeMillis())
            apply()
        }
    }

    private fun checkForDraftAndOfferRestore() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val nombreGuardado = prefs.getString(KEY_NOMBRE, "") ?: ""
        val comentariosGuardados = prefs.getString(KEY_COMENTARIOS, "") ?: ""
        val haySomethingGuardado = prefs.contains(KEY_STEP) &&
                (nombreGuardado.isNotBlank() || comentariosGuardados.isNotBlank() ||
                        (prefs.getString(KEY_APELLIDO, "") ?: "").isNotBlank())

        if (haySomethingGuardado) {
            AlertDialog.Builder(this)
                .setTitle("Borrador encontrado")
                .setMessage("Encontramos información sin terminar de una sesión anterior. ¿Deseas recuperarla?")
                .setCancelable(false)
                .setPositiveButton("Recuperar") { _, _ -> restoreDraftFromPrefs(prefs) }
                .setNegativeButton("Descartar") { _, _ -> prefs.edit().clear().apply() }
                .show()
        }
    }

    private fun restoreDraftFromPrefs(prefs: SharedPreferences) {
        currentStep = prefs.getInt(KEY_STEP, 0)
        elapsedSeconds = prefs.getInt(KEY_SECONDS, 0)
        // Decisión de diseño: NO reanudamos automáticamente un temporizador
        // que quedó "corriendo" en una sesión de hace horas/días; eso no
        // reflejaría un tiempo real transcurrido. Por eso isTimerRunning se
        // fuerza a false al recuperar un borrador persistente antiguo (a
        // diferencia de la restauración desde Bundle, que sí es inmediata).
        isTimerRunning = false

        etNombre.setText(prefs.getString(KEY_NOMBRE, ""))
        etApellido.setText(prefs.getString(KEY_APELLIDO, ""))
        etEmail.setText(prefs.getString(KEY_EMAIL, ""))
        etTelefono.setText(prefs.getString(KEY_TELEFONO, ""))
        etDireccion.setText(prefs.getString(KEY_DIRECCION, ""))
        etComentarios.setText(prefs.getString(KEY_COMENTARIOS, ""))

        updateStepUI()
        updateTimerText()

        android.widget.Toast.makeText(this, "Borrador recuperado", android.widget.Toast.LENGTH_SHORT).show()
    }

    // =====================================================================
    // Temporizador manual (sin Chronometer, para controlar exactamente qué
    // se guarda y se restaura)
    // =====================================================================
    private fun setupTimerRunnable() {
        timerRunnable = Runnable {
            elapsedSeconds++
            updateTimerText()
            handler.postDelayed(timerRunnable, 1000)
        }
    }

    private fun startTimer() {
        isTimerRunning = true
        btnTimerToggle.text = "Pausar"
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun stopTimer() {
        isTimerRunning = false
        btnTimerToggle.text = "Iniciar"
        handler.removeCallbacks(timerRunnable)
    }

    private fun updateTimerText() {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        tvTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    // =====================================================================
    // UI de navegación entre pasos
    // =====================================================================
    private fun updateStepUI() {
        layoutPaso1.visibility = if (currentStep == 0) View.VISIBLE else View.GONE
        layoutPaso2.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        layoutPaso3.visibility = if (currentStep == 2) View.VISIBLE else View.GONE

        tvStepIndicator.text = "Paso ${currentStep + 1} de 3"
        btnAnterior.isEnabled = currentStep > 0
        btnSiguiente.visibility = if (currentStep < 2) View.VISIBLE else View.GONE
        btnFinalizar.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
    }

    private fun updateDebugInfo(callback: String) {
        // Panel visual solo para fines didácticos: permite ver en pantalla,
        // durante la sustentación, en qué callback del ciclo de vida se está
        // guardando o restaurando el estado.
        tvDebugInfo.text = "Último callback de persistencia ejecutado: $callback " +
                "| paso=${currentStep + 1} | timer=${elapsedSeconds}s | corriendo=$isTimerRunning"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
