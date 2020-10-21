package com.example.socialsnap.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User (
    var uid : String? = null,
    var username : String? = null,
    var profileImageUrl : String? = null,
    var email : String? = null,
    var token : String? = null): Parcelable {

    constructor() : this("", "", "", "", "")
}