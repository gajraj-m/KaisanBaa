package com.example.kaisanbaa.Activities

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.kaisanbaa.Models.Message
import com.example.kaisanbaa.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
typealias LumaListener = (luma: Double) -> Unit

class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var senderUid : String
    private lateinit var receiverUid : String
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        supportActionBar?.hide()

        // initialize firebase stuff
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        //sender receiver room
        senderUid = intent.getStringExtra("senderUid")!!
        receiverUid = intent.getStringExtra("receiverUid")!!

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT , Locale.UK
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val imageView : ConstraintLayout = findViewById(R.id.preview_layout)
                    val shutter : ImageButton = findViewById(R.id.camera_capture_button)
                    val calendar = Calendar.getInstance()
                    imageView.visibility = View.VISIBLE
                    shutter.visibility = View.GONE
                    Glide.with(this@CameraActivity).load(savedUri).into(findViewById(R.id.image_preview))

                    send_button.setOnClickListener {
                        val dialog = ProgressDialog(this@CameraActivity)
                        dialog.setMessage("Sending image...")
                        dialog.setCancelable(false)
                        dialog.show()
                        // Firebase update
                    val reference : StorageReference = storage.reference.child("chats").child(
                        calendar.timeInMillis.toString()
                    )
                    reference.putFile(savedUri).addOnCompleteListener{
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

                                if(receiverUid != "") {
                                    // Making a Hash map to update the last messages details... updateChildren( ) takes hash map in arg
                                    val lastMsgObj: HashMap<String, Any> = HashMap()
                                    lastMsgObj.put("lastMsg", message.msg!!)
                                    lastMsgObj.put("lastMsgTime", date.time)
                                    database.reference.child("chats").child(senderUid + receiverUid).updateChildren(
                                            lastMsgObj
                                    )
                                    database.reference.child("chats").child(receiverUid + senderUid).updateChildren(
                                            lastMsgObj
                                    )

                                    database.reference
                                            .child("chats")
                                            .child(senderUid + receiverUid)
                                            .child("message")
                                            .child(randomKey!!)
                                            .setValue(message).addOnSuccessListener {
                                                database.reference
                                                        .child("chats")
                                                        .child(receiverUid + senderUid)
                                                        .child("message")
                                                        .child(randomKey)
                                                        .setValue(message)
                                            }.addOnSuccessListener {
                                                imageView.visibility = View.GONE
                                                shutter.visibility = View.VISIBLE
                                                finish()
                                            }
                                }
                                else{
                                    database.reference
                                            .child("public")
                                            .push()
                                            .setValue(message)
                                            .addOnSuccessListener {
                                                imageView.visibility = View.GONE
                                                shutter.visibility = View.VISIBLE
                                                finish()
                                            }
                                }
                            }
                        }
                    }
                    }
                }
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}