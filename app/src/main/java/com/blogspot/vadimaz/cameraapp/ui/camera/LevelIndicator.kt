package com.blogspot.vadimaz.cameraapp.ui.camera

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    
    // Extremely thin white strokes to match the premium design in the screenshot
    val strokeThicknessBgPx = with(density) { 2.dp.toPx() } // Subtle underlay shadow
    val strokeThicknessFgPx = with(density) { 1.dp.toPx() } // Very fine primary line

    val pitchSpacingPx = with(density) { 8.dp.toPx() } // Vertical distance per 10 degrees
    val smallTickWidthPx = with(density) { 8.dp.toPx() }
    val largeTickWidthPx = with(density) { 14.dp.toPx() }
    val labelTextSizePx = with(density) { 9.dp.toPx() }
    val labelOffsetPx = with(density) { 5.dp.toPx() }

    // Exactly matching the screenshot: the HUD uses a clean, premium white color
    val hudColor = Color.White

    Canvas(
        modifier = modifier
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // The circle diameter is exactly 76% of the screen-width to replicate the screenshot proportions
        val radiusPx = size.width * 0.38f

        // ==========================================
        // 1. FIXED REFERENCE HORIZON STUBS (Extended to edges)
        // ==========================================
        // Left stub: starts at the very left edge of the 90% canvas (5% from screen border)
        val leftStubStart = 0f
        val leftStubEnd = centerX - radiusPx - stubGapPx
        // Right stub: ends at the very right edge of the 90% canvas (5% from screen border)
        val rightStubStart = centerX + radiusPx + stubGapPx
        val rightStubEnd = size.width

        // Subtle dark shadow backdrop for stubs (extremely fine, high readability)
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

        // Primary white foreground for stubs
        drawLine(
            color = hudColor.copy(alpha = 0.9f),
            start = Offset(leftStubStart, centerY),
            end = Offset(leftStubEnd, centerY),
            strokeWidth = strokeThicknessFgPx
        )
        drawLine(
            color = hudColor.copy(alpha = 0.9f),
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
        // Leave a tiny 3 degrees gap on both sides centered at 0 and 180 degrees
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

        // Primary white foreground for arcs
        drawArc(
            color = hudColor.copy(alpha = 0.9f),
            startAngle = 183f,
            sweepAngle = 174f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeThicknessFgPx)
        )
        drawArc(
            color = hudColor.copy(alpha = 0.9f),
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
            // Rotating Line left & right segments (broken in the center and shortened at the ends with stubGapPx offset)
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

            // Primary white foreground for rotating line segments
            drawLine(
                color = hudColor,
                start = Offset(lineLeftStart, centerY),
                end = Offset(lineLeftEnd, centerY),
                strokeWidth = strokeThicknessFgPx
            )
            drawLine(
                color = hudColor,
                start = Offset(lineRightStart, centerY),
                end = Offset(lineRightEnd, centerY),
                strokeWidth = strokeThicknessFgPx
            )

            // Primary white foreground for hollow center circle
            drawCircle(
                color = hudColor,
                radius = centerDotRadiusPx,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeThicknessFgPx)
            )
        }
    }
}
