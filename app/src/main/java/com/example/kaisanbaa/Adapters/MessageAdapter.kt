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
import com.example.kaisanbaa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlin.collections.ArrayList

class MessageAdapter(val context: Context,
                     private val messages: ArrayList<Message>
                      ) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {



    private val MESSAGE_TYPE_LEFT = 0
    private val MESSAGE_TYPE_RIGHT = 1
    var firebaseUser: FirebaseUser? = null

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtUserName: TextView = view.findViewById(R.id.tvmsg)
        val image : ImageView = view.findViewById(R.id.id_image_msg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        if (viewType == MESSAGE_TYPE_RIGHT) {
            val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_send, parent, false)
            return MessageViewHolder(view)
        } else {
            val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_receive, parent, false)
            return MessageViewHolder(view)
        }

    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val chat = messages[position]

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

    override fun getItemViewType(position: Int): Int {
        firebaseUser = FirebaseAuth.getInstance().currentUser
        if (messages[position].senderId == firebaseUser!!.uid) {
            return MESSAGE_TYPE_RIGHT
        } else {
            return MESSAGE_TYPE_LEFT
        }

    }
}

