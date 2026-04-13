package com.nramos.finance_report.ui.movements

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.FragmentMovementsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MovementsFragment : Fragment(R.layout.fragment_movements) {

    private var _binding: FragmentMovementsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MovementsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMovementsBinding.bind(view)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // TODO: Configurar RecyclerView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}