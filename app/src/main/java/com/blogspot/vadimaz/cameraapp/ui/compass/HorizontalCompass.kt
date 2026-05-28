package com.blogspot.vadimaz.cameraapp.ui.compass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun HorizontalCompass(
    azimuth: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black.copy(alpha = 0.4f))
            .drawBehind {
                val borderThickness = 1.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = borderThickness
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Draw the scrolling analog tape in the background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val pixelsPerDegree = 8f // 1 degree = 8 pixels

            val startAngle = (azimuth - 45).toInt()
            val endAngle = (azimuth + 45).toInt()

            for (a in startAngle..endAngle) {
                val normAngle = (a % 360 + 360) % 360
                val offsetFromCenter = a - azimuth
                val tickX = centerX + (offsetFromCenter * pixelsPerDegree)

                // Only draw if within visual boundaries
                val distanceFromCenter = abs(tickX - centerX)
                val maxVisibleDistance = centerX * 0.95f

                if (distanceFromCenter < maxVisibleDistance) {
                    // Smooth quadratic fade towards the left/right edges
                    val alpha = (1f - (distanceFromCenter / maxVisibleDistance)).coerceIn(0f, 1f)
                    val finalAlpha = alpha * alpha

                    if (a % 10 == 0) {
                        // Major Tick line (highly visible)
                        drawLine(
                            color = Color.White.copy(alpha = finalAlpha * 0.75f),
                            start = Offset(tickX, height * 0.35f),
                            end = Offset(tickX, height * 0.65f),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Direction Letter directly on the tape
                        val direction = getDirectionText(normAngle)
                        if (direction != null) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    this.color = android.graphics.Color.WHITE
                                    this.alpha = (finalAlpha * 255).toInt() // Full visibility
                                    textSize = 12.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.create(
                                        android.graphics.Typeface.DEFAULT,
                                        android.graphics.Typeface.BOLD
                                    )
                                }
                                canvas.nativeCanvas.drawText(
                                    direction,
                                    tickX,
                                    height * 0.28f,
                                    paint
                                )
                            }
                        }
                    } else if (a % 5 == 0) {
                        // Minor Tick line (highly visible)
                        drawLine(
                            color = Color.White.copy(alpha = finalAlpha * 0.45f),
                            start = Offset(tickX, height * 0.42f),
                            end = Offset(tickX, height * 0.58f),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                }
            }
        }

        // 2. Draw static center hairline indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.5.dp)
                .background(Color.White.copy(alpha = 0.75f))
        )
    }
}

private fun getDirectionText(angle: Int): String? {
    return when (angle) {
        0, 360 -> "N"
        45 -> "NE"
        90 -> "E"
        135 -> "SE"
        180 -> "S"
        225 -> "SW"
        270 -> "W"
        315 -> "NW"
        else -> null
    }
}
