package com.ezest.mlfaceidentifier.ui_section.custom_camera

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AppCompatDelegate
import com.ezest.mlfaceidentifier.R

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        //supportActionBar!!.hide()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (null == savedInstanceState) {
            var cameraFragment:CameraFragment= CameraFragment()
            fragmentManager.beginTransaction()
                .replace(R.id.container, cameraFragment)
                .commit()
        }
    }
}
