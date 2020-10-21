package com.example.socialsnap.ui.home

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.socialsnap.R
import com.example.socialsnap.models.SnapItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_dashboard.*
import java.io.ByteArrayInputStream

class PersonalSnapsFragment : Fragment() {

    var snaps : MutableList<SnapItem> = ArrayList<SnapItem>()
    var userSnapAdapter : UserSnapAdapter? = null

    private val auth: FirebaseAuth = Firebase.auth
    private val currentUser = auth.currentUser
    private val storageRef = Firebase.storage.reference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userSnapAdapter = UserSnapAdapter()
        listViewUserSnaps.adapter = userSnapAdapter

        val db = FirebaseFirestore.getInstance()

        db.collection("snaps").whereEqualTo("userId", "${currentUser!!.email}")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                snaps.clear()

                if (querySnapshot != null) {

                    for(d in querySnapshot){

                        snaps.add(SnapItem.fromHashMap(d.data as HashMap<String, Any?>))
                    }
                }

                userSnapAdapter?.notifyDataSetChanged()
            }
    }

    inner class UserSnapAdapter : BaseAdapter() {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            val rowView = layoutInflater.inflate(R.layout.view_row_photos, parent, false)

            val textViewDate = rowView.findViewById<TextView>(R.id.textViewSnapDate)
            val textViewDescription = rowView.findViewById<TextView>(R.id.textViewSnapDescription)
            val textViewUser = rowView.findViewById<TextView>(R.id.textViewSnapUser)
            val imageViewSnap = rowView.findViewById<ImageView>(R.id.imageViewSnap)

            textViewDate.text = snaps[position].date
            textViewDescription.text = snaps[position].description
            textViewUser.text = snaps[position].userId

            val imagesRef = storageRef.child("images/${snaps[position].filePath}")

            val ONE_MEGABYTE: Long = 1024 * 1024

            imagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {

                val bais = ByteArrayInputStream(it)

                imageViewSnap.setImageBitmap(BitmapFactory.decodeStream(bais))
            }.addOnFailureListener {
                // Handle any errors
            }

            return rowView
        }

        override fun getItem(position: Int): Any {

            return snaps[position]
        }

        override fun getItemId(position: Int): Long {

            return 0
        }

        override fun getCount(): Int {

            return snaps.size
        }
    }
}