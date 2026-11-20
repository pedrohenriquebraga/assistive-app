package com.eitalab.objectdetection.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.eitalab.objectdetection.Home
import com.eitalab.objectdetection.databinding.FragmentSettingsBinding
import com.eitalab.objectdetection.src.services.StorageService

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val storage by lazy { StorageService(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup TTS Switch
        val isTtsEnabled = storage.getData("pref_tts_enabled", "true") == "true"
        binding.switchTts.isChecked = isTtsEnabled
        binding.switchTts.setOnCheckedChangeListener { _, isChecked ->
            storage.saveData("pref_tts_enabled", isChecked.toString())
            Toast.makeText(
                requireContext(),
                if (isChecked) "Feedback por voz ativado" else "Feedback por voz desativado",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Setup Vibration Switch
        val isVibrationEnabled = storage.getData("pref_vibration_enabled", "true") == "true"
        binding.switchVibration.isChecked = isVibrationEnabled
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            storage.saveData("pref_vibration_enabled", isChecked.toString())
            Toast.makeText(
                requireContext(),
                if (isChecked) "Vibração ativada" else "Vibração desativada",
                Toast.LENGTH_SHORT
            ).show()
        }

        // BLE Connect Button
        binding.btnBleScanConnect.setOnClickListener {
            val home = activity as? Home
            if (home?.isDeviceConnected == true) {
                home.disconnectBle()
            } else {
                home?.triggerBleScan()
            }
        }

        updateBleStatus()
    }

    override fun onResume() {
        super.onResume()
        updateBleStatus()
    }

    fun updateBleStatus() {
        if (_binding == null) return
        val isConnected = (activity as? Home)?.isDeviceConnected ?: false
        if (isConnected) {
            binding.tvBleStatusSettings.text = "Status: Conectado ao dispositivo"
            binding.btnBleScanConnect.text = "Desconectar Dispositivo"
        } else {
            binding.tvBleStatusSettings.text = "Status: Desconectado"
            binding.btnBleScanConnect.text = "Buscar e Conectar Dispositivo"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
