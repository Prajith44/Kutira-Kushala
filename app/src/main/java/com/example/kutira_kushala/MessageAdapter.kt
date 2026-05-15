package com.example.kutira_kushala

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == TYPE_SENT) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(view: View, private val viewType: Int) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = if (viewType == TYPE_SENT) view.findViewById(R.id.tv_message_sent) else view.findViewById(R.id.tv_message_received)
        private val timeText: TextView = if (viewType == TYPE_SENT) view.findViewById(R.id.tv_time_sent) else view.findViewById(R.id.tv_time_received)
        private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun bind(message: Message) {
            messageText.text = message.text
            message.timestamp?.let {
                timeText.text = dateFormat.format(it.toDate())
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.messageId == newItem.messageId
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
    }
}
