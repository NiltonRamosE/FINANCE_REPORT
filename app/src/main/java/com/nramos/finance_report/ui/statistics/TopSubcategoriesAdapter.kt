package com.nramos.finance_report.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.ItemTopSubcategoryBinding
import com.nramos.finance_report.domain.model.SubcategoryStat
import java.text.NumberFormat
import java.util.Locale

class TopSubcategoriesAdapter(
    private val onItemClick: (SubcategoryStat) -> Unit = {}
) : RecyclerView.Adapter<TopSubcategoriesAdapter.ViewHolder>() {

    private var items: List<SubcategoryStat> = emptyList()

    fun submitList(newItems: List<SubcategoryStat>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopSubcategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemTopSubcategoryBinding,
        private val onItemClick: (SubcategoryStat) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubcategoryStat, position: Int) {
            binding.apply {
                tvPosition.text = "#$position"
                tvSubcategoryName.text = item.subcategoryName
                tvCategoryName.text = item.categoryName
                tvAmount.text = formatAmount(item.totalAmount)
                tvPercentage.text = "${String.format("%.1f", item.percentage)}%"

                // Barra de progreso visual
                val progress = (item.percentage / 100).toFloat()
                progressBar.progress = (progress * 100).toInt()

                root.setOnClickListener { onItemClick(item) }
            }
        }

        private fun formatAmount(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
            return formatter.format(amount)
        }
    }
}