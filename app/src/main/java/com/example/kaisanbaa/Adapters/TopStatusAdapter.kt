    package com.example.kaisanbaa.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devlomi.circularstatusview.CircularStatusView
import com.example.kaisanbaa.Activities.MainActivity
import com.example.kaisanbaa.Models.Status
import com.example.kaisanbaa.Models.UserStatus
import com.example.kaisanbaa.R
import de.hdodenhof.circleimageview.CircleImageView
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory


    class TopStatusAdapter(
            private val context: Context,
            private val userStatuses: ArrayList<UserStatus>
    ) : RecyclerView.Adapter<TopStatusAdapter.TopStatusViewHolder> (){

    class TopStatusViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val circleImageView : CircleImageView = view.findViewById(R.id.circleImageView)
        val circleStatusView : CircularStatusView = view.findViewById(R.id.circular_status_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopStatusViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_status, parent, false)
        return TopStatusViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: TopStatusViewHolder, position: Int) {

        val userStatus = userStatuses[position]
        // set last updated image into image view
        val lastStatus : Status = userStatus.statuses!!.get(userStatus.statuses!!.size - 1 )
        Glide.with(context).load(lastStatus.imageUrl).into(holder.circleImageView)
        holder.circleStatusView.setPortionsCount(userStatus.statuses!!.size)

        holder.circleStatusView.setOnClickListener {
            val myStories : ArrayList<MyStory> = arrayListOf()
            for(status in userStatus.statuses!!){
                myStories.add(MyStory(status.imageUrl))
            }
            StoryView.Builder((context as MainActivity).supportFragmentManager)
                    .setStoriesList(myStories) // Required
                    .setStoryDuration(5000) // Default is 2000 Millis (2 Seconds)
                    .setTitleText(userStatus.name) // Default is Hidden
                    .setSubtitleText(" ") // Default is Hidden
                    .setTitleLogoUrl(userStatus.profileImage) // Default is Hidden
                    .setStoryClickListeners(object : StoryClickListeners {
                        override fun onDescriptionClickListener(position: Int) {
                        }

                        override fun onTitleIconClickListener(position: Int) {

                        }
                    }) // Optional Listeners
                    .build() // Must be called before calling show method
                    .show()
        }
    }

    override fun getItemCount(): Int {
       return  userStatuses.size
    }

}