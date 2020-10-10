package com.example.socialsnap

import com.example.socialsnap.ui.dateToString
import com.example.socialsnap.ui.stringToDate
import java.util.*
import kotlin.collections.HashMap

class SnapItem {

    var filePath : String? = null
    var description : String? = null
    var date : String?   = null
    var userId : String? = null

    constructor(
        filePath : String?,
        description : String?,
        date : String?,
        userId : String?
    ){
        this.filePath = filePath
        this.description = description
        this.date = date
        this.userId = userId
    }

    fun toHashMap() : HashMap<String, Any?>{

        val hashMap = HashMap<String, Any?>()
        hashMap["filePath"] = filePath
        hashMap["description"] = description
        hashMap["date"] = date
        hashMap["userId"] = userId

        return hashMap
    }

    companion object {

        fun fromHashMap(hashMap : HashMap<String?, Any?>) : SnapItem {

            val item = SnapItem(
                hashMap["filepath"].toString(),
                hashMap["description"].toString(),
                hashMap["date"].toString(),
                hashMap["userId"].toString()
            )
            return item
        }
    }
}