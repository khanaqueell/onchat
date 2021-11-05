package com.finalproject.onchat.messages

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import com.finalproject.onchat.R
import com.finalproject.onchat.logon.RegisterActivity
import com.finalproject.onchat.messages.NewMessageActivity.Companion.USER_KEY
import com.finalproject.onchat.models.ChatMessage
import com.finalproject.onchat.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.dialog_profile.*
import kotlinx.android.synthetic.main.latest_message_row.view.*

class LatestMessagesActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private var number: String? = "tel:"

    companion object {
        var currentUser: User? = null
    }

    val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_latest_messages)

        toolbar = findViewById(R.id.toolbar)

        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this@LatestMessagesActivity, DividerItemDecoration.VERTICAL))

        adapter.setOnItemClickListener {item, view ->
            val intent = Intent(this@LatestMessagesActivity, ChatLogActivity::class.java)
            val row = item as LatestMessageRow
            intent.putExtra(NewMessageActivity.USER_KEY, row.chatPartnerUser)
            startActivity(intent)
        }

        listenForLatestMessages()

        fetchCurrentUser()

        verifyUserIsLoggedIn()

        setUpToolbar()

    }

    val latestMessagesMap = HashMap<String, ChatMessage>()

    private fun refreshRecyclerViewMessages() {
        adapter.clear()
        latestMessagesMap.values.forEach {
            adapter.add(LatestMessageRow(it))
        }
    }

    private fun listenForLatestMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId")
        ref.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[p0.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[p0.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}

        })
    }

    class LatestMessageRow(val chatMessage: ChatMessage): Item<ViewHolder>() {
        var chatPartnerUser: User? = null

        override fun getLayout(): Int {
            return R.layout.latest_message_row
        }

        override fun bind(viewHolder: ViewHolder, position: Int) {
            viewHolder.itemView.txtRecentText.text =chatMessage.text

            val chatPartnerId: String
            if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                chatPartnerId = chatMessage.toId
            } else {
                chatPartnerId = chatMessage.fromId
            }

            val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
            ref.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    chatPartnerUser = p0.getValue(User::class.java)
                    viewHolder.itemView.txtUsername.text = chatPartnerUser?.username
                    val targetImageView = viewHolder.itemView.latestMessageProfile
                    Picasso.get().load(chatPartnerUser?.profileImageUrl).into(targetImageView)
                }

            })
        }

    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                currentUser = p0.getValue(User::class.java)
            }

        })
    }

    private fun verifyUserIsLoggedIn() {
        val uid = FirebaseAuth.getInstance().uid
        if (uid == null) {
            val intent = Intent(this@LatestMessagesActivity, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_item, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("NewApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId

        if (id == R.id.logOut) {
            val intent = Intent(this@LatestMessagesActivity, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            FirebaseAuth.getInstance().signOut()
        }

        if (id == R.id.newMessage) {
            val intent = Intent(this@LatestMessagesActivity, NewMessageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        if (id == R.id.redirectDialer) {
            val dialog = Dialog(this@LatestMessagesActivity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_profile)
            dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.edt_bg))
            dialog.setCancelable(false)
            val etPhoneNumber = dialog.findViewById<EditText>(R.id.etPhoneNumber)
            val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
            val btnMakeCall = dialog.findViewById<Button>(R.id.btnMakeCall)
            btnMakeCall.setOnClickListener {
                if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.CALL_PHONE),
                        1
                    )
                } else {
                    val phoneNumber = etPhoneNumber.text.toString()
                    var contact_number: String? = "tel:"+phoneNumber
                    if (!PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
                        contact_number = "tel:" + phoneNumber
                    } else {
                        contact_number = "tel:" + phoneNumber
                    }
                    startActivity(Intent(Intent.ACTION_CALL, Uri.parse(contact_number)))
                }
            }
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            dialog.create()
            dialog.show()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
                val phone = "tel:" + etPhoneNumber.text
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse(phone)))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            try {
                val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
                val phone = "tel:$etPhoneNumber"
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse(phone)))
            } catch (re: java.lang.RuntimeException) {
                re.printStackTrace()
                Toast.makeText(this@LatestMessagesActivity, "Invalid phone number.", Toast.LENGTH_SHORT).show()
            }
        }

    }

}