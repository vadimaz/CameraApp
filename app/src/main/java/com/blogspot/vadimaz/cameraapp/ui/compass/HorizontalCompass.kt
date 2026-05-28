package com.blogspot.vadimaz.cameraapp.ui.compass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

            // We hide the ticks and numbers that pass directly behind the center readout pill
            // The centerpiece pill has a width of about 90.dp.
            // In pixels, that is 90 * density.
            val centerHidingZonePx = 45.dp.toPx()

            for (a in startAngle..endAngle) {
                val normAngle = (a % 360 + 360) % 360
                val offsetFromCenter = a - azimuth
                val tickX = centerX + (offsetFromCenter * pixelsPerDegree)

                // Only draw if within visual boundaries
                val distanceFromCenter = abs(tickX - centerX)
                val maxVisibleDistance = centerX * 0.95f

                if (distanceFromCenter < maxVisibleDistance && distanceFromCenter > centerHidingZonePx) {
                    // Smooth quadratic fade towards the left/right edges
                    val alpha = (1f - (distanceFromCenter / maxVisibleDistance)).coerceIn(0f, 1f)
                    val finalAlpha = alpha * alpha

                    if (a % 10 == 0) {
                        // Major Tick line
                        drawLine(
                            color = Color.White.copy(alpha = finalAlpha * 0.35f),
                            start = Offset(tickX, height * 0.35f),
                            end = Offset(tickX, height * 0.65f),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        // Degree Label (Multiples of 30 for low clutter)
                        if (normAngle % 30 == 0) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    this.color = android.graphics.Color.WHITE
                                    this.alpha = (finalAlpha * 140).toInt()
                                    textSize = 10.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.create(
                                        android.graphics.Typeface.MONOSPACE,
                                        android.graphics.Typeface.NORMAL
                                    )
                                }
                                canvas.nativeCanvas.drawText(
                                    normAngle.toString(),
                                    tickX,
                                    height * 0.28f,
                                    paint
                                )
                            }
                        }
                    } else if (a % 5 == 0) {
                        // Minor Tick line (Every 5 degrees for a clean aesthetic)
                        drawLine(
                            color = Color.White.copy(alpha = finalAlpha * 0.18f),
                            start = Offset(tickX, height * 0.42f),
                            end = Offset(tickX, height * 0.58f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
        }

        // 2. Draw static center hairline indicator passing through the centerpiece
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.5.dp)
                .background(Color.White.copy(alpha = 0.6f))
        )

        // 3. Digital Centerpiece Readout (floating pill)
        val directionText = getApproximateDirection(azimuth)
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = String.format(java.util.Locale.US, "%s  •  %03d°", directionText, azimuth.toInt() % 360),
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private fun getApproximateDirection(azimuth: Float): String {
    val degrees = (azimuth % 360 + 360) % 360
    return when {
        degrees >= 337.5 || degrees < 22.5 -> "N"
        degrees >= 22.5 && degrees < 67.5 -> "NE"
        degrees >= 67.5 && degrees < 112.5 -> "E"
        degrees >= 112.5 && degrees < 157.5 -> "SE"
        degrees >= 157.5 && degrees < 202.5 -> "S"
        degrees >= 202.5 && degrees < 247.5 -> "SW"
        degrees >= 247.5 && degrees < 292.5 -> "W"
        else -> "NW"
    }
}
