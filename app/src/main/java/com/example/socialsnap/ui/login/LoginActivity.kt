package com.example.socialsnap.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.socialsnap.MainActivity
import com.example.socialsnap.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception


class LoginActivity : AppCompatActivity() {

    companion object {

        val TAG = "LoginActivity"
        private const val REQUEST_CODE_SIGN_IN = 9001
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        textViewRegister.setOnClickListener {

            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        buttonLogin.setOnClickListener {

            auth.signInWithEmailAndPassword(
                editTextLoginUsername.text.toString(),
                editTextLoginPassword.text.toString()
            ).addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                    else {

                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        buttonGoogleLogin.setOnClickListener {

            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.webclient_id))
                .requestEmail()
                .build()
            val signInClient = GoogleSignIn.getClient(this, options)

            signInClient.signInIntent.also {

                startActivityForResult(it, REQUEST_CODE_SIGN_IN)
            }
        }
    }

    private fun googleAuthForFirebase (account: GoogleSignInAccount){

        val credentials = GoogleAuthProvider.getCredential(account.idToken, null)
        CoroutineScope(Dispatchers.IO).launch {

            try {

                auth.signInWithCredential(credentials).await()
                withContext(Dispatchers.Main){
                    Toast.makeText(this@LoginActivity, "Sign In Successfully", Toast.LENGTH_LONG).show()
                }
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
            }
            catch (e: Exception){

                withContext(Dispatchers.Main) {

                    Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SIGN_IN) {

            val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
            account?.let {

                googleAuthForFirebase(it)
            }
        }
    }
}