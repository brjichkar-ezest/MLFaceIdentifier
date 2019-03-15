package com.ezest.mlfaceidentifier.ui_section.custom_camera

import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.coremedia.iso.IsoFile
import com.coremedia.iso.boxes.Container
import com.coremedia.iso.boxes.TimeToSampleBox
import com.coremedia.iso.boxes.TrackBox
import com.ezest.mlfaceidentifier.R
import com.googlecode.mp4parser.DataSource
import com.googlecode.mp4parser.FileDataSourceImpl
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.Mp4TrackImpl
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CameraFragment : CameraVideoFragment(),View.OnClickListener {


    private val TAG = "CameraFragment"
    private val VIDEO_DIRECTORY_NAME = "AndroidWave"


    lateinit var mTextureView: AutoFitTextureView

    lateinit var mRecordVideo: ImageView

    lateinit var mVideoView: VideoView

    lateinit var mPlayVideo: ImageView

    lateinit var mNextScreen: ImageView

    lateinit var gif_img:ImageView

    private var mOutputFilePath: String? = null



    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */

    fun CameraFragment() {
        // Required empty public constructor
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_camera, container, false)
        gif_img=view.findViewById(R.id.gif_img)

        mTextureView=view.findViewById(R.id.mTextureView)
        mRecordVideo=view.findViewById(R.id.mRecordVideo)
        mVideoView=view.findViewById(R.id.mVideoView)
        mPlayVideo=view.findViewById(R.id.mPlayVideo)
        mNextScreen=view.findViewById(R.id.mNextScreen)
        var requestManager = Glide.with(this)

        mRecordVideo.setOnClickListener(this)
        mPlayVideo.setOnClickListener(this)
        mNextScreen.setOnClickListener(this)

        var requestBuilder = requestManager.load(R.drawable.anim_flag_hungary)
        requestBuilder.load(R.drawable.anim_flag_hungary)
        requestBuilder.into(gif_img)
        gif_img.visibility=View.GONE
        return view
    }




    override fun getTextureResource(): Int {
        return R.id.mTextureView
    }

    override protected fun setUp(view: View) {

    }

    override fun onClick(v: View?) {
        when (v!!.getId())
        {
            R.id.mRecordVideo -> {
                if (mIsRecordingVideo) {
                    try {
                        gif_img!!.visibility = View.GONE
                        stopRecordingVideo()
                        prepareViews()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    gif_img!!.visibility = View.VISIBLE
                    startRecordingVideo()
                    mRecordVideo!!.setImageResource(R.drawable.ic_stop)
                    //Receive out put file here
                    mOutputFilePath = getCurrentFile()!!.getAbsolutePath()
                }
            }
            R.id.mPlayVideo -> {
                mVideoView!!.start()
                mPlayVideo!!.visibility = View.GONE
            }

            R.id.mNextScreen -> {
                Toast.makeText(activity, "Next activity", Toast.LENGTH_SHORT).show()
            }
            else->{
                Toast.makeText(activity, "Nothing matches", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareViews() {
        if (mVideoView!!.visibility == View.GONE) {
            mVideoView!!.visibility = View.VISIBLE
            mPlayVideo!!.visibility = View.VISIBLE
            mTextureView!!.visibility = View.GONE
            try {
                setMediaForRecordVideo()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    @Throws(IOException::class)
    private fun setMediaForRecordVideo() {
        mOutputFilePath = parseVideo(mOutputFilePath)
        // Set media controller
        mVideoView!!.setMediaController(MediaController(activity))
        mVideoView!!.requestFocus()
        mVideoView!!.setVideoPath(mOutputFilePath)
        mVideoView!!.seekTo(100)
        mVideoView!!.setOnCompletionListener { mp ->
            // Reset player
            mVideoView!!.setVisibility(View.GONE)
            mTextureView!!.setVisibility(View.VISIBLE)
            mPlayVideo!!.setVisibility(View.GONE)

            //mRecordVideo.setImageResource(R.drawable.ic_record);
            mNextScreen!!.setVisibility(View.VISIBLE)
            mRecordVideo!!.setVisibility(View.GONE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    @Throws(IOException::class)
    private fun parseVideo(mFilePath: String?): String? {
        val channel = FileDataSourceImpl(mFilePath)
        val isoFile = IsoFile(channel)
        val trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox::class.java)
        var isError = false
        for (trackBox in trackBoxes) {
            val firstEntry =
                trackBox.getMediaBox().getMediaInformationBox().getSampleTableBox().getTimeToSampleBox().getEntries()
                    .get(0)
            // Detect if first sample is a problem and fix it in isoFile
            // This is a hack. The audio deltas are 1024 for my files, and video deltas about 3000
            // 10000 seems sufficient since for 30 fps the normal delta is about 3000
            if (firstEntry.getDelta() > 10000) {
                isError = true
                firstEntry.setDelta(3000)
            }
        }
        val file = getOutputMediaFile()
        val filePath = file!!.absolutePath
        if (isError) {
            val movie = Movie()
            for (trackBox in trackBoxes) {
                movie.addTrack(
                    Mp4TrackImpl(
                        channel.toString() + "[" + trackBox.getTrackHeaderBox().getTrackId() + "]",
                        trackBox
                    )
                )
            }
            movie.setMatrix(isoFile.getMovieBox().getMovieHeaderBox().getMatrix())
            val out = DefaultMp4Builder().build(movie)

            //delete file first!
            val fc = RandomAccessFile(filePath, "rw").channel
            out.writeContainer(fc)
            fc.close()
            Log.d(TAG, "Finished correcting raw video")
            return filePath
        }
        return mFilePath
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
}