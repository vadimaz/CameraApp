package com.blogspot.vadimaz.cameraapp.ui.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.vadimaz.cameraapp.sensor.getAzimuthFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    val azimuth: StateFlow<Float> = getAzimuthFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )
}
