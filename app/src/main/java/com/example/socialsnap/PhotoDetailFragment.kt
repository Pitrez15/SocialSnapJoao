package com.example.socialsnap

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.socialsnap.ui.dateToString
import com.example.socialsnap.ui.saveImageToCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_photo_detail.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.spi.FileSystemProvider
import java.util.*

class PhotoDetailFragment : Fragment() {

    val REQUEST_PERM_WRITE_STORAGE = 102
    private val CAPTURE_PHOTO = 104
    internal var imagePath: String? = ""


    private var bitmap : Bitmap? = null
    var curFile: Uri? = null
    private var date : String? = dateToString(Date())

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val currentUser = auth.currentUser
    private val imageRef = Firebase.storage.reference


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

            var filename = editTextDescription.text.toString()

            imageRef.child("images/$filename").putFile(curFile!!)

            val snap = SnapItem("gs://socialsnappitrez-c1943.appspot.com/images/$filename", editTextDescription.text.toString(), date, currentUser!!.uid, null)

            db.collection("snaps").add(snap.toHashMap()).addOnSuccessListener {

                    requireActivity().supportFragmentManager.popBackStack()

            }.addOnFailureListener {

                Toast.makeText(requireContext(), "Something Wrong!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode === Activity.RESULT_OK) {

            if (requestCode == REQUEST_CODE_PHOTO) {

                data?.extras?.let {

                    bitmap = it.get("data") as Bitmap
                    saveImage(bitmap!!)
                    imageViewPhoto.setImageBitmap(bitmap!!)
                }
            }
            else if (requestCode == REQUEST_CODE_IMAGE_PICK) {

                data?.data?.let {
                    curFile = it
                    imageViewPhoto.setImageURI(it)
                }
            }
        }
    }

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
        }
        catch (e: Exception) {

            e.printStackTrace()
        }
    }

    companion object {

        const val REQUEST_CODE_PHOTO = 23524
        const val REQUEST_CODE_IMAGE_PICK = 0
    }
}