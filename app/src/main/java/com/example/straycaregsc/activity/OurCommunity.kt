package com.example.straycaregsc.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.example.straycaregsc.R

class OurCommunity : AppCompatActivity() {
    lateinit var ivBackBtnOC:ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_our_community)
        ivBackBtnOC= findViewById(R.id.ivBackBtnOC)
        ivBackBtnOC.setOnClickListener{
            finish()
            startActivity(Intent(this@OurCommunity,HomePageActivity::class.java))

        }

    }
}