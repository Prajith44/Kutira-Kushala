package com.example.kutira_kushala

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SuggestionAdapter(
    private var suggestions: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSuggestion: TextView = view.findViewById(R.id.tv_suggestion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.tvSuggestion.text = suggestion
        holder.itemView.setOnClickListener { onClick(suggestion) }
    }

    override fun getItemCount() = suggestions.size

    fun updateData(newSuggestions: List<String>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }
}
