package com.example.kutira_kushala

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class ThumbnailAdapter(
    private val images: List<Int>,
    private val onThumbnailClick: (Int) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder>() {

    private var selectedPosition = 0

    class ThumbnailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ShapeableImageView = view.findViewById(R.id.iv_thumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_thumbnail, parent, false)
        return ThumbnailViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        val resId = images[position]
        holder.image.setImageResource(resId)
        
        val isSelected = selectedPosition == position
        
        // Premium selection style
        holder.image.strokeWidth = if (isSelected) 6f else 0f
        holder.image.alpha = if (isSelected) 1.0f else 0.6f
        
        // Subtle scale animation for premium feel
        val scale = if (isSelected) 1.1f else 1.0f
        holder.image.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
        
        holder.itemView.setOnClickListener {
            if (selectedPosition != holder.adapterPosition) {
                val oldPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onThumbnailClick(resId)
                }
            }
        }
    }

    override fun getItemCount() = images.size
}
