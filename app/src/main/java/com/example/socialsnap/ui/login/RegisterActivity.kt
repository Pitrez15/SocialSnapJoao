package com.example.socialsnap.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.socialsnap.R
import com.example.socialsnap.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.core.View
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_register.view.*
import java.util.*

class RegisterActivity : AppCompatActivity() {

    companion object {

        val TAG = "RegisterActivity"
    }

    var selectedPhotoUri : Uri? = null

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        imageViewLogo.setOnClickListener {

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"

            startActivityForResult(intent, 0)
        }

        buttonRegister.setOnClickListener {

            auth.createUserWithEmailAndPassword(editTextRegisterEmail.text.toString(), editTextRegisterPassword.text.toString())
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        uploadImageToFirebaseStorage()

                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    }
                    else {

                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {

            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
            imageViewLogo.setImageBitmap(bitmap)
        }
    }

    private fun uploadImageToFirebaseStorage() {

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {

            ref.downloadUrl.addOnSuccessListener {

                saveUserToFirebaseDatabase(it.toString())
            }
        }
            .addOnFailureListener {

        }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl : String) {

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users_chat").document(FirebaseAuth.getInstance().uid ?: "")

        val user = User(uid, editTextRegisterUsername.text.toString(), profileImageUrl, editTextRegisterEmail.text.toString(), "")

        ref.set(user)
            .addOnSuccessListener {

            }
            .addOnFailureListener {

            }
    }
}