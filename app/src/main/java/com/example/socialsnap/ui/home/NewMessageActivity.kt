package com.example.socialsnap.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.socialsnap.R
import com.example.socialsnap.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*

class NewMessageActivity : AppCompatActivity() {

    //var users : MutableList<User> = ArrayList<User>()

    var user : User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        supportActionBar?.title = "Select User"

        //val adapter = GroupAdapter<GroupieViewHolder>()

        //recyclerViewNewMessage.adapter = adapter

        fetchUsers()
    }

    private fun fetchUsers() {

        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users_chat")

        ref.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

            val adapter = GroupAdapter<GroupieViewHolder>()
            if (querySnapshot != null) {

                for(d in querySnapshot){

                    //users.add(User(d.data.getValue("uid").toString(),
                                    //d.data.getValue("username").toString(),
                                    //d.data.getValue("profileImageUrl").toString()))

                    user = User(d.data.getValue("uid").toString(),
                                d.data.getValue("username").toString(),
                                d.data.getValue("profileImageUrl").toString())

                    adapter.add(UserItem(user!!))
                }
            }
            recyclerViewNewMessage.adapter = adapter
        }

        /*ref.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val adapter = GroupAdapter<GroupieViewHolder>()

                snapshot.children.forEach {

                    val user = it.getValue(User::class.java)

                    if (user != null) {

                        adapter.add(UserItem(user))
                    }
                }
                recyclerViewNewMessage.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })*/
    }
}

class UserItem(private val user : User) : Item<GroupieViewHolder>() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        viewHolder.itemView.textViewChatNewMessageUsername.text = user.username

        Picasso.get().load(user.profileImageUrl).into(viewHolder.itemView.imageViewChatNewMessage)
    }

    override fun getLayout(): Int {

        return R.layout.user_row_new_message
    }

}