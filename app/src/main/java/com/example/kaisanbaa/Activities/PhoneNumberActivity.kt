package com.example.kaisanbaa.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.kaisanbaa.databinding.ActivityPhoneNumberBinding
import com.google.firebase.auth.FirebaseAuth

class PhoneNumberActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser !=null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        val binding = ActivityPhoneNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.verifyBtn.setOnClickListener {
            val intent = Intent(this, OTPActivity::class.java)
            intent.putExtra("phoneNumber", "+91" + binding.phoneBox.text.toString())
            startActivity(intent)
        }

    }
}