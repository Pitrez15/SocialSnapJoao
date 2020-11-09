package com.example.socialsnap.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.socialsnap.R
import com.example.socialsnap.models.ChatMessage
import com.example.socialsnap.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_image_row.view.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_image_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import java.util.*

class ChatLogActivity : AppCompatActivity() {

    companion object {

        val TAG = "ChatLog"
    }

    var message : ChatMessage? = null
    val adapter = GroupAdapter<GroupieViewHolder>()

    var selectedPhotoUri : Uri? = null

    var toUser : User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username

        recyclerViewChatLog.adapter = adapter

        listenForMessages()

        buttonSendMessage.setOnClickListener {

            performSendMessage()
        }

        buttonSendImage.setOnClickListener {

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"

            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {

            selectedPhotoUri = data.data

            uploadImageToFirebaseStorage()
        }
    }

    private fun listenForMessages() {

        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid

        val reference = FirebaseFirestore.getInstance()
                                .collection("user_messages")
                                .document(fromId.toString())
                                .collection(toId.toString())
                                .orderBy("timeStamp")

        reference.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

            adapter.clear()

            if (querySnapshot != null) {

                for (d in querySnapshot) {

                    message = ChatMessage(
                        d.data.getValue("text").toString(),
                        d.data.getValue("fromId").toString(),
                        d.data.getValue("toId").toString(),
                        d.data.getValue("timeStamp") as Long,
                        d.data.getValue("messageType").toString())

                    if (message!!.fromId == FirebaseAuth.getInstance().uid) {

                        val currentUser = LatestMessagesActivity.currentUser

                        if(message!!.messageType.toString() == "text") {

                            adapter.add(ChatToItem(message!!.text.toString(), currentUser!!))
                        }
                        else if (message!!.messageType.toString() == "image"){

                            adapter.add(ChatToImageItem(message!!.text.toString(), currentUser!!))
                        }
                    }
                    else {

                        if(message!!.messageType.toString() == "text") {

                            adapter.add(ChatFromItem(message!!.text.toString(), toUser!!))
                        }
                        else if (message!!.messageType.toString() == "image") {

                            adapter.add(ChatFromImageItem(message!!.text.toString(), toUser!!))
                        }
                    }
                }
            }
            recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun performSendMessage() {

        val text = editTextTextSendMessage.text.toString()
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid

        val reference = FirebaseFirestore.getInstance()
                                        .collection("user_messages")
                                        .document(fromId.toString())
                                        .collection(toId.toString())
        val toReference = FirebaseFirestore.getInstance()
                                        .collection("user_messages")
                                        .document(toId.toString())
                                        .collection(fromId.toString())

        val chatMessage = ChatMessage(text, fromId!!, toId!!, System.currentTimeMillis() / 1000, "text")

        reference.add(chatMessage).addOnSuccessListener {

            editTextTextSendMessage.text.clear()
            recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
        }
        toReference.add(chatMessage)

        val latestMessageRef = FirebaseFirestore.getInstance()
                                                .collection("latest_messages")
                                                .document(fromId.toString())
                                                .collection("latest_message")
                                                .document(toId.toString())
        latestMessageRef.set(chatMessage)

        val latestMessageToRef = FirebaseFirestore.getInstance()
                                                .collection("latest_messages")
                                                .document(toId.toString())
                                                .collection("latest_message")
                                                .document(fromId.toString())
        latestMessageToRef.set(chatMessage)
    }

    private fun uploadImageToFirebaseStorage() {

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {

                ref.downloadUrl.addOnSuccessListener {

                    performSendImageMessage(it.toString())
                }
            }
            .addOnFailureListener {

            }
    }

    private fun performSendImageMessage(imageUrl : String) {

        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid

        val reference = FirebaseFirestore.getInstance()
                                        .collection("user_messages")
                                        .document(fromId.toString())
                                        .collection(toId.toString())
        val toReference = FirebaseFirestore.getInstance()
                                        .collection("user_messages")
                                        .document(toId.toString())
                                        .collection(fromId.toString())

        val chatMessage = ChatMessage(imageUrl, fromId!!, toId!!, System.currentTimeMillis() / 1000, "image")

        reference.add(chatMessage).addOnSuccessListener {

            editTextTextSendMessage.text.clear()
            recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
        }
        toReference.add(chatMessage)

        val latestMessageRef = FirebaseFirestore.getInstance()
                                                .collection("latest_messages")
                                                .document(fromId.toString())
                                                .collection("latest_message")
                                                .document(toId.toString())
        latestMessageRef.set(chatMessage)

        val latestMessageToRef = FirebaseFirestore.getInstance()
                                                .collection("latest_messages")
                                                .document(toId.toString())
                                                .collection("latest_message")
                                                .document(fromId.toString())
        latestMessageToRef.set(chatMessage)
    }
}

class ChatFromItem (val text : String, val user : User) : Item<GroupieViewHolder>(){

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        viewHolder.itemView.textViewChatFrom.text = text

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageViewChatFrom

        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {

        return R.layout.chat_from_row
    }
}

class ChatToItem (val text : String, val user : User) : Item<GroupieViewHolder>(){

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        viewHolder.itemView.textViewChatTo.text = text

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageViewChatTo

        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {

        return R.layout.chat_to_row
    }
}

class ChatFromImageItem (val text : String, val user : User) : Item<GroupieViewHolder>(){

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        val targetImageMessage = viewHolder.itemView.imageViewImageMessageChatFrom

        Picasso.get().load(text).into(targetImageMessage)

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageViewImageUserChatFrom

        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {

        return R.layout.chat_from_image_row
    }
}

class ChatToImageItem (val text : String, val user : User) : Item<GroupieViewHolder>(){

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        val targetImageMessage = viewHolder.itemView.imageViewImageMessageChatTo

        Picasso.get().load(text).into(targetImageMessage)

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageViewImageUserChatTo

        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {

        return R.layout.chat_to_image_row
    }
}