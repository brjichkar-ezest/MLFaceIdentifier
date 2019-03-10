package com.ezest.mlfaceidentifier.permissions_listener

import android.util.Log
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionRequestErrorListener

class SampleErrorListener : PermissionRequestErrorListener {
    override fun onError(error: DexterError) {
        Log.e("Dexter", "There was an error: $error")
    }
}