package com.example.socialsnap

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        auth = Firebase.auth

        val currentUser = auth.currentUser

        currentUser?.let {

            Handler(Looper.getMainLooper()).postDelayed({

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }, 2000)

        }?:run {

            Handler(Looper.getMainLooper()).postDelayed({

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }, 2000)
        }
    }
}