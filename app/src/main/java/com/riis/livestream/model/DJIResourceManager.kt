package com.riis.livestream.model

import android.content.Context
import android.util.Log
import com.riis.livestream.MainActivity
import com.riis.livestream.fragments.MainFragment
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
@FlowPreview
class DJIResourceManager {

    private object HOLDER {
        val INSTANCE = DJIResourceManager()
    }

    companion object {
        val instance: DJIResourceManager by lazy { HOLDER.INSTANCE }
    }

    var flightController: FlightController? = null
    var aircraft: Aircraft? = null

    private var _connectionStatus = ConflatedBroadcastChannel(false)
    val connectionStatus: Flow<Boolean> = _connectionStatus.asFlow()

    private var isRegistrationInProgress = false

    suspend fun registerApp(context: Context): Boolean {
        // Doesn't allow registration if it is already in the process
        if(isRegistrationInProgress){
            return false
        }

        isRegistrationInProgress = true

        return suspendCoroutine { continuation ->
            Log.d(MainFragment.TAG, "registering, please wait...")
            DJISDKManager.getInstance().registerApp(context, object :
                DJISDKManager.SDKManagerCallback {

                override fun onProductDisconnect() {
                    // If the drone is disconnected
                    Log.d(MainFragment.TAG, "Product disconnected")
                    resetDefaultValues()
                }

                override fun onProductConnect(product: BaseProduct?) {
                    // If the drone is connected
                    Log.d(MainFragment.TAG, "Product connected, ${product.toString()}")
                    product?.let {
                        try {
                            aircraft = (it as Aircraft)
                            flightController = it.flightController
                            _connectionStatus.offer(true)
                        } catch (ex: Exception){
                            flightController = null
                        }
                    }
                }

                override fun onComponentChange(componentKey: BaseProduct.ComponentKey?, oldComponent: BaseComponent?, newComponent: BaseComponent?) {

                }

                // Checks the status of the registration
                override fun onRegister(p0: DJIError?) {
                    if(p0 == DJISDKError.REGISTRATION_SUCCESS){
                        Log.d(MainFragment.TAG, "Registration success.")
                        DJISDKManager.getInstance().startConnectionToProduct()
                        continuation.resume(true)
                    } else {
                        Log.d(MainFragment.TAG, "Error registering SDK, ${p0?.description}")
                        continuation.resume(false)
                    }
                    isRegistrationInProgress = false
                }

                override fun onDatabaseDownloadProgress(p0: Long, p1: Long) {
                    // Not yet implemented
                }

                override fun onInitProcess(p0: DJISDKInitEvent?, p1: Int) {
                    // Not yet implemented
                }

            })
        }
    }

    private fun resetDefaultValues() {
        flightController = null
        _connectionStatus.offer(false)
    }

}