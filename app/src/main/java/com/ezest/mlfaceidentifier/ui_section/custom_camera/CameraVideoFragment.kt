package com.ezest.mlfaceidentifier.ui_section.custom_camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.ezest.mlfaceidentifier.R
import com.ezest.mlfaceidentifier.ui_section.base_section.BaseFragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


abstract class CameraVideoFragment : BaseFragment() {
    private val TAG = "CameraVideoFragment"

    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    var INVERSE_ORIENTATIONS = SparseIntArray()
    var DEFAULT_ORIENTATIONS = SparseIntArray()

    private var mCurrentFile: File? = null

    private val VIDEO_DIRECTORY_NAME = "MLFaceIdentifier"

    /**
     * An [AutoFitTextureView] for camera preview.
     */
    private var mTextureView: AutoFitTextureView? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null

    /**
     * A reference to the current [CameraCaptureSession] for
     * preview.
     */
    private var mPreviewSession: CameraCaptureSession? = null

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

    }

    /**
     * The [Size] of camera preview.
     */
    private var mPreviewSize: Size? = null

    /**
     * The [Size] of video recording.
     */
    private var mVideoSize: Size? = null

    /**
     * MediaRecorder
     */
    private var mMediaRecorder: MediaRecorder? = null

    /**
     * Whether the app is recording video now
     */
    var mIsRecordingVideo: Boolean = false

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var mBackgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            startPreview()
            mCameraOpenCloseLock.release()
            if (null != mTextureView) {
                configureTransform(mTextureView!!.width, mTextureView!!.height)
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
            val activity = activity
            activity?.finish()
        }

    }
    private var mSensorOrientation: Int? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null


    /**
     * In this sample, we choose a video size with 3x4 for  aspect ratio. for more perfectness 720 as well Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size 1080p,720px
     */
    private fun chooseVideoSize(choices: Array<Size>): Size {
        for (size in choices) {
            if (1920 == size.width && 1080 == size.height) {
                return size
            }
        }
        for (size in choices) {
            if (size.width <= 1080) {
                return size
            }
        }


        /*
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        */
        Log.e(TAG, "Couldn't find any suitable video size")
        return choices[choices.size - 1]
    }

    /**
     * Given `choices` of `Size`s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     * class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(
        choices: Array<Size>, textureViewWidth: Int,
        textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
    ): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough = ArrayList<Size>()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
                option.height == option.width * h / w
            ) {
                if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size > 0) {
            return Collections.min(bigEnough, CompareSizesByArea())
        } else if (notBigEnough.size > 0) {
            return Collections.max(notBigEnough, CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            return choices[0]
        }
    }

    /**
     * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int, aspectRatio: Size): Size {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.height == option.width * h / w &&
                option.width >= width && option.height >= height
            ) {
                bigEnough.add(option)
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size > 0) {
            return Collections.min(bigEnough, CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            return choices[0]
        }
    }

    abstract fun getTextureResource(): Int


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mTextureView = view.findViewById<AutoFitTextureView>(getTextureResource())

        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0)
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90)
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180)
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270)

        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0)
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90)
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180)
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        requestPermission()
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    protected fun getCurrentFile(): File? {
        return mCurrentFile
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    /**
     * Requesting permissions storage, audio and camera at once
     */
    fun requestPermission() {
        Dexter.withActivity(activity).withPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted or not
                    if (report.areAllPermissionsGranted()) {
                        if (mTextureView!!.isAvailable) {
                            openCamera(mTextureView!!.width, mTextureView!!.height)
                        } else {
                            mTextureView!!.surfaceTextureListener = mSurfaceTextureListener
                        }
                    }
                    // check for permanent denial of any permission show alert dialog
                    if (report.isAnyPermissionPermanentlyDenied) {
                        // open Settings activity
                        showSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).withErrorListener { error ->
                Toast.makeText(
                    activity.applicationContext,
                    "Error occurred! ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .onSameThread()
            .check()
    }

    /**
     * Showing Alert Dialog with Settings option in case of deny any permission
     */
    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.message_need_permission))
        builder.setMessage(getString(R.string.message_permission))
        builder.setPositiveButton(getString(R.string.title_go_to_setting)) { dialog, which ->
            dialog.cancel()
            openSettings()
        }
        builder.show()

    }

    // navigating settings app
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }

    /**
     * Tries to open a [CameraDevice]. The result is listened by `mStateCallback`.
     */
    private fun openCamera(width: Int, height: Int) {
        val activity = activity
        if (null == activity || activity.isFinishing) {
            return
        }
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            Log.d(TAG, "tryAcquire")
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            /**
             * default front camera will activate
             */
            var cameraId = manager.cameraIdList[0]
            for (i in 0 until manager.cameraIdList.size) {
                val cameraCharacteristics = manager.getCameraCharacteristics(manager.cameraIdList[i])
                    ?: throw NullPointerException("No camera with id $cameraId")
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = manager.cameraIdList[i]
                }
            }

            //String cameraId = manager.getCameraIdList()[1];

            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            if (map == null) {
                throw RuntimeException("Cannot get available preview/video sizes")
            }
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            mPreviewSize =
                chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height, mVideoSize!!)

            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView!!.setAspectRatio(mPreviewSize!!.width, mPreviewSize!!.height)
            } else {
                mTextureView!!.setAspectRatio(mPreviewSize!!.height, mPreviewSize!!.width)
            }
            configureTransform(width, height)
            mMediaRecorder = MediaRecorder()
            if (ActivityCompat.checkSelfPermission(
                    getActivity(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                requestPermission()
                return
            }
            manager.openCamera(cameraId, mStateCallback, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "openCamera: Cannot access the camera.")
        } catch (e: NullPointerException) {
            Log.e(TAG, "Camera2API is not supported on the device.")
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }

    }

    /**
     * Create directory and return file
     * returning video file
     */
    private fun getOutputMediaFile(): File? {

        // External sdcard file location
        val mediaStorageDir = File(
            Environment.getExternalStorageDirectory(),
            VIDEO_DIRECTORY_NAME
        )
        // Create storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(
                    TAG, "Oops! Failed create "
                            + VIDEO_DIRECTORY_NAME + " directory"
                )
                return null
            }
        }
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        val mediaFile: File

        mediaFile = File(
            mediaStorageDir.path + File.separator
                    + "VID_" + timeStamp + ".mp4"
        )
        return mediaFile
    }

    /**
     * close camera and release object
     */
    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            closePreviewSession()
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (null != mMediaRecorder) {
                mMediaRecorder!!.release()
                mMediaRecorder = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Start the camera preview.
     */
    private fun startPreview() {
        if (null == mCameraDevice || !mTextureView!!.isAvailable || null == mPreviewSize) {
            return
        }
        try {
            closePreviewSession()
            val texture = mTextureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val previewSurface = Surface(texture)
            mPreviewBuilder!!.addTarget(previewSurface)
            mCameraDevice!!.createCaptureSession(listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        mPreviewSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "onConfigureFailed: Failed ")
                    }
                }, mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    /**
     * Update the camera preview. [.startPreview] needs to be called in advance.
     */
    private fun updatePreview() {
        if (null == mCameraDevice) {
            return
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder!!)
            val thread = HandlerThread("CameraPreview")
            thread.start()
            mPreviewSession!!.setRepeatingRequest(mPreviewBuilder!!.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    /**
     * Configures the necessary [Matrix] transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = activity
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, mPreviewSize!!.height.toFloat(), mPreviewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / mPreviewSize!!.height,
                viewWidth.toFloat() / mPreviewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        }
        mTextureView!!.setTransform(matrix)
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        val activity = getActivity()
        try{
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        }catch (e:Exception)
        {
            e.printStackTrace()
        }
        /**
         * create video output file
         */
        mCurrentFile = getOutputMediaFile()
        /**
         * set output file in media recorder
         */
        mMediaRecorder!!.setOutputFile(mCurrentFile!!.absolutePath)
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
        mMediaRecorder!!.setVideoFrameRate(profile.videoFrameRate)
        mMediaRecorder!!.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
        mMediaRecorder!!.setVideoEncodingBitRate(profile.videoBitRate)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)
        mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mMediaRecorder!!.setAudioEncodingBitRate(profile.audioBitRate)
        mMediaRecorder!!.setAudioSamplingRate(profile.audioSampleRate)

        val rotation = activity.windowManager.defaultDisplay.rotation
        when (mSensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES -> mMediaRecorder!!.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
            SENSOR_ORIENTATION_INVERSE_DEGREES -> mMediaRecorder!!.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
        }
        mMediaRecorder!!.prepare()
    }

    fun startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView!!.isAvailable || null == mPreviewSize) {
            return
        }
        try {
            closePreviewSession()
            setUpMediaRecorder()
            //val texture = mTextureView!!.surfaceTexture!!
            var texture = mTextureView!!.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            val surfaces = ArrayList<Surface>()
            val previewSurface = Surface(texture)
            surfaces.add(previewSurface)
            mPreviewBuilder!!.addTarget(previewSurface)

            //MediaRecorder setup for surface
            val recorderSurface = mMediaRecorder!!.surface
            surfaces.add(recorderSurface)
            mPreviewBuilder!!.addTarget(recorderSurface)

            // Start a capture session
            mCameraDevice!!.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession
                    updatePreview()
                    activity.runOnUiThread {
                        mIsRecordingVideo = true
                        // Start recording
                        mMediaRecorder!!.start()
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Log.e(TAG, "onConfigureFailed: Failed")
                }
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }

    @Throws(Exception::class)
    fun stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false
        try {
            mPreviewSession!!.stopRepeating()
            mPreviewSession!!.abortCaptures()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        // Stop recording
        mMediaRecorder!!.stop()
        mMediaRecorder!!.reset()
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {

        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }

    }

}

