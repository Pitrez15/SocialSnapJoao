package com.example.socialsnap.ui.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.example.socialsnap.R
import com.example.socialsnap.models.ChatMessage
import com.example.socialsnap.models.User
import com.example.socialsnap.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_latest_messages.*

class LatestMessagesActivity : AppCompatActivity() {

    companion object {

        var currentUser : User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        setUpDummyRows()

        fetchCurrentUser()
    }

    class latestMessageRow : Item<GroupieViewHolder>() {

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        }

        override fun getLayout(): Int {

            return R.layout.latest_message_row
        }


    }

    private fun setUpDummyRows() {

        val adapter = GroupAdapter<GroupieViewHolder>()

        adapter.add(latestMessageRow())
        adapter.add(latestMessageRow())
        adapter.add(latestMessageRow())

        recyclerViewLatestMessages.adapter = adapter
    }

    private fun fetchCurrentUser() {

        val uid = FirebaseAuth.getInstance().uid

        val reference = FirebaseFirestore.getInstance().collection("users_chat").document(uid.toString())

        reference.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

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