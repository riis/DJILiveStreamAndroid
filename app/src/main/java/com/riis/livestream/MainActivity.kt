package com.riis.livestream

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.riis.livestream.fragments.MainFragment
import com.riis.livestream.fragments.VideoFragment
import com.riis.livestream.interfaces.AudioCheckboxListener
import com.riis.livestream.model.DisplayService
import com.riis.livestream.viewmodel.MainViewModel
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import net.ossrs.rtmp.ConnectCheckerRtmp


@FlowPreview
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), ConnectCheckerRtmp,
    AudioCheckboxListener {

    private val viewModel: MainViewModel by viewModels()
    private val REQUEST_CODE_STREAM = 179 //random num
    private val REQUEST_CODE_RECORD = 180 //random num
    private lateinit var button: Button
    private var disableAudio = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keeps the screen always on during the app use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        button = findViewById(R.id.startStopButton)

        DisplayService.init(this)

        if (DisplayService.isStreaming()) {
            button.text = getString(R.string.stop_stream)
        } else {
            button.text = getString(R.string.start_stream)
        }

        button.setOnClickListener {
            if (!DisplayService.isStreaming()) {
                button.text = getString(R.string.stop_stream)
                // Tries to start the display service intent (streaming)
                startActivityForResult(DisplayService.sendIntent(), REQUEST_CODE_STREAM)
            } else {
                button.text = getString(R.string.start_stream)
                // stops the display service intent (streaming)
                stopService(Intent(this, DisplayService::class.java))
            }
        }

        // if the connection status live data in the MainViewModel is updated
        // then the UI will be refreshed
        viewModel.notifyStatusChanged.observe(this, Observer {
            refreshLayout(it)
        })

        // Loads the main fragment into the activity
        val fragment = MainFragment()
        fragment.initializeAudioListeners(this)
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment,"fragment_main").commit()

    }

    private fun refreshLayout(connected: Boolean) {
        // if the drone is connected
        if(connected){
            // Allows the user to start the stream
            Log.i("LivestreamApplication", "product connected MainActivity")
            button.isEnabled = true
        } else {
            // If the drone becomes disconnected
            // Check if the current fragment displayed is the video fragment,
            // if so, then switch back to the main fragment
            val fragment = supportFragmentManager.findFragmentByTag("fragment_video")
            if( fragment != null && fragment.isVisible ){
                val mainFragment = MainFragment()
                mainFragment.initializeAudioListeners(this)
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, mainFragment,"fragment_main").commit()
            }
            // dispable the button and change button text
            button.text = getString(R.string.start_stream)
            button.isEnabled = false

            // Stop the display service (stops stream)
            stopService(Intent(this, DisplayService::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && (requestCode == REQUEST_CODE_STREAM
                    || requestCode == REQUEST_CODE_RECORD && resultCode == Activity.RESULT_OK)
        ) {
            // If the user accepts the foreground service
            // Start the DisplayService class
            DisplayService.setData(resultCode, data)
            val intent = Intent(this, DisplayService::class.java)
            intent.putExtra("endpoint", "rtmp://live.twitch.tv/app/live_596583026_gkF2Mb56wbDeu5N4ycL9su24i4SaJg")
            intent.putExtra("audio", disableAudio)

            // Start service and change the fragment to see the drone camera stream
            startService(intent)
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, VideoFragment(), "fragment_video").addToBackStack("fragment_video").commit()
        } else {
            // User didn't allow the foreground service
            // Then change button back to normal
            Log.i("streamService", "${MainActivity::class.java.name}: Missing Permissions")
            button.text = getString(R.string.start_stream)
        }
    }

    override fun onAuthSuccessRtmp() {
        Log.i("streamService", "${MainActivity::class.java.name}: Auth Success")
    }

    override fun onNewBitrateRtmp(bitrate: Long) {}

    override fun onConnectionSuccessRtmp() {
        Log.i("streamService", "${MainActivity::class.java.name}: connection success")
    }

    override fun onConnectionFailedRtmp(reason: String) {
        // if the connection fails, then stop the display service
        Log.i("streamService", "${MainActivity::class.java.name}: Connection failed -> $reason")
        stopService(Intent(this, DisplayService::class.java))
        button.text = getString(R.string.start_stream)
    }

    override fun onAuthErrorRtmp() {
        Log.i("streamService", "${MainActivity::class.java.name}: Auth Error")
    }

    override fun onDisconnectRtmp() {
        Log.i("streamService", "${MainActivity::class.java.name}: Connection Disconnected")
    }

    // Listens for when a usb is connected to the phone
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val action = intent?.action
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED == action) {
            val attachedIntent = Intent()
            attachedIntent.action = DJISDKManager.USB_ACCESSORY_ATTACHED
            sendBroadcast(attachedIntent)
        }
    }

    override fun isAudioDisabled(value: Boolean) {
        disableAudio = value
    }
}