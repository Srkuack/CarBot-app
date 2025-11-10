package com.example.carbotapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.ToggleButton
import androidx.fragment.app.Fragment

class ControlFragment : Fragment() {

    private val host: ModelCarActivity
        get() = requireActivity() as ModelCarActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_control, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val stop  = view.findViewById<ToggleButton>(R.id.button_stop)
        val sbSpeed = view.findViewById<SeekBar>(R.id.seekBar_speed)
        val sbSteer = view.findViewById<SeekBar>(R.id.seekBar_steering)
        val tbLeft  = view.findViewById<ToggleButton>(R.id.toggleButtonBlinkL)
        val tbRight = view.findViewById<ToggleButton>(R.id.toggleButtonBlinkR)


        fun send(mode: String) {
            android.util.Log.d("BLINK", "Fragment send=[$mode]")
            (activity as? ModelCarActivity)?.callPublishBlinkerLight(mode)
        }

        var squelch = false

        // Valores seguros por si el XML no trae max/progress
        if (sbSpeed.max != 2000) sbSpeed.max = 2000
        if (sbSpeed.progress == 0) sbSpeed.progress = 1000  // centro
        if (sbSteer.max != 180) sbSteer.max = 180
        if (sbSteer.progress == 0) sbSteer.progress = 90    // centro

        // ===== Velocidad: 0..2000 → -1000..+1000 (short) → host.callPublishSpeed(short)
        sbSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val valShort: Int = (progress - 1000).toInt()
                host.callPublishSpeed(valShort)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // ===== Dirección: 0..180 (90 centro) → host.callPublishSteering(short)
        sbSteer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                host.callPublishSteering(progress.toInt())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // ===== E-STOP: usa exactamente tu firma callPublishStopStart(int)


        tbLeft.setOnCheckedChangeListener { _, isChecked ->
            if (squelch) return@setOnCheckedChangeListener
            if (isChecked) {
                squelch = true
                if (tbRight.isChecked) tbRight.isChecked = false
                squelch = false
                send("le")          // ← izquierda
            } else if (!tbRight.isChecked) {
                send("diL")         // ← apagar si ambos quedan OFF
            }
        }

        tbRight.setOnCheckedChangeListener { _, isChecked ->
            if (squelch) return@setOnCheckedChangeListener
            if (isChecked) {
                squelch = true
                if (tbLeft.isChecked) tbLeft.isChecked = false
                squelch = false
                send("ri")          // ← derecha
            } else if (!tbLeft.isChecked) {
                send("diL")         // ← apagar
            }
        }
    }
}
