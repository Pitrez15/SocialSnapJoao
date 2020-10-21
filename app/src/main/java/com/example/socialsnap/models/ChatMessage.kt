package com.example.socialsnap.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ChatMessage (
    val text : String? = null,
    val fromId : String? = null,
    val toId : String? = null,
    val timeStamp : Long? = null): Parcelable {

    constructor() : this("", "", "", -1)
}