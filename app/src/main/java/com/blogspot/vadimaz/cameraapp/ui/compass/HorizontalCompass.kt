package com.blogspot.vadimaz.cameraapp.ui.compass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
            .height(100.dp)
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
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val pixelsPerDegree = 8f // 1 degree = 8 pixels

            // 1. Draw Background scrolling ticks and labels
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
                    // Compute smooth fade-off towards the left/right edges
                    val alpha = (1f - (distanceFromCenter / maxVisibleDistance)).coerceIn(0f, 1f)
                    val finalAlpha = alpha * alpha // Quadratic easing for organic fading

                    // Ticks
                    if (a % 10 == 0) {
                        // Major Tick line
                        drawLine(
                            color = Color.White.copy(alpha = finalAlpha * 0.7f),
                            start = Offset(tickX, height * 0.35f),
                            end = Offset(tickX, height * 0.55f),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Degree Label (Multiples of 20)
                        if (normAngle % 20 == 0) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    this.color = android.graphics.Color.WHITE
                                    this.alpha = (finalAlpha * 255).toInt()
                                    textSize = 12.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.create(
                                        android.graphics.Typeface.DEFAULT,
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

                        // Direction Letter (Multiples of 45)
                        val direction = getDirectionText(normAngle)
                        if (direction != null) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    this.color = android.graphics.Color.WHITE
                                    this.alpha = (finalAlpha * 255).toInt()
                                    textSize = 16.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.create(
                                        android.graphics.Typeface.DEFAULT,
                                        android.graphics.Typeface.BOLD
                                    )
                                }
                                canvas.nativeCanvas.drawText(
                                    direction,
                                    tickX,
                                    height * 0.8f,
                                    paint
                                )
                            }
                        }
                    } else if (a % 2 == 0) {
                        // Minor Tick line
                        drawLine(
                            color = Color.White.copy(alpha = finalAlpha * 0.4f),
                            start = Offset(tickX, height * 0.42f),
                            end = Offset(tickX, height * 0.48f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // 2. Draw HUD Center Indicator Pointers (Static in center)
            // Top Triangle pointing down
            val topTriangle = Path().apply {
                moveTo(centerX, 12.dp.toPx())
                lineTo(centerX - 8.dp.toPx(), 0f)
                lineTo(centerX + 8.dp.toPx(), 0f)
                close()
            }
            drawPath(
                path = topTriangle,
                color = Color.White
            )

            // Bottom Triangle pointing up
            val bottomTriangle = Path().apply {
                moveTo(centerX, height - 12.dp.toPx())
                lineTo(centerX - 8.dp.toPx(), height)
                lineTo(centerX + 8.dp.toPx(), height)
                close()
            }
            drawPath(
                path = bottomTriangle,
                color = Color.White
            )

            // Center Vertical indicator line
            drawLine(
                color = Color.White,
                start = Offset(centerX, 12.dp.toPx()),
                end = Offset(centerX, height - 12.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun getDirectionText(angle: Int): String? {
    return when (angle) {
        0 -> "N"
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
