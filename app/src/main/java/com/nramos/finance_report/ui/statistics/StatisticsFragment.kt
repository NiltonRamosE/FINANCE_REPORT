package com.nramos.finance_report.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.FragmentStatisticsBinding
import com.nramos.finance_report.domain.model.CategoryStat
import com.nramos.finance_report.domain.model.MonthlyStat
import com.nramos.finance_report.domain.model.SubcategoryStat
import com.nramos.finance_report.utils.extensions.showToast
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()


    private lateinit var subcategoriesAdapter: TopSubcategoriesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        setupObservers()
        setupCharts()
    }

    private fun setupRecyclerView() {
        subcategoriesAdapter = TopSubcategoriesAdapter { subcategory ->
            showToast("${subcategory.subcategoryName}: ${subcategory.totalAmount}")
        }

        binding.rvTopSubcategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subcategoriesAdapter
        }
    }

    private fun updateSubcategoriesList(subcategories: List<SubcategoryStat>) {
        subcategoriesAdapter.submitList(subcategories)
    }

    private fun setupListeners() {
        binding.apply {
            btnWeek.setOnClickListener {
                viewModel.onEvent(StatisticsEvent.OnFilterTypeChanged(FilterType.WEEK))
            }
            btnMonth.setOnClickListener {
                viewModel.onEvent(StatisticsEvent.OnFilterTypeChanged(FilterType.MONTH))
            }
            btnYear.setOnClickListener {
                viewModel.onEvent(StatisticsEvent.OnFilterTypeChanged(FilterType.YEAR))
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                updateTotals(state)
                updateLineChart(state.monthlyStats)
                updatePieChart(state.topCategories)
                updateSubcategoriesList(state.topSubcategories)

                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                state.error?.let { error ->
                    showToast(error)
                }

                // Actualizar estilo de botones
                updateFilterButtons(state.filterType)
            }
        }
    }

    private fun setupCharts() {
        setupLineChart()
        setupPieChart()
    }

    private fun setupLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = Legend.LegendOrientation.VERTICAL
            legend.textSize = 10f
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
        }
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleRadius(40f)
            setTransparentCircleRadius(45f)
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.textSize = 10f
        }
    }

    private fun updateTotals(state: StatisticsState) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        binding.tvTotalIncome.text = formatter.format(state.totalIncome)
        binding.tvTotalExpense.text = formatter.format(state.totalExpense)
        binding.tvBalance.text = formatter.format(state.balance)

        // Cambiar color del balance según sea positivo o negativo
        val balanceColor = if (state.balance >= 0) {
            ContextCompat.getColor(requireContext(), R.color.finance_green_600)
        } else {
            ContextCompat.getColor(requireContext(), R.color.finance_red_600)
        }
        binding.tvBalance.setTextColor(balanceColor)
    }

    private fun updateLineChart(monthlyStats: List<MonthlyStat>) {
        if (monthlyStats.isEmpty()) return

        val entries = monthlyStats.mapIndexed { index, stat ->
            listOf(
                Entry(index.toFloat(), stat.income.toFloat()),
                Entry(index.toFloat(), stat.expense.toFloat())
            )
        }

        val incomeEntries = monthlyStats.mapIndexed { index, stat ->
            Entry(index.toFloat(), stat.income.toFloat())
        }
        val expenseEntries = monthlyStats.mapIndexed { index, stat ->
            Entry(index.toFloat(), stat.expense.toFloat())
        }

        val incomeDataSet = LineDataSet(incomeEntries, "Ingresos").apply {
            color = ContextCompat.getColor(requireContext(), R.color.finance_green_600)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.finance_green_600))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9f
        }

        val expenseDataSet = LineDataSet(expenseEntries, "Egresos").apply {
            color = ContextCompat.getColor(requireContext(), R.color.finance_red_600)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.finance_red_600))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9f
        }

        val lineData = LineData(incomeDataSet, expenseDataSet)

        // Configurar etiquetas del eje X
        binding.lineChart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index < monthlyStats.size) monthlyStats[index].month else ""
            }
        }

        binding.lineChart.data = lineData
        binding.lineChart.invalidate()
    }

    private fun updatePieChart(categories: List<CategoryStat>) {
        if (categories.isEmpty()) return

        val entries = categories.map { category ->
            PieEntry(category.percentage, category.categoryName)
        }

        val dataSet = PieDataSet(entries, "Categorías").apply {
            colors = categories.map { it.color }
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            setDrawIcons(false)
            setDrawValues(true)
            valueFormatter = PercentFormatter(binding.pieChart)
        }

        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()
    }

    private fun updateFilterButtons(currentFilter: FilterType) {
        val buttons = listOf(binding.btnWeek, binding.btnMonth, binding.btnYear)

        buttons.forEach { button ->
            button.isEnabled = true
            button.isChecked = false
        }

        when (currentFilter) {
            FilterType.WEEK -> binding.btnWeek.isChecked = true
            FilterType.MONTH -> binding.btnMonth.isChecked = true
            FilterType.YEAR -> binding.btnYear.isChecked = true
            else -> {}
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}