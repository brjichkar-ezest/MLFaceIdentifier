package com.ezest.mlfaceidentifier.permissions_listener

import com.ezest.mlfaceidentifier.ui_section.signup_section.SignupActivity
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

open class SamplePermissionListener(private val activity: SignupActivity) : PermissionListener {

    override fun onPermissionGranted(response: PermissionGrantedResponse) {
        activity.showPermissionGranted(response.permissionName)
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse) {
        activity.showPermissionDenied(response.permissionName, response.isPermanentlyDenied)
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken
    ) {
        //activity.showPermissionRationale(token)
    }
}