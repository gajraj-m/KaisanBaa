package com.example.kaisanbaa.Adapters


import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kaisanbaa.Activities.ImageViewActivity
import com.example.kaisanbaa.Models.Message
import com.example.kaisanbaa.Models.User
import com.example.kaisanbaa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.collections.ArrayList

class GroupMessageAdapter(val context: Context,
                     private val messages: ArrayList<Message>
) : RecyclerView.Adapter<GroupMessageAdapter.GroupMessageViewHolder>() {



    private val MESSAGE_TYPE_LEFT = 0
    private val MESSAGE_TYPE_RIGHT = 1
    var firebaseUser: FirebaseUser? = null

    class GroupMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtUserName: TextView = view.findViewById(R.id.tvmsg)
        val image : ImageView = view.findViewById(R.id.id_image_msg)
        val senderName : TextView = view.findViewById(R.id.name_of_sender_group)
    }



    override fun getItemViewType(position: Int): Int {
        firebaseUser = FirebaseAuth.getInstance().currentUser
        if (messages[position].senderId == firebaseUser!!.uid) {
            return MESSAGE_TYPE_RIGHT
        } else {
            return MESSAGE_TYPE_LEFT
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMessageViewHolder {
        if (viewType == MESSAGE_TYPE_RIGHT) {
            val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_send_group, parent, false)
            return GroupMessageViewHolder(view)
        } else {
            val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_receive_group, parent, false)
            return GroupMessageViewHolder(view)
        }
    }
    override fun onBindViewHolder(holder: GroupMessageViewHolder, position: Int) {
        val chat = messages[position]

        FirebaseDatabase.getInstance().reference.child("users").child(chat.senderId!!)
                .addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                             val user = snapshot.getValue(User::class.java)
                            // set name of whoever sent msg
                            if((chat.senderId != FirebaseAuth.getInstance().uid)
                                    && position!=0 && (chat.senderId != messages[position-1].senderId)){
                                holder.senderName.visibility = View.VISIBLE
                                holder.senderName.text = user?.name
                            }
                            else {
                                holder.senderName.visibility = View.GONE
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })

        if(chat.imageUrl != ""){
            holder.image.visibility = View.VISIBLE
            Glide.with(context)
                    .load(chat.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(holder.image)
            holder.txtUserName.visibility = View.GONE
        }
        else{               // if else statement not written then item lists behave randomly
            holder.txtUserName.visibility = View.VISIBLE
            holder.image.visibility = View.GONE
            holder.txtUserName.text = chat.msg
        }
        holder.itemView.setOnClickListener {
            if(chat.imageUrl != ""){
                val intent = Intent(context, ImageViewActivity::class.java)
                intent.putExtra("imageUrl", chat.imageUrl)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = messages.size

}

