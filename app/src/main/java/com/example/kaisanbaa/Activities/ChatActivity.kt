package com.example.kaisanbaa.Activities

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kaisanbaa.Adapters.MessageAdapter
import com.example.kaisanbaa.Models.Message
import com.example.kaisanbaa.R
import com.example.kaisanbaa.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.ios.IosEmojiProvider
import java.util.*
import kotlin.collections.HashMap


class ChatActivity : AppCompatActivity() {

   private lateinit var messages : ArrayList<Message>
   private lateinit var database : FirebaseDatabase
   private lateinit var storage: FirebaseStorage
   private lateinit var senderRoom : String
   private lateinit var receiverRoom : String
   private lateinit var binding : ActivityChatBinding
   private  var senderUid : String?=null
    private var emojiIcon = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Installs emojis
        EmojiManager.install(IosEmojiProvider())

        messages = arrayListOf()

        // bind chat recycler view with chat activity
        val recyclerView = binding.chatRecyclerView
        recyclerView.adapter = MessageAdapter(this@ChatActivity, messages)

        // recieve name, profile dp and receiver uid sent from main activity through intent
        val name = intent.getStringExtra("name")
        val profile_image = intent.getStringExtra("profile image")
        val receiverUid  = intent.getStringExtra("uid")

        // set user name and image
        binding.idProfileName.text = name
        Glide.with(this).load(profile_image).placeholder(R.drawable.chat_avatar).into(binding.idProfileDp)
        binding.idProfileDp.setOnClickListener {
            val intent = Intent(this, ImageViewActivity::class.java)
            intent.putExtra("imageUri", profile_image)
            startActivity(intent)
        }

        // back arrow
        binding.backButton.setOnClickListener { finish() }  //finishes chat activity and goes back to main activity

        senderUid = FirebaseAuth.getInstance().uid

        senderRoom = senderUid + receiverUid   // string concatenation
        receiverRoom = receiverUid + senderUid

        // instantiate database and storage
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // emoji initialization
        val emojiPopup = EmojiPopup.Builder.fromRootView(binding.root).build(findViewById(R.id.id_typemsg))
        emojiPopup.dismiss() // Dismisses the Popup.
        emojiPopup.isShowing // Returns true when Popup is showing.
        // Emoji button is clicked
        binding.idEmoji.setOnClickListener {
            if(emojiIcon){
                binding.idEmoji.setImageResource(R.drawable.keyboard)
                emojiIcon = false
            }
            else{
                binding.idEmoji.setImageResource(R.drawable.emoji_ios_category_smileysandpeople)
                emojiIcon = true
            }
            emojiPopup.toggle() // Toggles visibility of the Popup.
        }

        // when send button is clicked create message object and push it to realtime database
        binding.idSend.setOnClickListener{
            val msgtxt = binding.idTypemsg.text.toString()
            val date = Date()
            val randomKey = database.reference.push().key
            val message = Message(randomKey, msgtxt, senderUid, date.time, 0, "")
            binding.idTypemsg.setText("")

            val lastMsgObj : HashMap<String, Any> = HashMap ()
            lastMsgObj.put("lastMsg", message.msg!!)
            lastMsgObj.put("lastMsgTime", date.time)
            database.reference.child("chats").child(senderRoom).updateChildren(lastMsgObj)
            database.reference.child("chats").child(receiverRoom).updateChildren(lastMsgObj)

            // put message into database
            database.reference
                    .child("chats")
                    .child(senderRoom)
                    .child("message")
                    .child(randomKey!!)
                    .setValue(message).addOnSuccessListener {
                        database.reference
                                .child("chats")
                                .child(receiverRoom)
                                .child("message")
                                .child(randomKey)
                                .setValue(message)
                    }
        }
        // read chats from realtime database
        database.reference.child("chats")
                .child(senderRoom)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        messages.clear()
                        if (snapshot.exists()) {
                            for (messagesSnapshot in snapshot.children) {
                                for (msgsnapshot in messagesSnapshot.children) {
                                    val message = msgsnapshot.getValue(Message::class.java)
                                    messages.add(message!!)
                                }
                            }
                            // add the new message to recycler view
                            recyclerView.adapter?.notifyDataSetChanged()
                            //everytime a new msg is sent/receiver automatic scroll to that position
                            if (messages.size != 0)
                                recyclerView.smoothScrollToPosition(messages.size - 1)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })

        // receiver is online or offline?
        database.reference.child("presence").child(receiverUid!!).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val presence = snapshot.getValue(String::class.java)
                    if (presence!!.isNotEmpty()) {
                        binding.idAvailable.text = presence
                        binding.idAvailable.visibility = View.VISIBLE
                        if (presence == "offline")
                            binding.idAvailable.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

        // I'm typing or not
        val hander = Handler()      // this handler and runnable is to notify when user has stopped typing
        binding.idTypemsg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                database.reference.child("presence").child(senderUid!!).setValue("typing...")
                hander.removeCallbacksAndMessages(null)
                hander.postDelayed(userStoppedTyping, 1000)
            }

            val userStoppedTyping = Runnable {
                database.reference.child("presence").child(senderUid!!).setValue("online")
            }

        })

        // when attachment button is clicked open intent to upload image
        binding.idAttach.setOnClickListener{
           val intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent, 25)
        }

        // when camera button is clicked
        binding.idClick.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("senderUid", senderUid)
            intent.putExtra("receiverUid", receiverUid)
            startActivity(intent)
        }
    }

    // right after binding.idAttach is executed it comes here with request code = 25
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==25){
            if(data!=null){
                if(data.data!=null){

                    //dialog for uploading image
                    val dialog = ProgressDialog(this)
                    dialog.setMessage("Uploading image...")
                    dialog.setCancelable(false)
                    dialog.show()

                    val selectedImage : Uri = data.data!!       // path of selected image
                    val calendar = Calendar.getInstance()
                    val reference : StorageReference = storage.reference.child("chats").child(
                        calendar.timeInMillis.toString()
                    )
                    reference.putFile(selectedImage).addOnCompleteListener{
                        if(it.isSuccessful){
                            dialog.dismiss()
            //if upload is successful, download the image from that url and proceed to put it into realtime database
                            reference.downloadUrl.addOnSuccessListener {
                                val filePath = it.toString()
                                val date = Date()
                                // nodes of same message in both sender and receiver room must be same to read conveniently
                                val randomKey = database.reference.push().key
                                val message = Message(
                                    randomKey,
                                    "Photo",
                                    senderUid,
                                    date.time,
                                    0,
                                    filePath
                                )
                                binding.idTypemsg.setText("")

                                val lastMsgObj : HashMap<String, Any> = HashMap ()
                                lastMsgObj.put("lastMsg", message.msg!!)
                                lastMsgObj.put("lastMsgTime", date.time)
                                database.reference.child("chats").child(senderRoom).updateChildren(
                                    lastMsgObj
                                )
                                database.reference.child("chats").child(receiverRoom).updateChildren(
                                    lastMsgObj
                                )

                                database.reference
                                        .child("chats")
                                        .child(senderRoom)
                                        .child("message")
                                        .child(randomKey!!)
                                        .setValue(message).addOnSuccessListener {
                                            database.reference
                                                    .child("chats")
                                                    .child(receiverRoom)
                                                    .child("message")
                                                    .child(randomKey)
                                                    .setValue(message)
                                        }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        database.reference.child("presence").child(FirebaseAuth.getInstance().uid!!).setValue("online")
    }

    override fun onPause() {
        super.onPause()
        database.reference.child("presence").child(FirebaseAuth.getInstance().uid!!).setValue("offline")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }


}