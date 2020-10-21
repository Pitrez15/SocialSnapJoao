package com.example.socialsnap.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.socialsnap.R
import com.example.socialsnap.models.ChatMessage
import com.example.socialsnap.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*

class ChatLogActivity : AppCompatActivity() {

    companion object {

        val TAG = "ChatLog"
    }

    var message : ChatMessage? = null
    val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = user.username

        recyclerViewChatLog.adapter = adapter

        listenForMessages()

        buttonSendMessage.setOnClickListener {

            performSendMessage()
        }
    }

    private fun listenForMessages() {

        val reference = FirebaseFirestore.getInstance().collection("messages")

        reference.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

            adapter.clear()

            if (querySnapshot != null) {

                for (d in querySnapshot) {

                    message = ChatMessage(
                        d.data.getValue("text").toString(),
                        d.data.getValue("fromId").toString(),
                        d.data.getValue("toId").toString(),
                        d.data.getValue("timeStamp") as Long)

                    if (message!!.fromId == FirebaseAuth.getInstance().uid) {

                        adapter.add(ChatFromItem(message!!.text.toString()))
                    }
                    else {

                        adapter.add(ChatToItem(message!!.text.toString()))
                    }
                }
            }
        }

        /*val ref = FirebaseDatabase.getInstance().getReference("/messages")

        ref.addChildEventListener(object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                val chatMessage = snapshot.getValue(ChatMessage::class.java)

                if (chatMessage != null) {

                    Log.d(TAG, chatMessage.text)

                    adapter.add(ChatFromItem(chatMessage.text))
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })*/
    }

    private fun performSendMessage() {

        val text = editTextTextSendMessage.text.toString()

        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user.uid

        val reference = FirebaseFirestore.getInstance().collection("messages")
        val chatMessage = ChatMessage(text, fromId!!, toId!!, System.currentTimeMillis() / 1000)

        reference.add(chatMessage).addOnSuccessListener {

        }
    }
}

class ChatFromItem (val text : String) : Item<GroupieViewHolder>(){

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        viewHolder.itemView.textViewChatFrom.text = text
    }

    override fun getLayout(): Int {

        return R.layout.chat_from_row
    }
}

class ChatToItem (val text : String) : Item<GroupieViewHolder>(){

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        viewHolder.itemView.textViewChatTo.text = text
    }

    override fun getLayout(): Int {

        return R.layout.chat_to_row
    }
}