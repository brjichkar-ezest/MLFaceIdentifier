package com.ezest.mlfaceidentifier.permissions_listener

import android.os.Handler
import android.os.Looper
import com.ezest.mlfaceidentifier.MainActivity
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest

/**
 * Sample listener that shows how to handle permission request callbacks on a background thread
 */
class SampleBackgroundThreadPermissionListener(activity: MainActivity) : SamplePermissionListener(activity) {

    private val handler = Handler(Looper.getMainLooper())

    override fun onPermissionGranted(response: PermissionGrantedResponse) {
        handler.post { super@SampleBackgroundThreadPermissionListener.onPermissionGranted(response) }
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse) {
        handler.post { super@SampleBackgroundThreadPermissionListener.onPermissionDenied(response) }
    }

    override fun onPermissionRationaleShouldBeShown(
        permission: PermissionRequest,
        token: PermissionToken
    ) {
        handler.post {
            super@SampleBackgroundThreadPermissionListener.onPermissionRationaleShouldBeShown(
                permission, token
            )
        }
    }
}