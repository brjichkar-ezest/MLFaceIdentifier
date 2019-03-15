package com.ezest.mlfaceidentifier.ui_section.splash_section

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ezest.mlfaceidentifier.ui_section.signup_section.SignupActivity
import android.content.Intent



class SpalshActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainActIntent = Intent(this, SignupActivity::class.java)
        startActivity(mainActIntent)
        finish()
    }
}