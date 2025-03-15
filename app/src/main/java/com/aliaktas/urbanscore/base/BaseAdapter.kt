package com.aliaktas.urbanscore.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T : Any, VB : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseAdapter.BaseViewHolder<VB>>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = createBinding(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        val item = getItem(position)
        bind(holder.binding, item, position)
    }

    abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean): VB

    abstract fun bind(binding: VB, item: T, position: Int)

    class BaseViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)
}