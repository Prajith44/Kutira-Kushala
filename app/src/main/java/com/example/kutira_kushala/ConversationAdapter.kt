package com.example.kutira_kushala

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ConversationAdapter(private val onClick: (Chat) -> Unit) : ListAdapter<Chat, ConversationAdapter.ChatViewHolder>(ChatDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ChatViewHolder(view, onClick, currentUserId)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(view: View, val onClick: (Chat) -> Unit, private val currentUserId: String?) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.tv_conversation_name)
        private val lastMessageText: TextView = view.findViewById(R.id.tv_conversation_last_message)
        private val timeText: TextView = view.findViewById(R.id.tv_conversation_time)
        private val userImage: ImageView = view.findViewById(R.id.iv_conversation_user)
        private val unreadBadge: TextView = view.findViewById(R.id.tv_unread_badge)
        private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun bind(chat: Chat) {
            val otherUserId = if (chat.buyerId == currentUserId) chat.sellerId else chat.buyerId
            
            FirebaseFirestore.getInstance().collection("sellers").document(otherUserId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("businessName") ?: doc.getString("sellerName") ?: "Artisan"
                        val image = doc.getString("profileImageUrl") ?: ""
                        nameText.text = name
                        userImage.load(image) { placeholder(R.drawable.ic_person) }
                    } else {
                        FirebaseFirestore.getInstance().collection("users").document(otherUserId).get()
                            .addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("fullName") ?: "Buyer"
                                nameText.text = name
                                userImage.load(R.drawable.ic_person)
                            }
                    }
                }

            lastMessageText.text = chat.lastMessage
            chat.lastTimestamp?.let {
                timeText.text = dateFormat.format(it.toDate())
            }

            // WhatsApp-style item badge
            val unreadCount = chat.unreadCount[currentUserId] ?: 0
            if (unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = unreadCount.toString()
                unreadBadge.setTextColor(Color.RED) // Red text as requested
                unreadBadge.setBackgroundResource(R.drawable.circle_green) // Green circular bg
                lastMessageText.setTypeface(null, Typeface.BOLD)
                timeText.setTextColor(Color.parseColor("#1F5D57"))
            } else {
                unreadBadge.visibility = View.GONE
                lastMessageText.setTypeface(null, Typeface.NORMAL)
                timeText.setTextColor(Color.parseColor("#9CA3AF"))
            }

            itemView.setOnClickListener { onClick(chat) }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem.chatId == newItem.chatId
        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem == newItem
    }
}
