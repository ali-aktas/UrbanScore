package com.aliaktas.urbanscore.ui.home.controllers

import android.util.Log
import android.view.View
import com.aliaktas.urbanscore.data.model.CategoryModel
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.CategoriesAdapter
import com.aliaktas.urbanscore.ui.home.HomeState

/**
 * Kategori listesini yöneten controller
 */
class CategoriesController(
    private val binding: FragmentHomeBinding,
    private val onCategoryClick: (CategoryModel) -> Unit,
    private val checkInternetBeforeClick: () -> Boolean
) : HomeController {

    private val categoriesAdapter = CategoriesAdapter()

    override fun bind(view: View) {
        Log.d(TAG, "Setting up categories RecyclerView")
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCategories.apply {
            adapter = categoriesAdapter
            setHasFixedSize(true)
            itemAnimator = null // Animasyonu devre dışı bırak
        }

        // Statik kategori listesini ayarla
        categoriesAdapter.submitList(CategoryModel.getDefaultCategories())

        // Tıklama olayını ayarla - internet kontrolü ile
        categoriesAdapter.onItemClick = { category ->
            if (checkInternetBeforeClick()) {
                Log.d(TAG, "Category clicked: ${category.title}")
                onCategoryClick(category)
            }
        }
    }

    override fun update(state: HomeState) {
        // Kategoriler statik olduğu için duruma göre güncelleme gerekmez
    }

    companion object {
        private const val TAG = "CategoriesController"
    }
}