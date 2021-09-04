package com.example.kaisanbaa.Models

data class UserStatus(var name : String? =null,
                      var profileImage : String ?=null,
                      var lastUpdated : Long?=0,
                      var statuses: ArrayList<Status>?=null
                     )
