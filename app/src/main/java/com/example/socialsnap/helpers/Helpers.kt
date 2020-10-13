package com.example.socialsnap.helpers

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
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

fun saveImageToCard(context: Context, bitmap : Bitmap) : File {


    var photoDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    Log.d("photocatalog", photoDir.toString() )

    photoDir = File(photoDir, "${UUID.randomUUID()}.jpg")

    Log.d("photocatalog", photoDir.toString() )

    try {
        val stream: OutputStream = FileOutputStream(photoDir)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.flush()
        stream.close()
    } catch (e: IOException){
        e.printStackTrace()
    }

    return photoDir
}

internal var imagePath: String? = ""

private fun saveImage(finalBitmap: Bitmap) {

    val root = Environment.getExternalStorageDirectory().toString()
    val myDir = File(root + "/capture_photo")
    myDir.mkdirs()
    val generator = Random()
    var n = 10000
    n = generator.nextInt(n)
    val OutletFname = "Image-$n.jpg"
    val file = File(myDir, OutletFname)
    if (file.exists()) file.delete()
    try {
        val out = FileOutputStream(file)
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        imagePath = file.absolutePath
        out.flush()
        out.close()


    } catch (e: Exception) {
        e.printStackTrace()

    }

}