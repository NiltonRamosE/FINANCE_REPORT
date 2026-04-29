package com.nramos.finance_report.ui.management

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nramos.finance_report.databinding.ItemSubcategoryManagementBinding
import com.nramos.finance_report.domain.model.Subcategory

class SubcategoriesManagementAdapter(
    private val onEditClick: (Subcategory) -> Unit,
    private val onDeleteClick: (Subcategory) -> Unit
) : RecyclerView.Adapter<SubcategoriesManagementAdapter.ViewHolder>() {

    private var subcategories: List<Subcategory> = emptyList()
    private var categoryNames: Map<String, String> = emptyMap()

    fun submitList(newList: List<Subcategory>, categoryNamesMap: Map<String, String>) {
        subcategories = newList
        categoryNames = categoryNamesMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubcategoryManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(subcategories[position], categoryNames)
    }

    override fun getItemCount(): Int = subcategories.size

    class ViewHolder(
        private val binding: ItemSubcategoryManagementBinding,
        private val onEditClick: (Subcategory) -> Unit,
        private val onDeleteClick: (Subcategory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subcategory: Subcategory, categoryNames: Map<String, String>) {
            binding.apply {
                tvSubcategoryName.text = subcategory.name
                tvParentCategory.text = categoryNames[subcategory.categoryId] ?: "Categoría"
                tvParentCategory.text = "Categoría: ${categoryNames[subcategory.categoryId] ?: "Desconocida"}"

                btnEdit.setOnClickListener { onEditClick(subcategory) }
                btnDelete.setOnClickListener { onDeleteClick(subcategory) }
            }
        }
    }
}