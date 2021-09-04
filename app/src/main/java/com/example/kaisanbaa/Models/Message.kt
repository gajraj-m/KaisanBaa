package com.example.kaisanbaa.Models

data class Message(val msgId : String?= null,
                   var msg : String?=null,
                   val senderId : String?=null,
                   val timeStamp: Long? = 0,
                   val feeling : Int? = 0,
                   val imageUrl : String?=null)
