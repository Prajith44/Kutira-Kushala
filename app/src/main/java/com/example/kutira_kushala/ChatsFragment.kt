package com.example.kutira_kushala

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatsFragment : Fragment() {

    private lateinit var rvChats: RecyclerView
    private lateinit var llNoChats: LinearLayout
    private lateinit var adapter: ConversationAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    private var firebaseChats = listOf<Chat>()
    private var bulkOrdersAsChats = listOf<Chat>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        
        rvChats = view.findViewById(R.id.rv_chats_list)
        llNoChats = view.findViewById(R.id.ll_no_chats)
        
        adapter = ConversationAdapter { chat ->
            // Repair: Mark as read locally and update badge
            NotificationRepository.updateUnreadForChat(chat.chatId, 0)
            
            if (chat.chatId.startsWith("bulk_")) {
                val intent = Intent(requireContext(), BulkOrderDetailActivity::class.java)
                intent.putExtra("NOTIFICATION_ID", chat.chatId.removePrefix("bulk_"))
                startActivity(intent)
            } else {
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("CHAT_ID", chat.chatId)
                startActivity(intent)
            }
        }
        rvChats.adapter = adapter
        
        // WhatsApp-like: Clear general count when viewing list
        NotificationRepository.clearMessages()
        
        loadAllMessages()
        
        return view
    }

    private fun loadAllMessages() {
        val uid = currentUserId ?: return
        
        // 1. Load regular chats with real-time updates
        db.collection("chats")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, _ ->
                firebaseChats = snapshot?.toObjects(Chat::class.java) ?: emptyList()
                mergeAndDisplay()
            }
            
        // 2. Load Bulk Orders (Notifications) as messages
        db.collection("notifications")
            .whereEqualTo("receiverId", uid)
            .addSnapshotListener { snapshot, _ ->
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                
                // Repair: Filter out completed/delivered orders from active messages
                bulkOrdersAsChats = notifications
                    .filter { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.COMPLETED }
                    .map { notif ->
                        val isUnread = !notif.isRead
                        Chat(
                            chatId = "bulk_${notif.id}",
                            buyerId = notif.senderId,
                            sellerId = notif.receiverId,
                            lastMessage = "Bulk Order Request: ${notif.productName}",
                            lastTimestamp = notif.timestamp,
                            productName = notif.productName,
                            productImage = notif.productImage,
                            participantIds = listOf(notif.senderId, notif.receiverId),
                            unreadCount = if (isUnread) mapOf(uid to 1) else emptyMap()
                        )
                    }
                mergeAndDisplay()
            }
    }

    private fun mergeAndDisplay() {
        val all = (firebaseChats + bulkOrdersAsChats).sortedByDescending { it.lastTimestamp }
        adapter.submitList(all)
        llNoChats.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
    }
}
