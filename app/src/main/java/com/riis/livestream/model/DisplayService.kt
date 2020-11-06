package com.riis.livestream.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pedro.rtplibrary.base.DisplayBase
import com.pedro.rtplibrary.rtmp.RtmpDisplay
import com.pedro.rtplibrary.rtsp.RtspDisplay
import com.riis.livestream.R
import net.ossrs.rtmp.ConnectCheckerRtmp

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class DisplayService: Service() {

    private var endpoint: String? = null
    private var disableAudio: Boolean? = null

    private val channelId = "rtpDisplayStreamChannel"
    private val notifyId = 123456
    private val streamBinder = StreamBinder(this)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        // If build sdk is over O, then a notification needs to be made when a
        // a foreground service starts (like recording the phone display)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        // If this function is not here, the recording/streaming only lasts
        // as long as the notification shows to the user (about 4 seconds)
        keepAliveTrick()
    }

    private fun keepAliveTrick() {
        // Not sure why it works but it does
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setContentTitle("")
                .setContentText("").build()
            startForeground(1, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // When the intent is started, the endpoint is pulled from the intent
        // and the stream is prepared and started
        Log.e(TAG, "RTP Display service started")
        endpoint = intent?.extras?.getString("endpoint")
        disableAudio = intent?.extras?.getBoolean("audio")
        if (endpoint != null) {
            prepareStreamRtp()
            startStreamRtp(endpoint!!)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return streamBinder
    }

    companion object {
        private val TAG = "DisplayService"
        private val channelId = "rtpDisplayStreamChannel"
        private val notifyId = 123456
        private var notificationManager: NotificationManager? = null
        private var displayBase: DisplayBase? = null
        private var contextApp: Context? = null
        private var resultCode: Int? = null
        private var data: Intent? = null

        // initialization
        fun init(context: Context) {
            contextApp = context
            if (displayBase == null) displayBase = RtmpDisplay(context, true, connectCheckerRtp)
        }

        // saves the result code and the data
        fun setData(resultCode: Int, data: Intent) {
            this.resultCode = resultCode
            this.data = data
        }

        // sends the intent
        fun sendIntent(): Intent? {
            if (displayBase != null) {
                return displayBase!!.sendIntent()
            } else {
                return null
            }
        }

        // check if the phone is streaming
        fun isStreaming(): Boolean {
            return if (displayBase != null) displayBase!!.isStreaming else false
        }

        // check if the phone is recording
        fun isRecording(): Boolean {
            return if (displayBase != null) displayBase!!.isRecording else false
        }

        // stops the stream
        fun stopStream() {
            if (displayBase != null) {
                if (displayBase!!.isStreaming) displayBase!!.stopStream()
            }
        }

        // callback to listen for connection to streaming platform
        private val connectCheckerRtp = object : ConnectCheckerRtmp {
            override fun onAuthSuccessRtmp() {
                Log.i("streamService", "auth::Success")
                showNotification("Stream authentication successful")
            }

            override fun onNewBitrateRtmp(bitrate: Long) {

            }

            override fun onConnectionSuccessRtmp() {
                Log.i("streamService", "connection:Success")
                showNotification("Stream started")
            }

            override fun onConnectionFailedRtmp(reason: String) {
                Log.i("streamService", "connection::Error")
                showNotification("Stream connection failed")
            }

            override fun onAuthErrorRtmp() {
                Log.i("streamService", "auth::Error")
                showNotification("Stream authentication failed")
            }

            override fun onDisconnectRtmp() {
                Log.i("streamService", "streamStopped")
                showNotification("Stream stopped")
            }
        }

        private fun showNotification(text: String) {
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                contextApp?.applicationContext?.let {
                    NotificationCompat.Builder(it, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("RTP Display Stream")
                        .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                        .setContentText(text).build()
                    }
                } else {
                    contextApp?.applicationContext?.let {
                        NotificationCompat.Builder(it, channelId)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("RTP Display Stream")
                            .setContentText(text).build()
                }
            }

            notificationManager?.notify(1, notification)
        }

    }

    // Gets the endpoint and starts the intent
    private fun prepareStreamRtp() {
        stopStream()
        if (endpoint!!.startsWith("rtmp")) {
            displayBase = RtmpDisplay(baseContext, true, connectCheckerRtp)
            displayBase?.setIntentResult(resultCode!!, data)
        }
    }

    // sets the parameters for the stream and starts it
    private fun startStreamRtp(endpoint: String) {
        val displayMetrics = DisplayMetrics()
        display?.getRealMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        if (!displayBase!!.isStreaming) {
            if (displayBase!!.prepareAudio() && displayBase!!.prepareVideo(
                    width,
                    height,
                    30,
                    1200 * 1024,
                    0,
                    displayMetrics.densityDpi
                )) {
                // if the value passed through the intent for disable audi is true
                // then the audio is disabled
                disableAudio?.let {
                    if(it){
                        displayBase!!.disableAudio()
                    }
                }
                displayBase!!.startStream(endpoint)
            }
        } else {
            Log.i("streamService", "AlreadyStreaming")
        }
    }

    // stops the stream and the service that controls the stream when intent is destroyed
    override fun onDestroy() {
        super.onDestroy()
        stopStream()
        stopForeground(true)
    }

}