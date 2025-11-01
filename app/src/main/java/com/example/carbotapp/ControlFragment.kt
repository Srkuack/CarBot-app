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
        val stop  = view.findViewById<ToggleButton>(R.id.button_stop)
        val sbSpeed = view.findViewById<SeekBar>(R.id.seekBar_speed)
        val sbSteer = view.findViewById<SeekBar>(R.id.seekBar_steering)
        val blL   = view.findViewById<ToggleButton>(R.id.toggleButtonBlinkL)
        val blR   = view.findViewById<ToggleButton>(R.id.toggleButtonBlinkR)

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
        stop.setOnCheckedChangeListener { _, isChecked ->
            host.callPublishStopStart(if (isChecked) true else false)
            // opcional: cuando haces STOP, re-centra sliders
            if (isChecked) {
                sbSpeed.progress = 1000
                sbSteer.progress = 90
            }
        }

        // ===== Direccionales: mismos tokens que ya tienes ("Lle","Lri","Lstop")
        // Exclusividad simple entre los ToggleButton (no RadioGroup)
        blL.setOnCheckedChangeListener { _, on ->
            if (on) {
                if (blR.isChecked) blR.isChecked = false
                host.callPublishBlinkerLight("Lle")
            } else {
                // sólo envía stop si ninguno quedó activo
                if (!blR.isChecked) host.callPublishBlinkerLight("Lstop")
            }
        }

        blR.setOnCheckedChangeListener { _, on ->
            if (on) {
                if (blL.isChecked) blL.isChecked = false
                host.callPublishBlinkerLight("Lri")
            } else {
                if (!blL.isChecked) host.callPublishBlinkerLight("Lstop")
            }
        }
    }
}
