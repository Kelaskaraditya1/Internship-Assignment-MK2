package com.example.straycaregsc.Fragments


import android.content.Intent
import android.os.Bundle
import android.provider.Settings.Global
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.straycaregsc.Adapters.PostsAdapter
import com.example.straycaregsc.Models.GlobalPostsModel
import com.example.straycaregsc.Models.PostModel
import com.example.straycaregsc.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.DataSnapshot // <-- Import
import com.google.firebase.database.DatabaseError // <-- Import


class HomeFragment : Fragment() {

    lateinit var rcvPostsHF :RecyclerView
    lateinit var pbHF :ProgressBar
    lateinit var posts : GlobalPostsModel
    private var postListener: ValueEventListener? = null
    private val postsRef = FirebaseDatabase.getInstance().getReference("posts")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    private fun initialiseVariables() {
        posts = GlobalPostsModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewOfLayout = inflater.inflate(R.layout.fragment_home, container, false)
        initialiseVariables()
        rcvPostsHF =viewOfLayout.findViewById(R.id.rcvPostsHF)
        pbHF =viewOfLayout.findViewById(R.id.pbHF)
        fetchPosts()


        return viewOfLayout


    }

    private fun setPostsRCV(postArray:List<PostModel>) {

        rcvPostsHF.layoutManager = LinearLayoutManager(parentFragment?.context)
        rcvPostsHF.adapter = PostsAdapter(postArray,object:PostsAdapter.Listener{
            override fun shareClicked(caption: String) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type="text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Please checkout this post on our app: ${caption}" )
                startActivity(Intent.createChooser(shareIntent,"Share via"))
            }
            override fun likeClicked(position:Int,likes:Int) {
                fetchPosts()
                posts.postsArray[position].likes = posts.postsArray[position].likes + likes
                Toast.makeText(context,"Liked Successfully",Toast.LENGTH_SHORT).show()
                updatePosts(posts)
            }
        })
    }
    private fun updatePosts(posts: GlobalPostsModel){
        FirebaseFirestore.getInstance().collection("posts")
            .document("global posts")
            .set(posts)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    Log.i("adi", "liked ")
                }
                else{
                    Log.i("adi", "error ")
                }
            }

    }

    private fun fetchPosts(){
//        Log.i("adi", "post fetched is called")
//
//        showPB()
//
//        FirebaseFirestore.getInstance().collection("posts").document("global posts")
//            .get()
//            .addOnCompleteListener{
//                if(it.isSuccessful){
//                    hidePB()
//                    Log.i("adi", "post fetched is successfull")
//                    if(it.result.exists()){
//                        Log.i("adi", "post fetched result exists")
//
//                        try {
//                             posts= it.result.toObject(GlobalPostsModel::class.java)!!
//                            setPostsRCV(posts.postsArray)
//                        }
//                        catch (e:Exception){
//                            Log.i("adi", "error:${e}")
//                        }
//                    }
//                    else{
//                        Log.i("adi", "no result exists")
//                    }
//                }
//                else{
//                    hidePB()
//                    Log.i("adi", "not able to fetch posts")
//                }
//            }

        Log.i("adi", "fetchPosts called (Realtime DB)")
        showPB()

        // Define the listener
        postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                hidePB()
                Log.i("adi", "Realtime DB data received")
                val postsList = mutableListOf<PostModel>()

                // Loop through each child node under "posts"
                for (postSnapshot in dataSnapshot.children) {
                    try {
                        // Try to convert the snapshot into a PostModel object
                        val post = postSnapshot.getValue(PostModel::class.java)
                        post?.let {
                            // If conversion is successful, add it to the list
                            postsList.add(it)
                        }
                    } catch (e: Exception) {
                        Log.e("adi", "Error converting post snapshot: ${e.message}")
                    }
                }

                Log.i("adi", "Fetched ${postsList.size} posts successfully")
                // Pass the list directly to your RecyclerView setup function
                setPostsRCV(postsList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                hidePB()
                Log.w("adi", "loadPosts:onCancelled", databaseError.toException())
                Toast.makeText(requireContext(), "Failed to load posts.", Toast.LENGTH_SHORT).show()
            }
        }

        // Attach the listener to the "posts" node
        postsRef.addValueEventListener(postListener!!) // Use !! because we just assigned it
    }

    // --- IMPORTANT: Add this to your Activity/Fragment to prevent memory leaks ---
    override fun onDestroy() {
        super.onDestroy()
        // Remove the listener when the view is destroyed
        postListener?.let {
            postsRef.removeEventListener(it)
        }

    }



    private fun showPB(){
        pbHF.visibility = View.VISIBLE
    }
    private fun hidePB(){
        pbHF.visibility = View.GONE
    }

}
