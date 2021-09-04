package com.example.kaisanbaa.Activities

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.kaisanbaa.Adapters.GroupMessageAdapter
import com.example.kaisanbaa.Models.Message
import com.example.kaisanbaa.R
import com.example.kaisanbaa.databinding.ActivityGroupChatBinding
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
import kotlin.collections.ArrayList

class GroupChatActivity : AppCompatActivity() {


    private  var messages : ArrayList<Message> = arrayListOf()
    private lateinit var database : FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var binding : ActivityGroupChatBinding
    private  var senderUid : String? = FirebaseAuth.getInstance().uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Installs emojis
        EmojiManager.install(IosEmojiProvider())

        // instantiate Firebase things
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        //initialize recycler view for group chat
        val recyclerView = binding.groupChatRecyclerView
        recyclerView.adapter = GroupMessageAdapter(this@GroupChatActivity, messages)

        // emoji initialization
        val emojiPopup = EmojiPopup.Builder.fromRootView(binding.root).build(findViewById(R.id.id_typemsg))
        emojiPopup.dismiss() // Dismisses the Popup.
        emojiPopup.isShowing // Returns true when Popup is showing.
        // Emoji button is clicked
        binding.idEmoji.setOnClickListener {
            emojiPopup.toggle() // Toggles visibility of the Popup.
        }

        //when send button is clicked
        binding.idSend.setOnClickListener {
            val msgtxt = binding.idTypemsg.text.toString()
            val date = Date()
            val message = Message("", msgtxt, senderUid, date.time, 0, "")
            binding.idTypemsg.setText("")

            database.reference.child("public")
                    .push()
                    .setValue(message)
        }

        // when attachment button is clicked open intent to upload image
        binding.idAttach.setOnClickListener{
            val intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent, 25)
        }

        // read chats from realtime database
        database.reference.child("public")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        messages.clear()
                        if(snapshot.exists()){
                            for(messagesSnapshot in snapshot.children){
                                    val message = messagesSnapshot.getValue(Message::class.java)
                                    messages.add(message!!)

                            }
                            // add the new message to recycler view
                            recyclerView.adapter?.notifyDataSetChanged()
                            //everytime a new msg is sent/receiver automatic scroll to that position
                            if(messages.size !=0)
                            recyclerView.smoothScrollToPosition(messages.size -1)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })

        binding.backButton.setOnClickListener {
            finish()
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
                    val reference : StorageReference = storage.reference.child("chats").child(calendar.timeInMillis.toString())
                    reference.putFile(selectedImage).addOnCompleteListener{
                        if(it.isSuccessful){
                            dialog.dismiss()
                            //if upload is successful, download the image from that url and proceed to put it into realtime database
                            reference.downloadUrl.addOnSuccessListener {
                                val filePath = it.toString()
                                val date = Date()
                                val message = Message("", "Photo", senderUid, date.time, 0, filePath)
                                binding.idTypemsg.setText("")
                                // nodes of same message in both sender and receiver room must be same to read conveniently

                                database.reference
                                        .child("public")
                                        .push()
                                        .setValue(message)
                            }
                        }
                    }
                }
            }
        }
    }
}