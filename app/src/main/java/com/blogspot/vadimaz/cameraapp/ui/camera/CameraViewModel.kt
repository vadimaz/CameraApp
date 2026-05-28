package com.blogspot.vadimaz.cameraapp.ui.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.vadimaz.cameraapp.sensor.OrientationData
import com.blogspot.vadimaz.cameraapp.sensor.getOrientationFlow
import com.blogspot.vadimaz.cameraapp.sensor.LocationData
import com.blogspot.vadimaz.cameraapp.sensor.getLocationFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    val orientation: StateFlow<OrientationData> = getOrientationFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OrientationData()
        )

    val location: StateFlow<LocationData> = getLocationFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LocationData()
        )
}
