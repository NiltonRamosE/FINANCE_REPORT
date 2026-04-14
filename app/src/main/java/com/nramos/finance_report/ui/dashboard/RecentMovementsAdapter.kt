package com.nramos.finance_report.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.ItemRecentMovementBinding
import com.nramos.finance_report.domain.model.Report
import java.text.NumberFormat
import java.util.Locale

class RecentMovementsAdapter(
    private val onItemClick: (Report) -> Unit = {}
) : RecyclerView.Adapter<RecentMovementsAdapter.ViewHolder>() {

    private var reports: List<Report> = emptyList()

    fun submitList(newReports: List<Report>) {
        reports = newReports
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentMovementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reports[position])
    }

    override fun getItemCount(): Int = reports.size

    class ViewHolder(
        private val binding: ItemRecentMovementBinding,
        private val onItemClick: (Report) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report) {
            binding.apply {
                tvConcept.text = report.concept ?: "Sin concepto"
                tvDate.text = formatDate(report.date)

                val formattedAmount = formatAmount(report.amount)
                tvAmount.text = if (report.type == 'I') "+ $formattedAmount" else "- $formattedAmount"

                tvAmount.setTextColor(
                    if (report.type == 'I') {
                        itemView.context.getColor(R.color.finance_green_600)
                    } else {
                        itemView.context.getColor(R.color.finance_red_600)
                    }
                )

                root.setOnClickListener {
                    onItemClick(report)
                }
            }
        }

        private fun formatAmount(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
            return formatter.format(amount)
        }

        private fun formatDate(date: String): String {
            return try {
                val parts = date.split("-")
                if (parts.size == 3) {
                    "${parts[2]}/${parts[1]}/${parts[0]}"
                } else {
                    date
                }
            } catch (e: Exception) {
                date
            }
        }
    }
}