package com.example.socialsnap.ui.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.socialsnap.R
import com.example.socialsnap.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*

class NewMessageActivity : AppCompatActivity() {

    var user : User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        supportActionBar?.title = "Select User"

        fetchUsers()
    }

    companion object {

        val USER_KEY = "USER_KEY"
    }

    private fun fetchUsers() {

        val fromId = FirebaseAuth.getInstance().uid

        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users_chat")

        ref.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

            val adapter = GroupAdapter<GroupieViewHolder>()
            if (querySnapshot != null) {

                for(d in querySnapshot){

                    user = User(d.data.getValue("uid").toString(),
                                d.data.getValue("username").toString(),
                                d.data.getValue("profileImageUrl").toString())

                    if (user!!.uid != fromId) {

                        adapter.add(UserItem(user!!))
                    }
                }
            }

            adapter.setOnItemClickListener { item, view ->

                val userItem = item as UserItem

                val intent = Intent(view.context, ChatLogActivity::class.java)
                intent.putExtra(USER_KEY, userItem.user)
                startActivity(intent)

                finish()
            }

            recyclerViewNewMessage.adapter = adapter
        }
    }
}

class UserItem(val user : User) : Item<GroupieViewHolder>() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        viewHolder.itemView.textViewChatNewMessageUsername.text = user.username

        Picasso.get().load(user.profileImageUrl).into(viewHolder.itemView.imageViewChatNewMessage)
    }

    override fun getLayout(): Int {

        return R.layout.user_row_new_message
    }

}