package com.example.kutira_kushala

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ProductAdapter(private val onItemClick: (ProductItem) -> Unit) : ListAdapter<ProductItem, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    class ProductViewHolder(view: View, onClick: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_product_name)
        val price: TextView = view.findViewById(R.id.tv_product_price)
        val availability: TextView = view.findViewById(R.id.tv_availability)
        val image: ImageView = view.findViewById(R.id.iv_product)

        init {
            view.setOnClickListener { 
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view) { position ->
            val product = getItem(position)
            onItemClick(product)
        }
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.name.text = product.name
        holder.price.text = product.price
        holder.availability.visibility = if (product.isAvailable) View.VISIBLE else View.GONE
        
        when {
            product.imageUrls.isNotEmpty() -> {
                holder.image.load(product.imageUrls[0]) {
                    crossfade(true)
                    placeholder(R.drawable.ic_shield)
                    error(R.drawable.ic_shield)
                }
            }
            product.imageUris.isNotEmpty() -> {
                holder.image.load(product.imageUris[0]) {
                    crossfade(true)
                    placeholder(R.drawable.ic_shield)
                }
            }
            product.additionalImages.isNotEmpty() -> {
                holder.image.setImageResource(product.additionalImages[0])
            }
            else -> {
                holder.image.setImageResource(R.drawable.ic_shield)
            }
        }

        // Apply fade-in animation
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_fade_in))
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductItem>() {
        override fun areItemsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean = 
            if (oldItem.productId.isNotEmpty() && newItem.productId.isNotEmpty()) oldItem.productId == newItem.productId
            else oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean = oldItem == newItem
    }
}
