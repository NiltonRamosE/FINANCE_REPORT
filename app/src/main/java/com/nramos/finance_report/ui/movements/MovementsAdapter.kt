package com.nramos.finance_report.ui.movements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.ItemMovementBinding
import com.nramos.finance_report.domain.model.Report
import com.nramos.finance_report.utils.extensions.formatToDisplayDate
import java.text.NumberFormat
import java.util.Locale

class MovementsAdapter(
    private val onItemClick: (EnrichedReport) -> Unit = {}
) : RecyclerView.Adapter<MovementsAdapter.MovementViewHolder>() {

    private var reports: List<EnrichedReport> = emptyList()

    fun submitList(newReports: List<EnrichedReport>) {
        reports = newReports
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val binding = ItemMovementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovementViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        holder.bind(reports[position])
    }

    override fun getItemCount(): Int = reports.size

    class MovementViewHolder(
        private val binding: ItemMovementBinding,
        private val onItemClick: (EnrichedReport) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(report: EnrichedReport) {
            binding.apply {
                // Configurar icono y color según tipo
                when (report.type) {
                    'I' -> {
                        ivTypeIcon.setImageResource(R.drawable.ic_income)
                        tvAmount.setTextColor(
                            itemView.context.getColor(R.color.green)
                        )
                        tvAmount.text = formatAmount(report.amount, true)
                    }
                    'E' -> {
                        ivTypeIcon.setImageResource(R.drawable.ic_expense)
                        tvAmount.setTextColor(
                            itemView.context.getColor(R.color.red)
                        )
                        tvAmount.text = formatAmount(report.amount, false)
                    }
                }

                tvCategory.text = report.categoryName
                tvSubcategory.text = report.subcategoryName
                tvConcept.text = report.concept ?: "Sin concepto"
                tvDate.text = report.date.formatToDisplayDate()

                root.setOnClickListener {
                    onItemClick(report)
                }
            }
        }

        private fun formatAmount(amount: Double, isIncome: Boolean): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
            val formatted = formatter.format(amount)
            return if (isIncome) "+ $formatted" else "- $formatted"
        }
    }
}