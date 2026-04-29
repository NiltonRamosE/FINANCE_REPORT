package com.nramos.finance_report.ui.management

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.FragmentManagementBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManagementFragment : Fragment(R.layout.fragment_management) {

    private var _binding: FragmentManagementBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManagementBinding.bind(view)

        setupViewPager()
    }

    private fun setupViewPager() {
        val pagerAdapter = ManagementPagerAdapter(requireActivity())
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Categorías"
                else -> "Subcategorías"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}