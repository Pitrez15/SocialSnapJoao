package com.example.socialsnap.ui.home

import android.app.Activity
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.socialsnap.R
import com.example.socialsnap.models.SnapItem
import com.example.socialsnap.helpers.dateToString
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_photo_detail.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PhotoDetailFragment : Fragment() {

    private var bitmap : Bitmap? = null
    var curFile: Uri? = null
    private var date : String? = dateToString(Date())

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private val currentUser = auth.currentUser
    private val storageRef = Firebase.storage.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.activity_photo_detail, container, false)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fabPhoto.setOnClickListener {

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CODE_PHOTO)
        }

        buttonSelectPhoto.setOnClickListener {

            Intent(Intent.ACTION_GET_CONTENT).also {

                it.type = "image/*"
                startActivityForResult(it, REQUEST_CODE_IMAGE_PICK)
            }
        }

        buttonUploadPhoto.setOnClickListener {

            val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

            val baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val uploadTask = imagesRef.putBytes(data)

            uploadTask.addOnFailureListener {
                // Handle unsuccessful uploads

            }.addOnSuccessListener { taskSnapshot ->

                val snap = SnapItem(
                    imagesRef.name,
                    editTextDescription.text.toString(),
                    date,
                    currentUser!!.uid
                )

                db.collection("snaps")
                    .add(snap.toHashMap())
                    .addOnSuccessListener {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Algo correu mal!", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode === Activity.RESULT_OK) {

            if (requestCode == REQUEST_CODE_PHOTO) {

                data?.extras?.let {

                    bitmap = it.get("data") as Bitmap
                    imageViewPhoto.setImageBitmap(bitmap)
                }
            }
            else if (requestCode == REQUEST_CODE_IMAGE_PICK) {

                data?.data?.let {

                    curFile = it
                    imageViewPhoto.setImageURI(it)

                    bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                }
            }
        }
    }

    companion object {

        const val REQUEST_CODE_PHOTO = 23524
        const val REQUEST_CODE_IMAGE_PICK = 0
    }
}