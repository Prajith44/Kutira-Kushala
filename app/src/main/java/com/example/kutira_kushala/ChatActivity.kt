package com.example.kutira_kushala

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var adapter: MessageAdapter
    
    private var chatId: String? = null
    private var recipientId: String? = null
    private var productId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recipientId = intent.getStringExtra("RECIPIENT_ID")
        productId = intent.getStringExtra("PRODUCT_ID")
        chatId = intent.getStringExtra("CHAT_ID")

        if (recipientId == null && chatId == null) {
            finish()
            return
        }

        initViews()
        setupChat()
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rv_messages)
        etInput = findViewById(R.id.et_message_input)
        btnSend = findViewById(R.id.btn_send_message)
        
        adapter = MessageAdapter()
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        findViewById<View>(R.id.btn_chat_back).setOnClickListener { finish() }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    private fun setupChat() {
        if (chatId == null && recipientId != null && currentUserId != null) {
            // Check if chat already exists
            db.collection("chats")
                .whereArrayContains("participantIds", currentUserId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val existingChat = snapshot.documents.find { doc ->
                        val participants = doc.get("participantIds") as? List<*>
                        participants?.contains(recipientId) == true
                    }
                    
                    if (existingChat != null) {
                        chatId = existingChat.id
                        listenForMessages()
                    } else {
                        // Brand new chat
                        loadRecipientInfo()
                    }
                }
        } else if (chatId != null) {
            listenForMessages()
            loadChatMetadata()
        }
        
        recipientId?.let { loadRecipientInfo() }
    }

    private fun loadRecipientInfo() {
        recipientId?.let { id ->
            db.collection("sellers").document(id).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("businessName") ?: doc.getString("sellerName") ?: "Artisan"
                        val image = doc.getString("profileImageUrl") ?: ""
                        findViewById<TextView>(R.id.tv_chat_name).text = name
                        findViewById<ImageView>(R.id.iv_chat_recipient).load(image) {
                            placeholder(R.drawable.ic_person)
                        }
                    } else {
                        db.collection("users").document(id).get().addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("fullName") ?: "Buyer"
                            findViewById<TextView>(R.id.tv_chat_name).text = name
                            findViewById<ImageView>(R.id.iv_chat_recipient).load(R.drawable.ic_person)
                        }
                    }
                }
        }
    }

    private fun loadChatMetadata() {
        chatId?.let { id ->
            db.collection("chats").document(id).get()
                .addOnSuccessListener { doc ->
                    val chat = doc.toObject(Chat::class.java)
                    chat?.let {
                        recipientId = if (it.buyerId == currentUserId) it.sellerId else it.buyerId
                        loadRecipientInfo()
                        if (it.productImage.isNotEmpty()) {
                            val ivProduct = findViewById<ImageView>(R.id.iv_chat_product)
                            ivProduct.visibility = View.VISIBLE
                            ivProduct.load(it.productImage)
                        }
                    }
                }
        }
    }

    private fun listenForMessages() {
        chatId?.let { id ->
            db.collection("chats").document(id).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    snapshot?.let {
                        val messages = it.toObjects(Message::class.java)
                        adapter.submitList(messages) {
                            rvMessages.scrollToPosition(messages.size - 1)
                        }
                    }
                }
        }
    }

    private fun sendMessage(text: String) {
        val uid = currentUserId ?: return
        val rid = recipientId ?: return
        
        if (chatId == null) {
            // Create new chat document
            val newChatId = db.collection("chats").document().id
            val participants = listOf(uid, rid).sorted()
            
            // Try to get product info if available
            if (productId != null) {
                db.collection("products").document(productId!!).get().addOnSuccessListener { doc ->
                    val prodName = doc.getString("name") ?: ""
                    val images = doc.get("imageUrls") as? List<*>
                    val prodImage = images?.firstOrNull()?.toString() ?: ""
                    
                    createNewChat(newChatId, uid, rid, text, participants, prodName, prodImage)
                }
            } else {
                createNewChat(newChatId, uid, rid, text, participants, "", "")
            }
        } else {
            val message = Message(
                messageId = db.collection("chats").document(chatId!!).collection("messages").document().id,
                senderId = uid,
                receiverId = rid,
                text = text,
                timestamp = Timestamp.now()
            )
            
            db.collection("chats").document(chatId!!).collection("messages").document(message.messageId)
                .set(message)
            
            db.collection("chats").document(chatId!!).update(
                "lastMessage", text,
                "lastTimestamp", Timestamp.now()
            )
            
            etInput.setText("")
        }
    }

    private fun createNewChat(id: String, uid: String, rid: String, text: String, participants: List<String>, pName: String, pImg: String) {
        val chat = Chat(
            chatId = id,
            buyerId = uid,
            sellerId = rid,
            participantIds = participants,
            lastMessage = text,
            lastTimestamp = Timestamp.now(),
            productName = pName,
            productImage = pImg
        )

        db.collection("chats").document(id).set(chat).addOnSuccessListener {
            chatId = id
            listenForMessages()
            sendMessage(text) // Send the actual message now that chat doc exists
        }
    }
}
