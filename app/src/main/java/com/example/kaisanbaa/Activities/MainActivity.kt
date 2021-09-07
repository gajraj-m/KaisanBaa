package com.example.kaisanbaa.Activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.kaisanbaa.Adapters.TopStatusAdapter
import com.example.kaisanbaa.Adapters.UserAdapter
import com.example.kaisanbaa.Models.Status
import com.example.kaisanbaa.Models.User
import com.example.kaisanbaa.Models.UserStatus
import com.example.kaisanbaa.R
import com.example.kaisanbaa.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase
    private var userArrayList : ArrayList<User>  = arrayListOf()
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusRecyclerView: RecyclerView
    private var userStatuses : ArrayList<UserStatus> = arrayListOf()
    private var user = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance()

        // recycler view for display of users
        recyclerView = binding.recyclerView

        //  recycler view for status
        statusRecyclerView = binding.statusLister
        statusRecyclerView.adapter = TopStatusAdapter(this, userStatuses)

        binding.recyclerView.showShimmerAdapter()

        // retrieve data from database
        getUserData()
        getUserStatusData()

        //current user data
        database.reference.child("users").child(FirebaseAuth.getInstance().uid!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        user = snapshot.getValue(User::class.java)!!
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })

        // Bottom navigation items clicked
        binding.bottomNavigationView.setOnNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.id_status -> {
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_GET_CONTENT)
                    intent.setType("image/*")
                    startActivityForResult(intent, 75)
                }
                R.id.id_group -> startActivity(Intent(this, GroupChatActivity::class.java))
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId){   // when group is clicked
                R.id.id_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.id_invite -> {
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
                    intent.setType("text/plain");
                    startActivity(intent)
                }
            }
        return super.onOptionsItemSelected(item)
    }

    fun getUserData(){
        database.getReference().child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userArrayList.clear()
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user?.uid != FirebaseAuth.getInstance().uid)
                            userArrayList.add(user!!)
                    }
                    recyclerView.adapter = UserAdapter(this@MainActivity, userArrayList)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    fun getUserStatusData(){
        database.reference.child("stories")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            userStatuses.clear()
                            for (storySnapshot in snapshot.children) {
                                val status = UserStatus()
                                status.name = storySnapshot.child("name").getValue(String::class.java)
                                status.profileImage = storySnapshot.child("profileImage").getValue(String::class.java)
                                status.lastUpdated = storySnapshot.child("lastUpdated").getValue(Long::class.java)

                                val statuses = ArrayList<Status>() // store the statuses

                                for (statusSnapshot in storySnapshot.child("statuses").children) {
                                    val sampleStatus = statusSnapshot.getValue(Status::class.java)
                                    statuses.add(sampleStatus!!)
                                }
                                status.statuses = statuses
                                userStatuses.add(status)
                            }
                            statusRecyclerView.adapter?.notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
    }

    override fun onResume() {
        super.onResume()
        database.reference.child("presence").child(FirebaseAuth.getInstance().uid!!).setValue("online")
    }

    override fun onPause() {
        super.onPause()
        database.reference.child("presence").child(FirebaseAuth.getInstance().uid!!).setValue("offline")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null){
            if(data.data != null) {
                //dialog box
                val dialog = ProgressDialog(this)
                dialog.setMessage("Uploading status...")
                dialog.setCancelable(false)
                dialog.show()

                val storage = FirebaseStorage.getInstance()
                val date = Date()
                val reference: StorageReference = storage.reference.child("status").child(date.time.toString())
                reference.putFile(data.data!!).addOnCompleteListener{
                    if(it.isSuccessful){
                        reference.downloadUrl.addOnSuccessListener {
                            val userStatus = UserStatus()
                            userStatus.name = user.name
                            userStatus.profileImage = user.imageUri
                            userStatus.lastUpdated = date.time

                            val storiesObj : HashMap<String, Any> = HashMap()
                            storiesObj.put("name", userStatus.name!!)
                            storiesObj.put("profileImage", userStatus.profileImage!!)
                            storiesObj.put("lastUpdated", userStatus.lastUpdated!!)

                            val stories = Status(it.toString(), userStatus.lastUpdated)

                            database.reference.child("stories")
                                    .child(FirebaseAuth.getInstance().uid!!)
                                    .updateChildren(storiesObj)

                            database.reference.child("stories")
                                    .child(FirebaseAuth.getInstance().uid!!)
                                    .child("statuses")
                                    .push()
                                    .setValue(stories)

                            dialog.dismiss()
                        }
                    }
                }
            }
        }
    }
}