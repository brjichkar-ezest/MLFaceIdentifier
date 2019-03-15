package com.ezest.mlfaceidentifier.permissions_listener


import com.ezest.mlfaceidentifier.ui_section.signup_section.SignupActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class SampleMultiplePermissionListener(private val activity: SignupActivity) : MultiplePermissionsListener {

    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
        for (response in report.grantedPermissionResponses) {
            activity.showPermissionGranted(response.permissionName)
        }

        for (response in report.deniedPermissionResponses) {
            activity.showPermissionDenied(response.permissionName, response.isPermanentlyDenied)
        }

        activity.proceedIfAllValid(report.deniedPermissionResponses.count())
    }

    override fun onPermissionRationaleShouldBeShown(
        permissions: List<PermissionRequest>,
        token: PermissionToken
    ) {
        //activity.showPermissionRationale(token)
    }
}