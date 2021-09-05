package com.example.kaisanbaa.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.kaisanbaa.R

class ClickViewActivity : AppCompatActivity() {
    private lateinit var senderRoom : String
    private lateinit var receiverRoom : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_click_view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Image Viewer")

        //sender receiver room
        senderRoom = intent.getStringExtra("senderRoom")!!
        receiverRoom = intent.getStringExtra("receiverRoom")!!

        val imageUri = intent.getStringExtra("imageUri")
        Glide.with(this).load(imageUri).into(findViewById(R.id.id_touch_image_view))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}