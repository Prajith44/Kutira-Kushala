package com.example.kutira_kushala

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsFragment : Fragment() {

    private lateinit var adapter: NotificationAdapter
    private lateinit var rvNotifications: RecyclerView
    private lateinit var llNoNotifications: LinearLayout
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        
        rvNotifications = view.findViewById(R.id.rv_notifications)
        llNoNotifications = view.findViewById(R.id.ll_no_notifications)
        rvNotifications.layoutManager = LinearLayoutManager(context)
        
        adapter = NotificationAdapter(mutableListOf())
        rvNotifications.adapter = adapter
        
        NotificationRepository.clearNotifications()
        listenForNotifications()
        
        return view
    }

    private fun listenForNotifications() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Only show active (non-delivered) notifications
        db.collection("notifications")
            .whereEqualTo("receiverId", currentUserId)
            .whereIn("status", listOf("REQUESTED", "ACCEPTED", "PROCESSING", "READY"))
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null) {
                    val notifications = snapshot.toObjects(Notification::class.java)
                        .sortedByDescending { it.timestamp } // Manual sort because whereIn with timestamp order might need index
                    adapter.updateList(notifications)
                    
                    llNoNotifications.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }
}
