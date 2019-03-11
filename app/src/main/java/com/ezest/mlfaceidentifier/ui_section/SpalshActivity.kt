package com.ezest.mlfaceidentifier.ui_section

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ezest.mlfaceidentifier.MainActivity
import android.content.Intent



class SpalshActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainActIntent = Intent(this, MainActivity::class.java)
        startActivity(mainActIntent)
        finish()
    }
}