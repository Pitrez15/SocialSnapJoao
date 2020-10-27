package com.example.socialsnap.ui.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.socialsnap.R
import com.example.socialsnap.models.ChatMessage
import com.example.socialsnap.models.User
import com.example.socialsnap.ui.home.NewMessageActivity.Companion.USER_KEY
import com.example.socialsnap.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.latest_message_row.view.*

class LatestMessagesActivity : AppCompatActivity() {

    companion object {

        var currentUser : User? = null
    }

    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        recyclerViewLatestMessages.adapter = adapter
        recyclerViewLatestMessages.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        adapter.setOnItemClickListener { item, view ->

            val intent = Intent(this, ChatLogActivity::class.java)
            val row = item as LatestMessageRow
            intent.putExtra(USER_KEY, row.chatPartnerUser)
            startActivity(intent)
        }

        listenForLatestMessages()

        fetchCurrentUser()
    }

    class LatestMessageRow (val chatMessage: ChatMessage) : Item<GroupieViewHolder>() {

        var chatPartnerUser : User? = null

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            viewHolder.itemView.textViewLatestMessage.text = chatMessage.text

            val chatPartnerId : String
            if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {

                chatPartnerId = chatMessage.toId.toString()
            }
            else {

                chatPartnerId = chatMessage.fromId.toString()
            }

            val reference = FirebaseFirestore.getInstance()
                                            .collection("users_chat")
                                            .document(chatPartnerId)

            reference.addSnapshotListener { querySnapshot, e ->

                if (querySnapshot != null) {

                    chatPartnerUser = User(
                        querySnapshot.data?.getValue("uid").toString(),
                        querySnapshot.data?.getValue("username").toString(),
                        querySnapshot.data?.getValue("profileImageUrl").toString(),
                        querySnapshot.data?.getValue("email").toString(),
                        querySnapshot.data?.getValue("token").toString())

                    viewHolder.itemView.textViewLatestUsername.text = chatPartnerUser!!.username

                    Picasso.get().load(chatPartnerUser!!.profileImageUrl).into(viewHolder.itemView.imageViewLatestUser)
                }
            }
        }

        override fun getLayout(): Int {

            return R.layout.latest_message_row
        }
    }

    private fun listenForLatestMessages() {

        val fromId = FirebaseAuth.getInstance().uid
        val ref = FirebaseFirestore.getInstance()
                            .collection("latest_messages")
                            .document(fromId.toString())
                            .collection("latest_message")
                            .orderBy("timeStamp", Query.Direction.DESCENDING)

        ref.addSnapshotListener { querySnapshot, e ->

            if (querySnapshot != null) {

                for (d in querySnapshot) {

                    val chatMessage = ChatMessage(
                        d.data.getValue("text").toString(),
                        d.data.getValue("fromId").toString(),
                        d.data.getValue("toId").toString(),
                        d.data.getValue("timeStamp") as Long
                    )
                    adapter.add(LatestMessageRow(chatMessage))
                }
            }
        }
    }

    private fun fetchCurrentUser() {

        val uid = FirebaseAuth.getInstance().uid

        val reference = FirebaseFirestore.getInstance()
                                            .collection("users_chat")
                                            .document(uid.toString())



        reference.addSnapshotListener { querySnapshot, e ->

            if (querySnapshot != null) {

                currentUser = User(
                    querySnapshot.data?.getValue("uid").toString(),
                    querySnapshot.data?.getValue("username").toString(),
                    querySnapshot.data?.getValue("profileImageUrl").toString(),
                    querySnapshot.data?.getValue("email").toString(),
                    querySnapshot.data?.getValue("token").toString()
                )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.menuChatNewMessage -> {

                val intent = Intent(this, NewMessageActivity::class.java)
                startActivity(intent)
            }
            R.id.menuChatSignOut -> {

                Firebase.auth.signOut()

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.chat_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}