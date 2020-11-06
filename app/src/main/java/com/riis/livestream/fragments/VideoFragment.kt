package com.riis.livestream.fragments

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.riis.livestream.R
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class VideoFragment: Fragment() {

    private lateinit var videoTextureView: TextureView
    private var codecManager: DJICodecManager? = null
    private var mReceivedVideoDataListener: VideoFeeder.VideoDataListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video, container, false)
        // Texture view for the drone camera view
        videoTextureView = view.findViewById(R.id.video_texture_view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        // Adds a texture view listener for when its view is updated
        videoTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                if (codecManager == null){
                    codecManager = DJICodecManager(requireActivity(), p0, p1, p2)
                }
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                // If the texture is destroyed, then clean the texture view
                codecManager?.cleanSurface()
                return false
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                // If the texture view is available adn the DJI codec manager is not yet initialized yet
                if (codecManager == null){
                    // Assign the codec manager to the texture view
                    codecManager = DJICodecManager(requireActivity(), surface, width, height)
                }
            }

        }

        // Set the data listener callback from the drone to the codec manager
        mReceivedVideoDataListener = VideoFeeder.VideoDataListener { bytes, i ->
            codecManager?.sendDataToDecoder(bytes, i)
        }
    }


    override fun onPause() {
        // Removes the data listener
        if(mReceivedVideoDataListener != null){
            VideoFeeder.getInstance().primaryVideoFeed.removeVideoDataListener(mReceivedVideoDataListener)
        }
        super.onPause()
    }

    override fun onDestroy() {
        // Cleans and destroys teh texture view codec
        codecManager?.cleanSurface()
        codecManager?.destroyCodec()
        super.onDestroy()
    }
}