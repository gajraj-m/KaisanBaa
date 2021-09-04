package com.example.kaisanbaa.Activities

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.kaisanbaa.Models.User
import com.example.kaisanbaa.databinding.ActivitySetupProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class SetupProfileActivity : AppCompatActivity() {
    private lateinit var auth : FirebaseAuth
    private lateinit var binding : ActivitySetupProfileBinding
    private lateinit var database : FirebaseDatabase
    private lateinit var storage : FirebaseStorage
    private  var selectedImageURI : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

         binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.profileImage.setOnClickListener {
            val intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent, 45)
        }

        binding.setupProfile.setOnClickListener {
            val name = binding.nameBox.text.toString()
            if (name.isEmpty()) {
                binding.nameBox.setError("Please type a Name")
            } else {
                val dialog = ProgressDialog(this)
                dialog.setMessage("Setting up your profile...")
                dialog.setCancelable(false)
                dialog.show()

                if(selectedImageURI != null) {
                    val reference: StorageReference = storage.reference.child("Profiles").child(auth.uid!!)
                    reference.putFile(selectedImageURI!!).addOnCompleteListener {
                        if (it.isSuccessful) {
                            reference.downloadUrl.addOnSuccessListener {
                                val imageUri = it.toString()
                                val uid = auth.uid
                                val phone = auth.currentUser!!.phoneNumber
                                val user = User(uid, name, phone, imageUri)
                                database.reference
                                        .child("users")
                                        .child(uid!!)
                                        .setValue(user)
                                        .addOnSuccessListener {
                                            dialog.dismiss()
                                            val intent = Intent(this, MainActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }

                            }.addOnFailureListener {
                                Toast.makeText(this, "Failed bro", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                else {
                    val uid = auth.uid
                    val phone = auth.currentUser!!.phoneNumber
                    val user = User(uid, name, phone, "No Image")
                    database.reference
                            .child("users")
                            .child(uid!!)
                            .setValue(user)
                            .addOnSuccessListener {
                                dialog.dismiss()
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null){
            if(data.data != null) {
                binding.profileImage.setImageURI(data.data)
                selectedImageURI = data.data!!
            }
        }
    }
}