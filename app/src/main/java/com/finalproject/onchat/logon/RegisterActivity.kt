package com.finalproject.onchat.logon

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.finalproject.onchat.R
import com.finalproject.onchat.messages.LatestMessagesActivity
import com.finalproject.onchat.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.io.IOException
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var btnChooseImage: Button
    private lateinit var etUsername: EditText
    private lateinit var etEmailAdd: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPass: EditText
    private lateinit var btnRegister: Button
    private lateinit var txtSignIn: TextView
    private val mAuth = FirebaseAuth.getInstance()
    var selectedPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_register)

        btnChooseImage = findViewById(R.id.btnChooseImage)
        etUsername = findViewById(R.id.etUsername)
        etEmailAdd = findViewById(R.id.etEmailAddress)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPass = findViewById(R.id.etConfirmPass)
        btnRegister = findViewById(R.id.btnRegister)
        txtSignIn = findViewById(R.id.txtSignIn)

        btnRegister.setOnClickListener {
            if (etUsername.length() == 0 && etEmailAdd.length() == 0 && etPassword.length() == 0 && etConfirmPass.length() == 0) {
                if (etUsername.length() == 0) {
                    etUsername.error = "Field can't be blank"
                }
                if (etEmailAdd.length() == 0) {
                    etEmailAdd.error = "Field can't be blank"
                }
                if (etPassword.length() == 0) {
                    etPassword.error = "Field can't be blank"
                }
                if (etConfirmPass.length() == 0) {
                    etConfirmPass.error = "Field can't be blank"
                }
            } else {
                mAuth.createUserWithEmailAndPassword(
                    etEmailAdd.text.toString(),
                    etPassword.text.toString()
                )
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val alertDialog = AlertDialog.Builder(this@RegisterActivity)
                            alertDialog.setTitle("Registration Successful")
                            alertDialog.setMessage("Your account has been successfully created in our Database, click ok to get redirected to main screen")
                            alertDialog.setCancelable(false)
                            alertDialog.setPositiveButton("Ok") {text, listener ->
                                uploadImageToFirebaseStorage()
                            }
                            alertDialog.create()
                            alertDialog.show()
                        } else {
                            val alertDialog = AlertDialog.Builder(this@RegisterActivity)
                            alertDialog.setTitle("Registration Failed")
                            alertDialog.setMessage("Either you're already register or having network connectivity issue")
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

        txtSignIn.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
        }

    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) return

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    saveUserToFirebaseDatabase(it.toString())
                }
            }
            .addOnFailureListener {
                // do some logging here
            }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(
            uid,
            etUsername.text.toString(),
            profileImageUrl
        )

        ref.setValue(user)
            .addOnSuccessListener {
                val intent = Intent(this@RegisterActivity, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            // proceed and check what the selected image was....
            try {
                selectedPhotoUri = data.data
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedPhotoUri)
                circleImageView.setImageBitmap(bitmap)
                btnChooseImage.alpha = 0f
                //val bitmapDrawable = BitmapDrawable(bitmap)
                //btnChooseImage.setBackgroundDrawable(bitmapDrawable)
            } catch (re: java.lang.RuntimeException) {
                re.printStackTrace()
                Toast.makeText(this@RegisterActivity, "Image Size is too big for profile picture.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun getPhoto() {
        try{
            try {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, 1)
            } catch (re: RuntimeException) {
                re.printStackTrace()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("NewApi")
    fun chooseImage(view: View) {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            getPhoto()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPhoto()
            }
        }
    }
}