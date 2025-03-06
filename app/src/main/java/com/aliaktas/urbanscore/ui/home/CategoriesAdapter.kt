package com.aliaktas.urbanscore.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CategoryModel
import com.aliaktas.urbanscore.databinding.ItemContinentBinding
import com.bumptech.glide.Glide

/**
 * Adapter for displaying city rating categories in a horizontal RecyclerView
 */
class CategoriesAdapter : ListAdapter<CategoryModel, CategoriesAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    var onItemClick: ((CategoryModel) -> Unit)? = null

    // Kıtalar için farklı arka plan drawable listesi
    private val backgroundDrawables = listOf(
        R.drawable.category_landscape_bg,
        R.drawable.category_safety_bg,
        R.drawable.category_livability_bg,
        R.drawable.category_cost_bg,
        R.drawable.category_social_bg
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemContinentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class CategoryViewHolder(
        private val binding: ItemContinentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
        }

        fun bind(category: CategoryModel, position: Int) {
            with(binding) {
                txtContinentName.text = category.title

                // Pozisyona göre farklı arka plan uygula
                val backgroundDrawablePosition = position % backgroundDrawables.size

                // MaterialCardView'in arka planını değiştir
                root.background = ContextCompat.getDrawable(
                    root.context,
                    backgroundDrawables[backgroundDrawablePosition]
                )

                // Load category background image
                // Glide.with(root.context)
                //     .load(category.imageUrl)
                //     .centerCrop()
                //     .into(imgCategoryBackground)
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryModel>() {
        override fun areItemsTheSame(oldItem: CategoryModel, newItem: CategoryModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryModel, newItem: CategoryModel): Boolean {
            return oldItem == newItem
        }
    }
}