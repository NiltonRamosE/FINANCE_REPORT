package com.nramos.finance_report.ui.reminders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.ItemReminderBinding
import com.nramos.finance_report.domain.model.Reminder
import java.text.SimpleDateFormat
import java.util.*

class RemindersAdapter(
    private val onToggleClick: (String, Boolean) -> Unit,
    private val onEditClick: (Reminder) -> Unit,
    private val onDeleteClick: (Reminder) -> Unit
) : RecyclerView.Adapter<RemindersAdapter.ViewHolder>() {

    private var reminders: List<Reminder> = emptyList()
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun submitList(newList: List<Reminder>) {
        reminders = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onToggleClick, onEditClick, onDeleteClick, displayDateFormat)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reminders[position])
    }

    override fun getItemCount(): Int = reminders.size

    class ViewHolder(
        private val binding: ItemReminderBinding,
        private val onToggleClick: (String, Boolean) -> Unit,
        private val onEditClick: (Reminder) -> Unit,
        private val onDeleteClick: (Reminder) -> Unit,
        private val displayDateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isBinding = false
        private var currentReminderId: String = ""

        fun bind(reminder: Reminder) {
            isBinding = true
            currentReminderId = reminder.id

            binding.apply {
                tvTitle.text = reminder.title
                tvDescription.text = reminder.description ?: "Sin descripción"

                val dateString = try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = sdf.parse(reminder.date)
                    displayDateFormat.format(date)
                } catch (e: Exception) {
                    reminder.date
                }
                tvDateTime.text = "$dateString - ${reminder.time}"

                tvFrequency.text = when (reminder.frequency) {
                    "once" -> "Una sola vez"
                    "daily" -> "Diario"
                    "weekly" -> "Semanal"
                    "monthly" -> "Mensual"
                    else -> reminder.frequency
                }

                switchActive.setOnCheckedChangeListener(null)
                switchActive.isChecked = reminder.isActive

                if (reminder.isActive) {
                    ivIcon.setImageResource(R.drawable.ic_notifications_active)
                    ivIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(root.context, R.color.green))
                } else {
                    ivIcon.setImageResource(R.drawable.ic_notifications_off)
                    ivIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(root.context, R.color.gray_dark))
                }

                // Agregar el listener después de configurar el estado
                switchActive.setOnCheckedChangeListener { _, isChecked ->
                    if (!isBinding && isChecked != reminder.isActive) {
                        onToggleClick(currentReminderId, isChecked)
                    }
                }

                root.setOnClickListener { onEditClick(reminder) }
                btnDelete.setOnClickListener { onDeleteClick(reminder) }
            }

            isBinding = false
        }
    }
}