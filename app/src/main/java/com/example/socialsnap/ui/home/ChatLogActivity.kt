package com.example.socialsnap.ui.home

import android.Manifest
import android.app.Activity
import android.content.ContentProvider
import android.content.ContentResolver
import android.view.View
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.transition.TransitionManager
import android.util.Log
import android.webkit.URLUtil
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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
import kotlinx.android.synthetic.main.chat_from_voice_row.view.*
import kotlinx.android.synthetic.main.chat_to_image_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import kotlinx.android.synthetic.main.chat_to_voice_row.view.*
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.URL
import java.util.*

@RequiresApi(api = Build.VERSION_CODES.M)
class ChatLogActivity : AppCompatActivity() {

    companion object {

        val TAG = "ChatLog"
    }

    var message : ChatMessage? = null
    val adapter = GroupAdapter<GroupieViewHolder>()

    var selectedPhotoUri : Uri? = null
    private var mRecorder : MediaRecorder? = null
    private val RECORD_AUDIO_REQUEST_CODE = 101
    private var audioName : String? = null
    private var audioUri : String? = null
    private var audioDir : String? = null

    var toUser : User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio()
        }

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

        buttonSendVoice.setOnClickListener {


            buttonSendVoice.visibility = View.INVISIBLE
            buttonStopVoice.visibility = View.VISIBLE
            startRecording()
        }

        buttonStopVoice.setOnClickListener {


            buttonSendVoice.visibility = View.VISIBLE
            buttonStopVoice.visibility = View.INVISIBLE
            stopRecording()
            uploadAudioToFirebaseStorage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {

            selectedPhotoUri = data.data

            uploadImageToFirebaseStorage()
        }
    }

    private fun startRecording() {

        mRecorder = MediaRecorder()
        mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
        mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

        val root = android.os.Environment.getExternalStorageDirectory()
        val file = File(root.absolutePath + "/AndroidCodility/Audios")

        if (!file.exists()) {

            file.mkdirs()
        }

        audioName = (System.currentTimeMillis().toString() + ".mp3")
        audioDir = root.absolutePath + "/AndroidCodility/Audios/" +  audioName
        mRecorder!!.setOutputFile(audioDir)
        audioUri = "content://com.example.socialsnap/files/" + audioName

        try {

            mRecorder!!.prepare()
            mRecorder!!.start()
        }
        catch (e: IOException) {

            e.printStackTrace()
        }
    }

    private fun stopRecording() {

        try {

            mRecorder!!.stop()
            mRecorder!!.release()
        }
        catch (e: Exception) {

            e.printStackTrace()
        }

        mRecorder = null
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
                        else if (message!!.messageType.toString() == "audio") {

                            adapter.add(ChatToAudioItem(message!!.text.toString(), currentUser!!))
                        }
                    }
                    else {

                        if(message!!.messageType.toString() == "text") {

                            adapter.add(ChatFromItem(message!!.text.toString(), toUser!!))
                        }
                        else if (message!!.messageType.toString() == "image") {

                            adapter.add(ChatFromImageItem(message!!.text.toString(), toUser!!))
                        }
                        else if (message!!.messageType.toString() == "audio") {

                            adapter.add(ChatFromAudioItem(message!!.text.toString(), toUser!!))
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

    private fun uploadAudioToFirebaseStorage() {

        val file = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/audios/$file")
        val selectedAudioUri = Uri.fromFile(File(audioDir!!))

        ref.putFile(selectedAudioUri)
            .addOnSuccessListener {

                ref.downloadUrl.addOnSuccessListener {

                    performSendAudioMessage(it.toString())
                }
            }
            .addOnFailureListener {

            }
    }

    private fun performSendAudioMessage(audioUrl : String) {

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

        val chatMessage = ChatMessage(audioUrl, fromId!!, toId!!, System.currentTimeMillis() / 1000, "audio")

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


    private fun getPermissionToRecordAudio() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid checking the build version since Context.checkSelfPermission(...) is only available in Marshmallow
        // 2) Always check for permission (even if permission has already been granted) since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
                RECORD_AUDIO_REQUEST_CODE)
        }
    }

    // Callback with the request from calling requestPermissions(...)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {

            if (grantResults.size == 3
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                //Toast.makeText(this, "Record Audio permission granted", Toast.LENGTH_SHORT).show();
            } else {

                Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
        }
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

class ChatFromAudioItem (val text : String, val user : User) : Item<GroupieViewHolder>(){

    private var mp : MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        val buttonPlay = viewHolder.itemView.imageButtonVoicePlayChatFrom
        val buttonPause = viewHolder.itemView.imageButtonVoicePauseChatFrom
        val seekBar = viewHolder.itemView.seekBarVoiceChatFrom

        buttonPlay.setOnClickListener {

            mp = MediaPlayer()
            mp!!.setDataSource(text)
            mp!!.setOnPreparedListener { player ->
                player.start()
                initializeSeekBar(viewHolder)
            }
            mp!!.prepareAsync()

            /*if (mp == null) {

                mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(text)
                    prepare() // might take long! (for buffering, etc)
                    initializeSeekBar(viewHolder)
                }
            }
            mp?.start()*/

            buttonPlay.visibility = View.GONE
            buttonPause.visibility = View.VISIBLE
        }

        buttonPause.setOnClickListener {

            if (mp !== null) {

                mp?.pause()
            }

            buttonPause.visibility = View.GONE
            buttonPlay.visibility = View.VISIBLE
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                if (fromUser) {

                    mp?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageViewVoiceUserChatFrom

        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {

        return R.layout.chat_from_voice_row
    }

    private fun initializeSeekBar(viewHolder: GroupieViewHolder) {

        val seekBar = viewHolder.itemView.seekBarVoiceChatFrom

        seekBar.max = mp!!.duration

        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    seekBar.progress = mp!!.currentPosition
                    handler.postDelayed(this, 1000)
                } catch (e : Exception) {
                    seekBar.progress = 0
                }
            }
        }, 0)
    }
}

class ChatToAudioItem (val text : String, val user : User) : Item<GroupieViewHolder>(){

    private var mp : MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        val buttonPlay = viewHolder.itemView.imageButtonVoicePlayChatTo
        val buttonPause = viewHolder.itemView.imageButtonVoicePauseChatTo
        val seekBar = viewHolder.itemView.seekBarVoiceChatTo

        buttonPlay.setOnClickListener {

            mp = MediaPlayer()
            mp!!.setDataSource(text)
            mp!!.setOnPreparedListener { player ->
                player.start()
                initializeSeekBar(viewHolder)
            }
            mp!!.prepareAsync()

            /*if (mp == null) {

                mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(text)
                    prepare() // might take long! (for buffering, etc)
                    initializeSeekBar(viewHolder)
                }
            }
            mp?.start()*/

            buttonPlay.visibility = View.INVISIBLE
            buttonPause.visibility = View.VISIBLE
        }

        buttonPause.setOnClickListener {

            if (mp !== null) {

                mp?.pause()
            }

            buttonPause.visibility = View.INVISIBLE
            buttonPlay.visibility = View.VISIBLE
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                if (fromUser) {

                    mp?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageViewVoiceUserChatTo

        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {

        return R.layout.chat_to_voice_row
    }

    private fun initializeSeekBar(viewHolder: GroupieViewHolder) {

        val seekBar = viewHolder.itemView.seekBarVoiceChatTo

        seekBar.max = mp!!.duration

        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    seekBar.progress = mp!!.currentPosition
                    handler.postDelayed(this, 1000)
                } catch (e : Exception) {
                    seekBar.progress = 0
                }
            }
        }, 0)
    }
}