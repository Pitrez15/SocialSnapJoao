package com.example.socialsnap.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.socialsnap.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    companion object {

        val TAG = "RegisterActivity"
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        buttonRegister.setOnClickListener {

            auth.createUserWithEmailAndPassword(editTextRegisterUsername.text.toString(), editTextRegisterPassword.text.toString())
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser

                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    }
                    else {

                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}