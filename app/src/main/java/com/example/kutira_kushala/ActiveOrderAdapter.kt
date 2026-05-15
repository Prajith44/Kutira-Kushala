package com.example.kutira_kushala

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActiveOrderAdapter(
    private val orders: List<Notification>,
    private val onOrderClick: (Notification) -> Unit
) : RecyclerView.Adapter<ActiveOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val buyer: TextView = view.findViewById(R.id.tv_card_buyer)
        val product: TextView = view.findViewById(R.id.tv_card_product)
        val status: TextView = view.findViewById(R.id.tv_card_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_active_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        holder.buyer.text = order.buyerName
        holder.product.text = "${order.quantity} x ${order.productName}"
        holder.status.text = order.status.displayName.uppercase()
        
        holder.itemView.setOnClickListener { onOrderClick(order) }
    }

    override fun getItemCount() = orders.size
}
