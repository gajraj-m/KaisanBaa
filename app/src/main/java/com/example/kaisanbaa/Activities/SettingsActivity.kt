package com.example.kaisanbaa.Activities

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kaisanbaa.Models.User
import com.example.kaisanbaa.R
import com.example.kaisanbaa.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var uid: String
    private lateinit var storage : FirebaseStorage
    private lateinit var database: FirebaseDatabase
    private var selectedImageURI : Uri? = null
    private var imageUri : String? = null
    private var user = User()
    private var name = String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setTitle("Settings")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        uid = FirebaseAuth.getInstance().currentUser!!.uid
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        database.reference.child("users").child(uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            user = snapshot.getValue(User::class.java)!!
                            imageUri = user.imageUri
                            Glide.with(applicationContext)
                                    .load(imageUri)
                                    .placeholder(R.drawable.placeholder)
                                    .into(binding.profileDp)

                            binding.profileName.text = user.name
                            binding.idUid.text = user.uid
                            val phone = user.phone
                            binding.idPhone.text = phone?.substring(0, 3) + " " + phone?.substring(3)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })

        binding.profileDp.setOnClickListener {
            val intent = Intent(this, ImageViewActivity::class.java)
            intent.putExtra("imageUrl", imageUri)
            startActivity(intent)
        }

        //update Name
        binding.idEditName.setOnClickListener {

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Update your Name")
            // Set up the input
            val input = EditText(this)
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            // Set up the buttons
            builder.setPositiveButton("OK") { dialog, _ ->
                    name = input.text.toString()
                    binding.profileName.text = name
                val userMap = HashMap<String, Any?>()
                userMap.put("name", name)
                database.reference.child("users").child(uid).updateChildren(userMap)
                }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            builder.show()
        }

        // update DP
        binding.changeDp.setOnClickListener {
            val intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent, 45)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null){
            if(data.data != null) {
                binding.profileDp.setImageURI(data.data)
                selectedImageURI = data.data!!

                // update image in storage as well as database
                   // Toast.makeText(this, "pasigali bhitare", Toast.LENGTH_SHORT).show()
                    val reference: StorageReference = storage.reference.child("Profiles").child(uid)
                    reference.putFile(selectedImageURI!!).addOnCompleteListener {
                        if (it.isSuccessful) {
                            reference.downloadUrl.addOnSuccessListener {
                                val imageUri = it.toString()
                                val userMap = HashMap<String, Any?>()
                                userMap.put("imageUri", imageUri)
                                database.reference.child("users").child(uid).updateChildren(userMap)
                            }
                        }
                    }
            }
        }
    }
}