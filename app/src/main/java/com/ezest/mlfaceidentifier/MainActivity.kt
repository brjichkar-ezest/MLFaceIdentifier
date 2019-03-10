package com.ezest.mlfaceidentifier

import android.Manifest
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import android.support.design.widget.Snackbar
import com.ezest.mlfaceidentifier.permissions_listener.SampleMultiplePermissionListener
import com.ezest.mlfaceidentifier.permissions_listener.SamplePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.ezest.mlfaceidentifier.permissions_listener.SampleErrorListener
import com.karumi.dexter.listener.PermissionRequestErrorListener
import android.support.v4.content.ContextCompat
import android.widget.TextView
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.ezest.mlfaceidentifier.permissions_listener.SampleBackgroundThreadPermissionListener
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.karumi.dexter.PermissionToken

class MainActivity : AppCompatActivity(),View.OnClickListener {
    lateinit var inputFName:EditText
    lateinit var inputLName:EditText
    lateinit var inputEmail:EditText
    lateinit var inputMobile:EditText
    lateinit var btnSignUp:Button
    lateinit var inputLayoutFName:TextInputLayout
    lateinit var inputLayoutEmail:TextInputLayout
    lateinit var inputLayoutMobile:TextInputLayout
    lateinit var inputLayoutLName:TextInputLayout
    lateinit var allPermissionsListener: MultiplePermissionsListener
    lateinit var errorListener: PermissionRequestErrorListener
    lateinit var contentViewForPermission:View
    lateinit var cameraPermissionListener:PermissionListener
    lateinit var readStoragePermission:PermissionListener
    lateinit var writeStoragePermission:PermissionListener
    lateinit var mStorageRef: StorageReference
    val VIDEO_CAPTURE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contentViewForPermission=findViewById(android.R.id.content)
        btnSignUp = findViewById(R.id.btn_signup)
        mStorageRef = FirebaseStorage.getInstance().getReference("Uploads")
        inputLayoutFName = findViewById(R.id.input_layout_fname)
        inputLayoutLName = findViewById(R.id.input_layout_lname)
        inputLayoutEmail = findViewById(R.id.input_layout_email)
        inputLayoutMobile = findViewById(R.id.input_layout_mobile)
        inputFName = findViewById(R.id.input_fname)
        inputLName = findViewById(R.id.input_lname)
        inputEmail = findViewById(R.id.input_email)
        inputMobile = findViewById(R.id.input_mobile)

        inputFName.addTextChangedListener(MyTextWatcher(inputFName))
        inputLName.addTextChangedListener(MyTextWatcher(inputLName))
        inputEmail.addTextChangedListener(MyTextWatcher(inputEmail))
        inputMobile.addTextChangedListener(MyTextWatcher(inputMobile))
        btnSignUp.setOnClickListener(this)

        createPermissionListeners()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_signup->validateForm()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            val videoUri = data.data
            if (requestCode == VIDEO_CAPTURE) {
                if (resultCode == Activity.RESULT_OK) {
                    uploadVideoFile(videoUri)
                    //Toast.makeText(this, "Video saved to:\n" + videoUri!!, Toast.LENGTH_LONG).show()
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Video recording cancelled.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to record video", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkPermission(){
        Dexter.withActivity(this)
            .withPermissions(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(allPermissionsListener)
            .withErrorListener(errorListener)
            .check()
    }

    private fun validateForm(){
        if (!validateFName()) {
            return
        }

        if (!validateLName()) {
            return
        }

        if (!validateEmail()) {
            return
        }

        if (!validateMobile()) {
            return
        }

        if (hasCamera()) {
            checkPermission()
        }else{
            Toast.makeText(this,"No camera present to capture the video",Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasCamera(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    private fun proceedAfterPermission() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (takeVideoIntent.resolveActivity(packageManager) != null) {
            takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)
            startActivityForResult(takeVideoIntent, VIDEO_CAPTURE)
        }
    }

    private fun uploadVideoFile(videoFile: Uri) {
        val cR = this@MainActivity.contentResolver
        val mime = MimeTypeMap.getSingleton()
        val mimeType = mime.getExtensionFromMimeType(cR.getType(videoFile))
        val upFile =
            mStorageRef.child("videos/" + inputFName.text.toString() + "_" + inputLName.text.toString() + "_" + inputMobile.text.toString() + "." + mimeType)
        val uploadTask = upFile.putFile(videoFile)
        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }

            // Continue with the task to get the download URL
            upFile.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val downloadURL = downloadUri!!.toString()
                Toast.makeText(this,"Record uploaded successfully",Toast.LENGTH_SHORT).show()
                resetFields();
            } else {
                // Handle failures
                Toast.makeText(this,"Something went wrong while uploading video at firebase",Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun resetFields(){
        inputFName.text.clear()
        inputLName.text.clear()
        inputEmail.text.clear()
        inputMobile.text.clear()
        inputLayoutFName.setErrorEnabled(false)
        inputLayoutLName.setErrorEnabled(false)
        inputLayoutEmail.setErrorEnabled(false)
        inputLayoutMobile.setErrorEnabled(false)
    }
    private fun validateFName(): Boolean {
        if (inputFName.text.toString().trim { it <= ' ' }.isEmpty()) {
            inputLayoutFName.setError(getString(R.string.err_msg_fname))
            requestFocus(inputFName)
            return false
        } else {
            inputLayoutFName.setErrorEnabled(false)
        }

        return true
    }

    private fun validateLName(): Boolean {
        if (inputFName.text.toString().trim { it <= ' ' }.isEmpty()) {
            inputLayoutLName.setError(getString(R.string.err_msg_lname))
            requestFocus(inputLName)
            return false
        } else {
            inputLayoutLName.setErrorEnabled(false)
        }

        return true
    }

    private fun validateEmail(): Boolean {
        val email = inputEmail.text.toString().trim { it <= ' ' }

        if (email.isEmpty() || !isValidEmail(email)) {
            inputLayoutEmail.setError(getString(R.string.err_msg_email))
            requestFocus(inputEmail)
            return false
        } else {
            inputLayoutEmail.setErrorEnabled(false)
        }

        return true
    }

    private fun validateMobile(): Boolean {
        if (inputMobile.text.toString().trim { it <= ' ' }.isEmpty()) {
            inputLayoutMobile.setError(getString(R.string.err_msg_mobile))
            requestFocus(inputMobile)
            return false
        } else {
            inputLayoutMobile.setErrorEnabled(false)
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun requestFocus(view: View) {
        if (view.requestFocus()) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    private inner class MyTextWatcher (private val view: View) : TextWatcher {

        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun afterTextChanged(editable: Editable) {
            when (view.id) {
                R.id.input_fname -> validateFName()
                R.id.input_lname -> validateLName()
                R.id.input_email -> validateEmail()
                R.id.input_mobile -> validateMobile()
            }
        }
    }

    fun showPermissionGranted(permission: String) {

//        Toast.makeText(this,"All permission accepted",Toast.LENGTH_SHORT).show()
    }

    fun showPermissionDenied(permission: String, isPermanentlyDenied: Boolean) {
        //Toast.makeText(this,"All permission denied",Toast.LENGTH_SHORT).show()
    }

    fun proceedIfAllValid(deniedIfAny:Int){
        if(deniedIfAny==0){
            proceedAfterPermission()
        }
    }
    fun showPermissionRationale(token: PermissionToken) {
        AlertDialog.Builder(this).setTitle(R.string.permission_rationale_title)
            .setMessage(R.string.permission_rationale_message)
            .setNegativeButton(android.R.string.cancel, DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                token.cancelPermissionRequest()
            })
            .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                token.continuePermissionRequest()
            })
            .setOnDismissListener(DialogInterface.OnDismissListener { token.cancelPermissionRequest() })
            .show()
    }

    private fun createPermissionListeners(){
        val feedbackViewPermissionListener = SamplePermissionListener(this)
        val feedbackViewMultiplePermissionListener = SampleMultiplePermissionListener(this)
        errorListener = SampleErrorListener()
        allPermissionsListener = CompositeMultiplePermissionsListener(
            feedbackViewMultiplePermissionListener,
            SnackbarOnAnyDeniedMultiplePermissionsListener.Builder.with(contentViewForPermission,
                R.string.all_permissions_denied_feedback
            )
                .withOpenSettingsButton(R.string.permission_rationale_settings_button_text)
                .build()
        )
        readStoragePermission = CompositePermissionListener(
            feedbackViewPermissionListener,
            SnackbarOnDeniedPermissionListener.Builder.with(
                contentViewForPermission,
                R.string.contacts_permission_denied_feedback
            )
                .withOpenSettingsButton(R.string.permission_rationale_settings_button_text)
                .withCallback(object : Snackbar.Callback() {
                    override fun onShown(snackbar: Snackbar?) {
                        super.onShown(snackbar)
                    }

                    override fun onDismissed(snackbar: Snackbar?, event: Int) {
                        super.onDismissed(snackbar, event)
                    }
                })
                .build()
        )

        val dialogOnDeniedPermissionListener = DialogOnDeniedPermissionListener.Builder.withContext(this)
            .withTitle(R.string.audio_permission_denied_dialog_title)
            .withMessage(R.string.audio_permission_denied_dialog_feedback)
            .withButtonText(android.R.string.ok)
            .withIcon(R.mipmap.ic_launcher)
            .build()
        writeStoragePermission = CompositePermissionListener(
            feedbackViewPermissionListener,
            dialogOnDeniedPermissionListener
        )
        cameraPermissionListener = SampleBackgroundThreadPermissionListener(this)

        errorListener = SampleErrorListener()
    }
}
