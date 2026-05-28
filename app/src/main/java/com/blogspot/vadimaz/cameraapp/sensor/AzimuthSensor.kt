package com.blogspot.vadimaz.cameraapp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class OrientationData(
    val azimuth: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f
)

fun getOrientationFlow(context: Context): Flow<OrientationData> = callbackFlow {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val rotationMatrix = FloatArray(9)

    // Helper function to smoothly interpolate between two angles over the shortest path
    fun interpolateAngles(angle1: Float, angle2: Float, t: Float): Float {
        val diff = (angle2 - angle1)
        val shortestDiff = ((diff + 180f) % 360f - 180f)
        var result = angle1 + shortestDiff * t
        if (result < 0) result += 360f
        if (result >= 360f) result -= 360f
        return result
    }

    // Function to calculate smoothly blended orientation based on inclination
    fun calculateBlendedOrientation(matrix: FloatArray): OrientationData {
        // 1. Calculate traditional flat orientation (heading of the top of the phone + raw pitch/roll)
        val flatOrientation = FloatArray(3)
        SensorManager.getOrientation(matrix, flatOrientation)
        var flatAzimuth = Math.toDegrees(flatOrientation[0].toDouble()).toFloat()
        if (flatAzimuth < 0) flatAzimuth += 360f

        val pitch = Math.toDegrees(flatOrientation[1].toDouble()).toFloat()
        val roll = Math.toDegrees(flatOrientation[2].toDouble()).toFloat()

        // 2. Calculate vertical azimuth (heading of the camera lens, -Z axis)
        val verticalR = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            matrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            verticalR
        )
        val verticalOrientation = FloatArray(3)
        SensorManager.getOrientation(verticalR, verticalOrientation)
        var verticalAzimuth = Math.toDegrees(verticalOrientation[0].toDouble()).toFloat()
        if (verticalAzimuth < 0) verticalAzimuth += 360f

        // 3. Compute inclination weight (t) based on the screen's tilt
        // matrix[8] represents the Z-axis vertical component (cosine of tilt)
        val r8 = matrix[8].coerceIn(-1f, 1f)
        // sine of tilt = sqrt(1 - cos^2). t is 0 when flat, 1 when completely upright
        val t = Math.sqrt((1f - r8 * r8).toDouble()).toFloat()

        // 4. Smoothly interpolate between flat and vertical azimuths
        val blendedAzimuth = interpolateAngles(flatAzimuth, verticalAzimuth, t)

        return OrientationData(
            azimuth = blendedAzimuth,
            pitch = pitch,
            roll = roll
        )
    }

    val listener = object : SensorEventListener {
        private val accelerometerReading = FloatArray(3)
        private val magnetometerReading = FloatArray(3)
        private var hasAcc = false
        private var hasMag = false
        private var currentAzimuth = 0f
        private var currentPitch = 0f
        private var currentRoll = 0f

        override fun onSensorChanged(event: SensorEvent) {
            var targetOrientation = OrientationData()
            var sensorDataReceived = false

            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                targetOrientation = calculateBlendedOrientation(rotationMatrix)
                sensorDataReceived = true
            } else {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                    hasAcc = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                    hasMag = true
                }
                if (hasAcc && hasMag) {
                    val success = SensorManager.getRotationMatrix(
                        rotationMatrix,
                        null,
                        accelerometerReading,
                        magnetometerReading
                    )
                    if (success) {
                        targetOrientation = calculateBlendedOrientation(rotationMatrix)
                        sensorDataReceived = true
                    }
                }
            }

            if (sensorDataReceived) {
                // Low-pass filter for azimuth (shortest path wrapping)
                val diffAz = (targetOrientation.azimuth - currentAzimuth)
                val shortestDiffAz = ((diffAz + 180f) % 360f - 180f)
                currentAzimuth += shortestDiffAz * 0.15f // Dampening factor
                if (currentAzimuth < 0) currentAzimuth += 360f
                if (currentAzimuth >= 360f) currentAzimuth -= 360f

                // Low-pass filter for pitch
                val diffPitch = (targetOrientation.pitch - currentPitch)
                currentPitch += diffPitch * 0.15f

                // Low-pass filter for roll
                val diffRoll = (targetOrientation.roll - currentRoll)
                currentRoll += diffRoll * 0.15f

                trySend(OrientationData(
                    azimuth = currentAzimuth,
                    pitch = currentPitch,
                    roll = currentRoll
                ))
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (rotationVectorSensor != null) {
        sensorManager.registerListener(
            listener,
            rotationVectorSensor,
            SensorManager.SENSOR_DELAY_UI
        )
    } else {
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    awaitClose {
        sensorManager.unregisterListener(listener)
    }
}
