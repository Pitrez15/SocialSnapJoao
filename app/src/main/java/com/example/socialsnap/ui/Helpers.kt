package com.example.socialsnap.ui

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

fun dateToString (date : Date) : String{

    val formatter = SimpleDateFormat("dd MMMM yyyy hh:mm")

    return formatter.format(date)
}

fun stringToDate (dateStr: String) : Date {

    val formatter = SimpleDateFormat("dd MMMM yyyy hh:mm")
    val date = formatter.parse(dateStr)

    return date
}