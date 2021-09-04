package com.example.kaisanbaa.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kaisanbaa.Activities.ChatActivity
import com.example.kaisanbaa.R
import com.example.kaisanbaa.Models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UserAdapter (
        private val context : Context,
        private val user : ArrayList<User>
        ): RecyclerView.Adapter<UserAdapter.UserViewHolder> (){

    class UserViewHolder (view : View): RecyclerView.ViewHolder(view) {
        val dp : ImageView = view.findViewById(R.id.id_profile_dp)
        val name : TextView = view.findViewById(R.id.id_name)
        val msg : TextView = view.findViewById(R.id.id_msg)
        val time : TextView = view.findViewById(R.id.id_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.row_conversation, parent, false)
        return UserViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = user[position]
        holder.name.text = item.name

        val senderRoom = FirebaseAuth.getInstance().uid + item.uid
        FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(senderRoom)
                .addValueEventListener(object : ValueEventListener{
            @SuppressLint("SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    val lastMsg = snapshot.child("lastMsg").getValue(String:: class.java)
                val time = snapshot.child("lastMsgTime").getValue(Long::class.java)
                holder.msg.text = lastMsg
                    val simpleDateFormat = SimpleDateFormat("hh:mm a")
                    val dateTime : String = simpleDateFormat.format(time)
                    holder.time.text = dateTime
                }
                else holder.msg.text = context.getString(R.string.tap_to_chat)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        Glide.with(context).load(item.imageUri).placeholder(R.drawable.avatar)
                .into(holder.dp)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("name", item.name)
            intent.putExtra("profile image", item.imageUri)
            intent.putExtra("uid", item.uid)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = user.size
}