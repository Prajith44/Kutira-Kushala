package com.example.kutira_kushala

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_product)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            
            // Observe unread count for Messages tab badge
            NotificationRepository.unreadCount.observe(this) { count ->
                val badge = bottomNav.getOrCreateBadge(R.id.nav_notifications)
                if (count > 0) {
                    badge.isVisible = true
                    badge.number = count
                    badge.backgroundColor = Color.parseColor("#1F5D57") // Premium Green
                    badge.badgeTextColor = Color.RED // Red number as requested
                } else {
                    badge.isVisible = false
                }
            }

            // Real-time listener to detect new messages and bulk orders
            db.collection("chats")
                .whereArrayContains("participantIds", uid)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.documentChanges?.forEach { change ->
                        val chat = change.document.toObject(Chat::class.java)
                        val unread = chat.unreadCount[uid] ?: 0
                        NotificationRepository.updateUnreadForChat(chat.chatId, unread)
                    }
                }

            db.collection("notifications")
                .whereEqualTo("receiverId", uid)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.documentChanges?.forEach { change ->
                        val notif = change.document.toObject(Notification::class.java)
                        // Repair: Delivered/Completed orders should not show unread badge
                        if (!notif.isRead && notif.status != OrderStatus.DELIVERED && notif.status != OrderStatus.COMPLETED) {
                            NotificationRepository.updateUnreadForChat("bulk_${notif.id}", 1)
                        } else {
                            NotificationRepository.updateUnreadForChat("bulk_${notif.id}", 0)
                        }
                    }
                }
        }

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_products -> ProductsFragment()
                R.id.nav_notifications -> ChatsFragment() // Now serves as the Messages screen
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }

        fab.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .commit()
    }

    fun loadFragmentWithSharedElement(fragment: Fragment, sharedView: View, transitionName: String, menuId: Int) {
        findViewById<BottomNavigationView>(R.id.bottom_navigation).menu.findItem(menuId).isChecked = true
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedView, transitionName)
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
