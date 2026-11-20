package com.eitalab.objectdetection.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.eitalab.objectdetection.databinding.FragmentHistoryBinding
import com.eitalab.objectdetection.src.services.HistoryRepository

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val historyRepo by lazy { HistoryRepository.getInstance(requireContext()) }
    private lateinit var adapter: HistoryAdapter

    private val historyListener: () -> Unit = {
        activity?.runOnUiThread {
            loadHistory()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        binding.btnClearHistory.setOnClickListener {
            showClearConfirmationDialog()
        }

        historyRepo.addListener(historyListener)
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        if (_binding == null) return
        val items = historyRepo.getHistory()
        adapter.updateList(items)

        if (items.isEmpty()) {
            binding.llEmptyState.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
            binding.btnClearHistory.visibility = View.GONE
            binding.tvHistoryCount.text = "Nenhum objeto registrado"
        } else {
            binding.llEmptyState.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
            binding.btnClearHistory.visibility = View.VISIBLE
            binding.tvHistoryCount.text = "${items.size} detecções registradas"
        }
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Limpar Histórico")
            .setMessage("Tem certeza que deseja apagar todas as detecções salvas?")
            .setPositiveButton("Limpar") { _, _ ->
                historyRepo.clearHistory()
                loadHistory()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historyRepo.removeListener(historyListener)
        _binding = null
    }

    companion object {
        fun newInstance() = HistoryFragment()
    }
}
