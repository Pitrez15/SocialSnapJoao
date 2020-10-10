package com.example.socialsnap.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.example.socialsnap.LoginActivity
import com.example.socialsnap.PhotoDetailFragment
import com.example.socialsnap.R
import com.example.socialsnap.SnapItem
import com.example.socialsnap.ui.stringToDate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_photo_detail.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.tasks.await
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    var snaps : MutableList<SnapItem> = ArrayList<SnapItem>()
    var snapAdapter : SnapAdapter? = null

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        snapAdapter = SnapAdapter()
        listViewSnaps.adapter = snapAdapter

        val docRef = db.collection("snaps")

        docRef.get().addOnSuccessListener { result ->

            snaps.clear()

            for (document in result) {

                Log.d("exist", "DocumentSnapshot data: ${document.data}")

                snaps.add(SnapItem(document.data.getValue("filePath").toString(),
                            document.data.getValue("description").toString(),
                            document.data.getValue("date").toString(),
                            document.data.getValue("userId").toString(),))
            }
            snapAdapter?.notifyDataSetChanged()

        }.addOnFailureListener { exception ->

            Log.d("errordb", "get failed with ", exception)
        }

        fabNewPhoto.setOnClickListener {

            val action = HomeFragmentDirections.actionNavigationHomeToPhotoDetailFragment()
            it.findNavController().navigate(action)
        }

        buttonLogout.setOnClickListener {

            Firebase.auth.signOut()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }
    }

    inner class SnapAdapter : BaseAdapter() {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            val rowView = layoutInflater.inflate(R.layout.view_row_photos, parent, false)

            val textViewDate = rowView.findViewById<TextView>(R.id.textViewSnapDate)
            val textViewDescription = rowView.findViewById<TextView>(R.id.textViewSnapDescription)
            val textViewUser = rowView.findViewById<TextView>(R.id.textViewSnapUser)

            textViewDate.text = snaps[position].date
            textViewDescription.text = snaps[position].description
            textViewUser.text = snaps[position].userId

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