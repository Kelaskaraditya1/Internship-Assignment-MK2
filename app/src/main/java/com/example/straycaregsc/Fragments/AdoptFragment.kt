package com.example.straycaregsc.Fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.straycaregsc.Adapters.AdoptPetAdapter
import com.example.straycaregsc.Models.AdoptArrayModel // Assuming you still need this temporarily?
import com.example.straycaregsc.Models.AdoptPostsModel // You need this model class
import com.example.straycaregsc.Models.UserModel
import com.example.straycaregsc.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore // Still used for fetching user details

class AdoptFragment : Fragment() {
    // Removed adoptArrayModel as it's not used for fetching from RTDB
    lateinit var rcvAdoptPet: RecyclerView
    lateinit var llContactUser: LinearLayout
    // lateinit var llDetails: LinearLayout // This was declared but never initialized/used
    lateinit var tvOwnerMobile: TextView
    lateinit var tvOwnerName: TextView
    lateinit var tvOwnerEmail: TextView
    lateinit var ivBackAF: ImageView
    lateinit var ivCall: ImageView
    lateinit var ivEmail: ImageView
    lateinit var pbAdoptFragment: ProgressBar
    var ownerDetails = UserModel()
    private var adoptionPostListener: ValueEventListener? = null
    private val adoptionPostsRef = FirebaseDatabase.getInstance().getReference("adoption posts")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initialiseVariables() is usually called after view creation
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewOfAdopt = inflater.inflate(R.layout.fragment_adopt, container, false)
        // Initialize views here
        rcvAdoptPet = viewOfAdopt.findViewById(R.id.rcvAdoptPet)
        llContactUser = viewOfAdopt.findViewById(R.id.llContactUser)
        tvOwnerEmail = viewOfAdopt.findViewById(R.id.tvOwnerEmail)
        tvOwnerName = viewOfAdopt.findViewById(R.id.tvOwnerName)
        tvOwnerMobile = viewOfAdopt.findViewById(R.id.tvOwnerMobile)
        pbAdoptFragment = viewOfAdopt.findViewById(R.id.pbAdoptFragment)
        ivCall = viewOfAdopt.findViewById(R.id.ivCall)
        ivEmail = viewOfAdopt.findViewById(R.id.ivEmail)
        ivBackAF = viewOfAdopt.findViewById(R.id.ivBackAF)

        // initialiseVariables() // Don't need this if using RTDB fetch
        fetchPosts() // Start fetching data

        ivBackAF.setOnClickListener {
            hideOwnerContact()
            showRCV()
        }
        return viewOfAdopt
    }

    // Removed initialiseVariables as it only initialized the unused adoptArrayModel

    private fun fetchPosts() {
        Log.i("adi", "fetchAdoptionPosts called (Realtime DB)")
        showProgressBar()

        // Define the listener
        adoptionPostListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                hideProgressBar()
                val adoptionPostsList = mutableListOf<AdoptPostsModel>() // Use your actual adoption post model

                for (postSnapshot in dataSnapshot.children) {
                    try {
                        val post = postSnapshot.getValue(AdoptPostsModel::class.java)
                        post?.let {
                            adoptionPostsList.add(it)
                        }
                    } catch (e: Exception) {
                        Log.e("adi", "Error converting adoption post snapshot: ${e.message}")
                    }
                }

                if (adoptionPostsList.isEmpty()) {
                    Log.i("adi", "Adoption posts result empty")
                } else {
                    Log.i("adi", "Fetched ${adoptionPostsList.size} adoption posts successfully")
                }
                setPostsInRCV(adoptionPostsList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                hideProgressBar()
                Log.w("adi", "loadAdoptionPosts:onCancelled", databaseError.toException())
                // Use requireContext() for safety in Fragments
                context?.let { // Use context which is available in Fragment lifecycle methods
                    Toast.makeText(it, "Error encountered.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Attach the listener
        adoptionPostsRef.addValueEventListener(adoptionPostListener!!)
    } // <-- *** THIS IS WHERE THE fetchPosts() FUNCTION ENDS ***

    // --- IMPORTANT: onDestroyView() is preferred for Fragments ---
    override fun onDestroyView() { // Use onDestroyView in Fragments
        super.onDestroyView()
        // Remove the listener when the view is destroyed
        adoptionPostListener?.let {
            adoptionPostsRef.removeEventListener(it)
        }
        Log.d("AdoptFragment", "Listener removed in onDestroyView")
    }
    // --- Lifecycle method moved outside fetchPosts() ---

    // Updated parameter type to List<AdoptionPostModel>
    private fun setPostsInRCV(adoptPostsArray: List<AdoptPostsModel>?) {
        if (context == null || adoptPostsArray == null) {
            Log.e("adi", "Context is null or posts array is null in setPostsInRCV")
            return // Prevent crash if context or list is null
        }
        rcvAdoptPet.layoutManager = LinearLayoutManager(requireContext()) // Use requireContext()
        // Make sure AdoptPetAdapter accepts List<AdoptionPostModel>
        rcvAdoptPet.adapter = AdoptPetAdapter(adoptPostsArray, object : AdoptPetAdapter.Listener {
            override fun onPostClicked(position: Int) {
                Log.i("adi", "onPostClicked: showing details of post $position ")
                // Implement detail view logic if needed
            }

            override fun onContactUserClicked(userID: String) {
                var uid = userID.replace(" ", "")
                Log.i("adi", "Contact clicked for UID: $uid")
                showProgressBar() // Show progress while fetching user

                FirebaseFirestore.getInstance().collection("Users")
                    .document(uid)
                    .get()
                    .addOnCompleteListener { task -> // Renamed 'it' to 'task' for clarity
                        hideProgressBar() // Hide progress bar regardless of outcome
                        if (task.isSuccessful) {
                            val document = task.result
                            if (document != null && document.exists()) {
                                Log.i("adi", "User data found for UID: $uid")
                                ownerDetails = document.toObject(UserModel::class.java)!!
                                Log.i("adi", "Fetched username: ${ownerDetails.userName}, Contact: ${ownerDetails.contactNo}")
                                setValuesOfOwner()
                                showOwnerContact()
                                hideRCV()
                            } else {
                                Log.w("adi", "No user document found for UID: $uid")
                                Toast.makeText(context, "Owner details not found.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("adi", "Error fetching user details for UID $uid:", task.exception)
                            Toast.makeText(context, "Error fetching owner details.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        })
    }

    // Removed fetchUserDetails as the logic is inside onContactUserClicked

    private fun setValuesOfOwner() {
        tvOwnerName.text = ownerDetails.userName
        tvOwnerMobile.text = ownerDetails.contactNo
        tvOwnerEmail.text = ownerDetails.email

        ivCall.setOnClickListener {
            val phoneNumber = tvOwnerMobile.text.toString()
            if (phoneNumber.isNotEmpty()) {
                val u: Uri = Uri.parse("tel:$phoneNumber")
                val i = Intent(Intent.ACTION_DIAL, u)
                try {
                    startActivity(i)
                } catch (e: SecurityException) {
                    Log.e("adi", "Permission error dialing: ${e.message} ")
                    Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("adi", "Error opening dialer: ${e.message}")
                    Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Phone number not available.", Toast.LENGTH_SHORT).show()
            }
        }

        ivEmail.setOnClickListener {
            val emailAddress = ownerDetails.email
            if (emailAddress != null && emailAddress.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:") // only email apps should handle this
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress)) // Use arrayOf for multiple recipients if needed
                    putExtra(Intent.EXTRA_SUBJECT, "Inquiry about adopting your pet from StrayCare")
                }
                try {
                    // Check if there's an app to handle the intent
                    if (intent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("adi", "Error opening email client: ${e.message}")
                    Toast.makeText(context, "Could not open email app.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Email address not available.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- UI Visibility Helper Functions ---
    private fun showRCV() {
        rcvAdoptPet.visibility = View.VISIBLE
    }

    private fun hideRCV() {
        rcvAdoptPet.visibility = View.GONE
    }

    private fun showOwnerContact() {
        llContactUser.visibility = View.VISIBLE
    }

    private fun hideOwnerContact() {
        llContactUser.visibility = View.GONE
    }

    private fun showProgressBar() {
        pbAdoptFragment.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        pbAdoptFragment.visibility = View.GONE
    }
} // <-- *** THIS IS WHERE THE AdoptFragment CLASS ENDS ***