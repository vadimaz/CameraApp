package com.blogspot.vadimaz.cameraapp.ui.camera

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun LevelIndicator(
    roll: Float,
    pitch: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Precision dimensions in pixels to match the screenshot design
    val centerDotRadiusPx = with(density) { 5.5.dp.toPx() } // Hollow center circle radius
    val stubGapPx = with(density) { 5.dp.toPx() }
    
    // Overall thickness of lines increased by 50%
    val strokeThicknessBgPx = with(density) { 3.dp.toPx() } // Subtle underlay shadow (was 2.dp)
    val strokeThicknessFgPx = with(density) { 1.5.dp.toPx() } // Primary line (was 1.dp)

    val pitchSpacingPx = with(density) { 8.dp.toPx() } // Vertical distance per 10 degrees
    val smallTickWidthPx = with(density) { 8.dp.toPx() }
    val largeTickWidthPx = with(density) { 14.dp.toPx() }
    val labelTextSizePx = with(density) { 9.dp.toPx() }
    val labelOffsetPx = with(density) { 5.dp.toPx() }

    // Color definitions
    val hudColor = Color.White
    val activeGreenColor = Color(0xFF00E676) // Sleek, modern, high-visibility green

    // Smooth transition logic as values approach 0 degrees
    // We use a 5-degree threshold: at >= 5°, it is fully white; at 0°, it is fully green.
    val threshold = 5f
    
    val pitchFraction = ((threshold - Math.abs(pitch)) / threshold).coerceIn(0f, 1f)
    val rollFraction = ((threshold - Math.abs(roll)) / threshold).coerceIn(0f, 1f)
    val bothFraction = Math.min(pitchFraction, rollFraction)

    val stubColor = lerpColor(hudColor, activeGreenColor, pitchFraction)
    val mainLineColor = lerpColor(hudColor, activeGreenColor, rollFraction)
    val circleColor = mainLineColor // Small center circle turns green with roll
    val arcColor = lerpColor(hudColor, activeGreenColor, bothFraction) // Outer circle turns green with both

    Canvas(
        modifier = modifier
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // The circle diameter is exactly 76% of the screen-width
        val radiusPx = size.width * 0.38f

        // ==========================================
        // 1. FIXED REFERENCE HORIZON STUBS (Extended to edges)
        // ==========================================
        val leftStubStart = 0f
        val leftStubEnd = centerX - radiusPx - stubGapPx
        val rightStubStart = centerX + radiusPx + stubGapPx
        val rightStubEnd = size.width

        // Subtle dark shadow backdrop for stubs
        drawLine(
            color = Color.Black.copy(alpha = 0.15f),
            start = Offset(leftStubStart, centerY),
            end = Offset(leftStubEnd, centerY),
            strokeWidth = strokeThicknessBgPx
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.15f),
            start = Offset(rightStubStart, centerY),
            end = Offset(rightStubEnd, centerY),
            strokeWidth = strokeThicknessBgPx
        )

        // Primary foreground for stubs (smoothly colors green as pitch approaches 0)
        drawLine(
            color = stubColor.copy(alpha = 0.9f),
            start = Offset(leftStubStart, centerY),
            end = Offset(leftStubEnd, centerY),
            strokeWidth = strokeThicknessFgPx
        )
        drawLine(
            color = stubColor.copy(alpha = 0.9f),
            start = Offset(rightStubStart, centerY),
            end = Offset(rightStubEnd, centerY),
            strokeWidth = strokeThicknessFgPx
        )

        // ==========================================
        // 2. OUTER ARCS (CIRCLE WITH GAPS)
        // ==========================================
        val arcSize = Size(radiusPx * 2f, radiusPx * 2f)
        val arcTopLeft = Offset(centerX - radiusPx, centerY - radiusPx)

        // Subtle dark shadow backdrop for arcs
        drawArc(
            color = Color.Black.copy(alpha = 0.15f),
            startAngle = 183f,
            sweepAngle = 174f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeThicknessBgPx)
        )
        drawArc(
            color = Color.Black.copy(alpha = 0.15f),
            startAngle = 3f,
            sweepAngle = 174f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeThicknessBgPx)
        )

        // Primary foreground for arcs (smoothly colors green as BOTH pitch and roll approach 0)
        drawArc(
            color = arcColor.copy(alpha = 0.9f),
            startAngle = 183f,
            sweepAngle = 174f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeThicknessFgPx)
        )
        drawArc(
            color = arcColor.copy(alpha = 0.9f),
            startAngle = 3f,
            sweepAngle = 174f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeThicknessFgPx)
        )

        // ==========================================
        // 3. PITCH NOTCHES (DYNAMIC SCALE)
        // ==========================================
        val absPitch = Math.abs(pitch)
        if (absPitch > 0.05f) {
            val isTiltingDown = pitch < 0f
            val directionFactor = if (isTiltingDown) -1f else 1f

            for (i in 1..9) {
                val targetDegree = i * 10f
                
                val opacity = when {
                    absPitch < targetDegree - 10f -> 0f
                    absPitch >= targetDegree -> 1f
                    else -> (absPitch - (targetDegree - 10f)) / 10f
                }

                if (opacity > 0f) {
                    val yOffset = directionFactor * i * pitchSpacingPx
                    val notchY = centerY + yOffset

                    val isMajor = i % 3 == 0
                    val tickWidth = if (isMajor) largeTickWidthPx else smallTickWidthPx

                    val leftNotchStart = centerX - radiusPx - tickWidth - stubGapPx
                    val leftNotchEnd = centerX - radiusPx - stubGapPx
                    val rightNotchStart = centerX + radiusPx + stubGapPx
                    val rightNotchEnd = centerX + radiusPx + tickWidth + stubGapPx

                    // Underlay shadow for notches
                    drawLine(
                        color = Color.Black.copy(alpha = 0.15f * opacity),
                        start = Offset(leftNotchStart, notchY),
                        end = Offset(leftNotchEnd, notchY),
                        strokeWidth = strokeThicknessBgPx
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = 0.15f * opacity),
                        start = Offset(rightNotchStart, notchY),
                        end = Offset(rightNotchEnd, notchY),
                        strokeWidth = strokeThicknessBgPx
                    )

                    // White foreground for notches
                    drawLine(
                        color = hudColor.copy(alpha = 0.8f * opacity),
                        start = Offset(leftNotchStart, notchY),
                        end = Offset(leftNotchEnd, notchY),
                        strokeWidth = strokeThicknessFgPx
                    )
                    drawLine(
                        color = hudColor.copy(alpha = 0.8f * opacity),
                        start = Offset(rightNotchStart, notchY),
                        end = Offset(rightNotchEnd, notchY),
                        strokeWidth = strokeThicknessFgPx
                    )

                    // Major ticks label
                    if (isMajor) {
                        val text = "${i * 10}"
                        
                        drawContext.canvas.nativeCanvas.apply {
                            val textPaintBg = Paint().apply {
                                color = android.graphics.Color.BLACK
                                alpha = (0.25f * opacity * 255).toInt()
                                textSize = labelTextSizePx
                                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                                textAlign = Paint.Align.RIGHT
                                style = Paint.Style.STROKE
                                strokeWidth = strokeThicknessBgPx
                            }

                            val textPaintFg = Paint().apply {
                                color = android.graphics.Color.argb(
                                    (opacity * 220).toInt(),
                                    255, 255, 255
                                )
                                textSize = labelTextSizePx
                                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                                textAlign = Paint.Align.RIGHT
                                isAntiAlias = true
                            }

                            val bounds = Rect()
                            textPaintFg.getTextBounds(text, 0, text.length, bounds)
                            val vOffset = bounds.height() / 2f

                            val leftTextX = leftNotchStart - labelOffsetPx
                            drawText(text, leftTextX, notchY + vOffset, textPaintBg)
                            drawText(text, leftTextX, notchY + vOffset, textPaintFg)

                            textPaintBg.textAlign = Paint.Align.LEFT
                            textPaintFg.textAlign = Paint.Align.LEFT

                            val rightTextX = rightNotchEnd + labelOffsetPx
                            drawText(text, rightTextX, notchY + vOffset, textPaintBg)
                            drawText(text, rightTextX, notchY + vOffset, textPaintFg)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. ROTATING HORIZON LINE AND HOLLOW CENTER DOT
        // ==========================================
        // We rotate the central line & center dot around the canvas center.
        // We use -roll so the line represents a true horizontal horizon.
        rotate(degrees = -roll, pivot = Offset(centerX, centerY)) {
            // Rotating Line left & right segments
            val lineLeftStart = centerX - radiusPx + stubGapPx
            val lineLeftEnd = centerX - centerDotRadiusPx
            val lineRightStart = centerX + centerDotRadiusPx
            val lineRightEnd = centerX + radiusPx - stubGapPx

            // Underlay shadow for rotating line
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(lineLeftStart, centerY),
                end = Offset(lineLeftEnd, centerY),
                strokeWidth = strokeThicknessBgPx
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(lineRightStart, centerY),
                end = Offset(lineRightEnd, centerY),
                strokeWidth = strokeThicknessBgPx
            )

            // Underlay shadow for hollow center circle
            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = centerDotRadiusPx,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeThicknessBgPx)
            )

            // Primary foreground for rotating line segments (smoothly colors green as roll approaches 0)
            drawLine(
                color = mainLineColor,
                start = Offset(lineLeftStart, centerY),
                end = Offset(lineLeftEnd, centerY),
                strokeWidth = strokeThicknessFgPx
            )
            drawLine(
                color = mainLineColor,
                start = Offset(lineRightStart, centerY),
                end = Offset(lineRightEnd, centerY),
                strokeWidth = strokeThicknessFgPx
            )

            // Primary foreground for hollow center circle (smoothly colors green as roll approaches 0)
            drawCircle(
                color = circleColor,
                radius = centerDotRadiusPx,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeThicknessFgPx)
            )
        }
    }
}

// Helper function to smoothly interpolate colors in RGB space
private fun lerpColor(color1: Color, color2: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = color1.red + (color2.red - color1.red) * f,
        green = color1.green + (color2.green - color1.green) * f,
        blue = color1.blue + (color2.blue - color1.blue) * f,
        alpha = color1.alpha + (color2.alpha - color1.alpha) * f
    )
}
