package com.example.kaisanbaa.Activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.kaisanbaa.databinding.ActivityOTPBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class OTPActivity : AppCompatActivity() {

    private lateinit var auth : FirebaseAuth
    private var storedVerificationId: String? = ""
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var credential : PhoneAuthCredential

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        supportActionBar?.hide()
        val binding = ActivityOTPBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dialog = ProgressDialog(this)
        dialog.setMessage("Sending OTP...")
        dialog.setCancelable(true)
        dialog.show()

        val phoneNumber = intent.getStringExtra("phoneNumber").toString()
        binding.textView.text = "Verify ${phoneNumber}"

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {

                Log.d("onVerificationCompleted", "onVerificationCompleted:$credential")
            }

            override fun onVerificationFailed(e: FirebaseException) {

                Log.d("onVerificationFailed", "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }
            }

            override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
            ) {
                   dialog.dismiss()
                Log.d("onCodeSent", "onCodeSent:$verificationId")
                storedVerificationId = verificationId
            }
        }
        // [END phone_auth_callbacks]

        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

        binding.otpView.setOtpCompletionListener {
            credential = PhoneAuthProvider.getCredential(storedVerificationId!!, it)
            auth.signInWithCredential(credential).addOnCompleteListener {
                if(it.isSuccessful){
                    val intent = Intent(this, SetupProfileActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                }
                else{
                    Toast.makeText(this, "Log in failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    }
