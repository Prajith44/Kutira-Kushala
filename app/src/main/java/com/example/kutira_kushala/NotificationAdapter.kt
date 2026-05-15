package com.example.kutira_kushala

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(private var notifications: MutableList<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: ConstraintLayout = view.findViewById(R.id.cl_notification_root)
        val message: TextView = view.findViewById(R.id.tv_notification_msg)
        val time: TextView = view.findViewById(R.id.tv_notification_time)
        val unreadDot: ImageView = view.findViewById(R.id.iv_notif_unread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        
        val displayMsg = "${notification.senderName} requested ${notification.quantity} units of ${notification.productName}"
        holder.message.text = displayMsg
        
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.time.text = if (notification.timestamp != null) {
            dateFormat.format(notification.timestamp.toDate())
        } else {
            "Just now"
        }
        
        // Repair: WhatsApp-like unread indicator and styling
        if (!notification.isRead) {
            holder.root.setBackgroundColor(Color.parseColor("#F0FDF4")) // Light green for unread
            holder.message.setTypeface(null, Typeface.BOLD)
            holder.unreadDot.visibility = View.VISIBLE
        } else {
            holder.root.setBackgroundColor(Color.WHITE)
            holder.message.setTypeface(null, Typeface.NORMAL)
            holder.unreadDot.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            // Update local state for immediate feedback
            notification.isRead = true
            notifyItemChanged(position)

            val intent = if (notification.status == OrderStatus.REQUESTED) {
                Intent(context, BulkOrderDetailActivity::class.java)
            } else {
                Intent(context, OrderTrackingActivity::class.java)
            }
            intent.putExtra("NOTIFICATION_ID", notification.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateList(newList: List<Notification>) {
        notifications.clear()
        notifications.addAll(newList)
        notifyDataSetChanged()
    }
}
