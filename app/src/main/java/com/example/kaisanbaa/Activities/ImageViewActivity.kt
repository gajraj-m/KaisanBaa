package com.example.kaisanbaa.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.kaisanbaa.R

class ImageViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Image Viewer")

        val imageUrl = intent.getStringExtra("imageUrl")
        Glide.with(this).load(imageUrl).placeholder(R.drawable.placeholder).into(findViewById(R.id.id_touch_image_view))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}