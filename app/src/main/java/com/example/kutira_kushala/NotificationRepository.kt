package com.example.kutira_kushala

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Singleton state manager for frontend unread badge simulation.
 */
object NotificationRepository {
    private val _unreadCount = MutableLiveData<Int>(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val unreadMap = mutableMapOf<String, Int>()

    fun updateUnreadForChat(chatId: String, count: Int) {
        if (count <= 0) {
            unreadMap.remove(chatId)
        } else {
            unreadMap[chatId] = count
        }
        _unreadCount.postValue(unreadMap.values.sum())
    }

    fun clearMessages() {
        // Clear all regular chats unread counts
        val keysToRemove = unreadMap.keys.filter { !it.startsWith("bulk_") }
        keysToRemove.forEach { unreadMap.remove(it) }
        _unreadCount.postValue(unreadMap.values.sum())
    }

    fun clearNotifications() {
        // Clear all bulk order unread counts
        val keysToRemove = unreadMap.keys.filter { it.startsWith("bulk_") }
        keysToRemove.forEach { unreadMap.remove(it) }
        _unreadCount.postValue(unreadMap.values.sum())
    }

    fun clearAll() {
        unreadMap.clear()
        _unreadCount.postValue(0)
    }

    fun getUnreadForChat(chatId: String): Int = unreadMap[chatId] ?: 0
}
