package com.nramos.finance_report.ui.reports

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.FragmentReportsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportsFragment : Fragment(R.layout.fragment_reports) {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportsBinding.bind(view)

        setupListeners()
    }

    private fun setupListeners() {
        // TODO: Implementar listeners
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}