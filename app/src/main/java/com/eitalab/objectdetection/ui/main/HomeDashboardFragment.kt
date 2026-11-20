package com.eitalab.objectdetection.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.eitalab.objectdetection.Home
import com.eitalab.objectdetection.databinding.FragmentHomeDashboardBinding
import com.eitalab.objectdetection.src.services.HistoryRepository

class HomeDashboardFragment : Fragment() {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!

    private val historyRepo by lazy { HistoryRepository.getInstance(requireContext()) }
    private val historyListener = { updateStats() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnOpenDetection.setOnClickListener {
            (activity as? Home)?.selectTab(1)
        }

        binding.cardStartDetection.setOnClickListener {
            (activity as? Home)?.selectTab(1)
        }

        binding.cardLastDetection.setOnClickListener {
            (activity as? Home)?.selectTab(2)
        }

        binding.btnConnectBle.setOnClickListener {
            (activity as? Home)?.triggerBleScan()
        }

        historyRepo.addListener(historyListener)
        updateStats()
        updateBleStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStats()
        updateBleStatus()
    }

    fun updateBleStatus() {
        if (_binding == null) return
        val isConnected = (activity as? Home)?.isDeviceConnected ?: false
        if (isConnected) {
            binding.chipBleStatus.text = "Conectado"
            binding.chipBleStatus.setChipBackgroundColorResource(android.R.color.holo_green_dark)
            binding.tvBleSubtext.text = "Assistive Device conectado e pronto"
            binding.btnConnectBle.text = "Desconectar"
        } else {
            binding.chipBleStatus.text = "Desconectado"
            binding.chipBleStatus.setChipBackgroundColorResource(com.eitalab.objectdetection.R.color.dark_card)
            binding.tvBleSubtext.text = "Assistive Device • Pronto para conectar"
            binding.btnConnectBle.text = "Conectar"
        }
    }

    private fun updateStats() {
        if (_binding == null) return
        val total = historyRepo.getTotalCount()
        binding.tvTotalDetections.text = total.toString()

        val last = historyRepo.getLastDetection()
        if (last != null) {
            binding.tvLastDetection.text = "${last.label} (${last.getConfidencePercent()}%)"
        } else {
            binding.tvLastDetection.text = "Nenhum"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historyRepo.removeListener(historyListener)
        _binding = null
    }

    companion object {
        fun newInstance() = HomeDashboardFragment()
    }
}
