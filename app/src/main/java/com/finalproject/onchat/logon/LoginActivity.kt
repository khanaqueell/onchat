package com.finalproject.onchat.logon

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.finalproject.onchat.R
import com.finalproject.onchat.messages.LatestMessagesActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmailAdd: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtSignUp: TextView
    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_login)

        etEmailAdd = findViewById(R.id.etEmailAddress)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtSignUp = findViewById(R.id.txtSignUp)

        btnLogin.setOnClickListener {
            if (etEmailAdd.length() == 0 && etPassword.length() == 0) {
                if (etEmailAdd.length() == 0) {
                    etEmailAdd.error = "Field can't be blank"
                }
                if (etPassword.length() == 0) {
                    etPassword.error = "Field can't be blank"
                }
            } else {
                mAuth.signInWithEmailAndPassword(
                    etEmailAdd.text.toString(),
                    etPassword.text.toString()
                )
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Send to the message Activity
                            val intent = Intent(this@LoginActivity, LatestMessagesActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        } else {
                            val alertDialog = AlertDialog.Builder(this@LoginActivity)
                            alertDialog.setTitle("Error")
                            alertDialog.setMessage("Either bad credentials or having network connectivity issue")
                            alertDialog.setCancelable(false)
                            alertDialog.setPositiveButton("Retry") {text, listener ->
                                // Go to Message Screen
                            }
                            alertDialog.create()
                            alertDialog.show()
                        }
                    }
            }
        }

        txtSignUp.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}