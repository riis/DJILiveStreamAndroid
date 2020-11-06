package com.riis.livestream.viewmodel

import android.app.Activity
import androidx.lifecycle.*
import com.riis.livestream.model.DJIResourceManager
import dji.sdk.products.Aircraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@FlowPreview
class MainViewModel: ViewModel() {
    // Updates if the connections status of the controller/drone changes
    val notifyStatusChanged = DJIResourceManager.instance.connectionStatus.asLiveData()

    // Gets the drone from the resource manager
    var product: Aircraft? = null
        get(){
            field = DJIResourceManager.instance.aircraft
            return field
        }
        private set

    // calls the resource manager to register the application
    fun registerDJI(activity: Activity){
        viewModelScope.launch {
            DJIResourceManager.instance.registerApp(activity)
        }
    }

}