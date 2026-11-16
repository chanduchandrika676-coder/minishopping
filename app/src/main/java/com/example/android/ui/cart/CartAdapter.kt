package com.example.android.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.android.data.local.CartItemEntity
import com.example.android.databinding.ItemCartBinding

class CartAdapter(
    private val onQuantityChanged: (Int, Int) -> Unit, // productId, newQuantity
    private val onItemRemoved: (Int) -> Unit            // productId
) : ListAdapter<CartItemEntity, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItemEntity) {
            binding.tvCartTitle.text = item.title
            val discountedPrice = item.price * (1 - item.discountPercentage / 100.0)
            binding.tvCartPrice.text = "$${String.format("%.2f", discountedPrice)}"
            binding.tvCartQuantity.text = item.quantity.toString()

            Glide.with(binding.ivCartThumbnail.context)
                .load(item.thumbnail)
                .centerCrop()
                .into(binding.ivCartThumbnail)

            binding.btnPlus.setOnClickListener {
                onQuantityChanged(item.productId, item.quantity + 1)
            }

            binding.btnMinus.setOnClickListener {
                onQuantityChanged(item.productId, item.quantity - 1)
            }

            binding.btnRemove.setOnClickListener {
                onItemRemoved(item.productId)
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItemEntity>() {
        override fun areItemsTheSame(oldItem: CartItemEntity, newItem: CartItemEntity): Boolean =
            oldItem.productId == newItem.productId

        override fun areContentsTheSame(oldItem: CartItemEntity, newItem: CartItemEntity): Boolean =
            oldItem == newItem
    }
}
