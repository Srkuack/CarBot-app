package com.example.carbotapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.ros.android.MasterChooser

class ModelCarActivity : AppCompatActivity() {

    // --------- Estado de conexión (persistimos último master) ----------
    private var masterUri: String? = null
    private val prefs by lazy { getSharedPreferences("ros_prefs", MODE_PRIVATE) }

    // --------- UI ---------
    private var cameraView: ImageView? = null
    private var stopBtn: ToggleButton? = null
    private var sbSpeed: SeekBar? = null
    private var sbSteer: SeekBar? = null
    private var blinkL: ToggleButton? = null
    private var blinkR: ToggleButton? = null
    private var btnConnect: Button? = null
    private var btnDisconnect: Button? = null

    // MasterChooser moderno (Activity Result API)
    private val chooseMaster =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK && res.data != null) {
                val uri = res.data!!.getStringExtra("ROS_MASTER_URI")
                if (!uri.isNullOrBlank()) {
                    connectToMaster(uri)
                } else {
                    toast("MasterChooser regresó URI vacío")
                }
            } else {
                toast("MasterChooser cancelado")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_car)

        findViewById<ToggleButton>(R.id.button_stop).setOnCheckedChangeListener { _, isChecked ->
            // 1/0 + luces desde callPublishStopStart
            callPublishStopStart(isChecked)

            // UI/seguridad local
            sbSpeed?.progress = 1000  // 0
            sbSteer?.progress = 90    // centro
            RosManager.setVelocity(0.0, 0.0)
        }


        // Cargar último master
        masterUri = prefs.getString("master_uri", null)

        // ====== Referencias UI ======
        cameraView   = findViewById(R.id.cameraView)

        // Si el include de fragment_control no está inflado, lo insertamos
        val root = findViewById<android.view.ViewGroup>(android.R.id.content)
        stopBtn     = findViewById(R.id.button_stop)
        sbSpeed     = findViewById(R.id.seekBar_speed)
        sbSteer     = findViewById(R.id.seekBar_steering)
        blinkL      = findViewById(R.id.toggleButtonBlinkL)
        blinkR      = findViewById(R.id.toggleButtonBlinkR)
        btnConnect  = findViewById(R.id.button_connect)     // en tu layout debería existir
        btnDisconnect = findViewById(R.id.button_disconnect) // en tu layout debería existir

        if (stopBtn == null || sbSpeed == null || sbSteer == null || blinkL == null || blinkR == null) {
            layoutInflater.inflate(R.layout.fragment_control, root, true)
            stopBtn  = findViewById(R.id.button_stop)
            sbSpeed  = findViewById(R.id.seekBar_speed)
            sbSteer  = findViewById(R.id.seekBar_steering)
            blinkL   = findViewById(R.id.toggleButtonBlinkL)
            blinkR   = findViewById(R.id.toggleButtonBlinkR)
            toast("Panel de controles inyectado")
        }

        // Valores por defecto (mitad = 0 velocidad, 90° dirección)
        sbSpeed?.max = 2000
        if (sbSpeed?.progress == 0) sbSpeed?.progress = 1000
        sbSteer?.max = 180
        if (sbSteer?.progress == 0) sbSteer?.progress = 90

        // ====== Listeners UI ======
        sbSpeed?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                RosManager.setVelocity((progress - 1000).toDouble() / 1000.0, 0.0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        sbSteer?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                RosManager.publishSteering(progress.toShort())   // 0..180 (90 centro)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // E-STOP: regresa sliders a 0/90 y manda stop real

        blinkL?.setOnCheckedChangeListener { _, on ->
            RosManager.publishBlinker(if (on) "left_on" else "left_off")
        }
        blinkR?.setOnCheckedChangeListener { _, on ->
            RosManager.publishBlinker(if (on) "right_on" else "right_off")
        }

        // ====== Conectar / Desconectar (sin servicio que crashea) ======
        btnConnect?.setOnClickListener {
            // Abre MasterChooser para elegir ROS_MASTER_URI (no lanza el servicio)
            val i = Intent(this, MasterChooser::class.java)
            chooseMaster.launch(i)
        }

        btnDisconnect?.setOnClickListener {
            RosManager.disconnect(this)
            toast("Desconectado")
        }

        // Si había un master guardado, mostramos botón desconectar activo
        if (!masterUri.isNullOrBlank()) {
            // si quieres autoconectar al abrir, descomenta:
            // connectToMaster(masterUri!!)
        }
    }

    private fun connectToMaster(uri: String) {
        masterUri = uri
        prefs.edit().putString("master_uri", uri).apply()
        try {
            RosManager.connect(this, uri)
            toast("Conectado a $uri")
        } catch (e: Throwable) {
            toast("Error al conectar: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        // Si prefieres mantener conexión, comenta esto
        // RosManager.disconnect(this)
    }

    private var lastSpeed = 0
    private var lastSteer = 90
    private var isStopped = false

    fun callPublishSpeed(progress: Int) {
        lastSpeed = progress
        if (isStopped) {
            RosManager.setVelocity(0.0, 0.0)
            return
        }
        val linear = progress / 100.0
        RosManager.setVelocity(linear, 0.0)
    }

    fun callPublishSteering(progress: Int) {
        lastSteer = progress
        RosManager.publishSteering(progress.toShort())
    }


    fun callPublishBlinkerLight(mode: String) {
        android.util.Log.d("BLINK", "Activity relay=[$mode]")
        RosManager.publishBlinker(mode)
    }

    fun callPublishStopStart(stop: Boolean) {
        // 1) Señal principal (Int16: 1/0)
        RosManager.publishStopStart(stop)

        // 2) Seguridad de motores
        RosManager.setVelocity(0.0, 0.0)

        // 3) Luces de freno por compatibilidad
        if (stop) {
            // al activar E-Stop, prende freno
            RosManager.publishBlinker("stop")
        } else {
            // al liberar E-Stop, apaga freno/intermitentes
            RosManager.publishBlinker("diL")
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
