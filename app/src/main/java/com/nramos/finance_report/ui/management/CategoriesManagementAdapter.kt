package com.nramos.finance_report.ui.management

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.ItemCategoryManagementBinding
import com.nramos.finance_report.domain.model.Category

class CategoriesManagementAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoriesManagementAdapter.ViewHolder>() {

    private var categories: List<Category> = emptyList()

    fun submitList(newList: List<Category>) {
        categories = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    class ViewHolder(
        private val binding: ItemCategoryManagementBinding,
        private val onEditClick: (Category) -> Unit,
        private val onDeleteClick: (Category) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.apply {
                tvCategoryName.text = category.name
                tvCategoryType.text = if (category.inputType == 'I') "Ingreso" else "Egreso"

                // Cambiar ícono según tipo
                ivTypeIcon.setImageResource(
                    if (category.inputType == 'I') R.drawable.ic_income else R.drawable.ic_expense
                )

                btnEdit.setOnClickListener { onEditClick(category) }
                btnDelete.setOnClickListener { onDeleteClick(category) }
            }
        }
    }
}