package com.example.kutira_kushala

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView

class DetailThumbnailAdapter(
    product: ProductItem,
    private val onThumbnailClick: (Any) -> Unit
) : RecyclerView.Adapter<DetailThumbnailAdapter.ThumbnailViewHolder>() {

    private var selectedPosition = 0
    private val images: List<Any> = (product.imageUrls + product.imageUris + product.additionalImages).ifEmpty { listOf(R.drawable.ic_shield) }

    class ThumbnailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ShapeableImageView = view.findViewById(R.id.iv_thumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_thumbnail, parent, false)
        return ThumbnailViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        val source = images[position]
        
        holder.image.load(source) {
            crossfade(true)
            placeholder(R.drawable.ic_shield)
        }
        
        val isSelected = selectedPosition == position
        holder.image.strokeWidth = if (isSelected) 6f else 0f
        holder.image.alpha = if (isSelected) 1.0f else 0.6f
        
        val scale = if (isSelected) 1.1f else 1.0f
        holder.image.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
        
        holder.itemView.setOnClickListener {
            if (selectedPosition != holder.adapterPosition) {
                val oldPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onThumbnailClick(source)
                }
            }
        }
    }

    override fun getItemCount() = images.size
}
