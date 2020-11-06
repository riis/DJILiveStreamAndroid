package com.riis.livestream.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.riis.livestream.interfaces.AudioCheckboxListener
import com.riis.livestream.R
import com.riis.livestream.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private var missingPermissions = mutableListOf<String>()
    private lateinit var mListener: AudioCheckboxListener

    private lateinit var statusTextView: TextView
    private lateinit var productInfoTextView: TextView

    companion object{
        const val TAG = "LivestreamApplication"
        private const val REQUEST_PERMISSION_CODE = 12345
        private val REQUIRED_PERMISSION_LIST = mutableListOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        setupData(view)
        checkAndRequestPermissions()

        // Checks the value of the checkbox when clicked and passes the information back to the activity
        val checkbox = view.findViewById<CheckBox>(R.id.audioCheckBox)
        checkbox.setOnClickListener {
            mListener.isAudioDisabled(checkbox.isChecked)
        }

        return view
    }

    fun initializeAudioListeners(listener: AudioCheckboxListener){
        mListener = listener
    }

    private fun setupData(v: View) {
        // When the drone is connected or disconnected, the UI will be updated
        viewModel.notifyStatusChanged.observe(viewLifecycleOwner, Observer {
            refreshSDKRelativeUI()
        })

        statusTextView = v.findViewById(R.id.status)
        productInfoTextView = v.findViewById(R.id.product_info)
    }

    private fun refreshSDKRelativeUI(){
        val mProduct = viewModel.product
        Log.i("LivestreamApplication", "${mProduct?.isConnected}")
        // If the drone is connected
        if(mProduct != null && mProduct.isConnected){
            statusTextView.text = getString(R.string.status_connected)
            if (mProduct.model != null){
                productInfoTextView.text = mProduct.model.displayName
            } else {
                productInfoTextView.text = getString(R.string.model_NA)
            }
        // If the drone is not available
        } else {
            productInfoTextView.text = getString(R.string.model_NA)
            statusTextView.text = getString(R.string.status_NA)
        }
    }

    private fun checkAndRequestPermissions(){
        // If build version is greater than p, then the foreground service permission needs to be added
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            Log.i("permissionManager", "addingForegroundServices")
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        // If permission is missing, then it is added to the list
        for (permission in REQUIRED_PERMISSION_LIST){
            if(ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED){
                missingPermissions.add(permission)
            }
        }
        // If there are no permissions in the missing permission list, then start drone connection
        if(missingPermissions.isEmpty()){
            viewModel.registerDJI(requireActivity())
        } else {
            // If there are missing permissions, then request the missing permissions
            Log.d(TAG, "Missing permission.")
            ActivityCompat.requestPermissions(requireActivity(), missingPermissions.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_PERMISSION_CODE){
            // Checks to see if the permissions have been granted, if
            // they have been granted, remove them from the missing permission list
            for (i in grantResults.size - 1 downTo 0) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.remove(permissions[i])
                }
            }
        }
        // If the missing permission list is empty, then start the drone connection
        if(missingPermissions.isEmpty()){
            viewModel.registerDJI(requireActivity())
        } else {
            Log.d(TAG, "Missing permission.")
        }
    }

}