package com.blogspot.vadimaz.cameraapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.blogspot.vadimaz.cameraapp.ui.camera.CameraScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(CameraDest)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<CameraDest> {
                    CameraScreen(
                        modifier = Modifier
                    )
                }
            },
    )
}
