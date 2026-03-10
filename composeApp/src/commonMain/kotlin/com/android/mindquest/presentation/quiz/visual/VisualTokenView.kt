package com.android.mindquest.presentation.quiz.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders a [VisualToken] at the given size.
 *
 * - [VisualToken.Shape]: Canvas-drawn geometric shape
 * - [VisualToken.ColorSwatch]: Filled circle of that color
 * - [VisualToken.Combo]: Colored geometric shape
 * - [VisualToken.Emoji]: Plain text fallback
 */
@Composable
fun VisualTokenView(
    token: VisualToken,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    when (token) {
        is VisualToken.Shape -> {
            val color = if (token.fillColor == Color.Unspecified) {
                defaultShapeColor(token.shape)
            } else {
                token.fillColor
            }
            ShapeCanvas(
                shape = token.shape,
                fillColor = color,
                strokeColor = color.darken(0.3f),
                size = size,
                modifier = modifier,
            )
        }

        is VisualToken.ColorSwatch -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(token.color),
            )
        }

        is VisualToken.Combo -> {
            ShapeCanvas(
                shape = token.shape,
                fillColor = token.color,
                strokeColor = token.color.darken(0.3f),
                size = size,
                modifier = modifier,
            )
        }

        is VisualToken.Emoji -> {
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = token.text,
                    fontSize = (size.value * 0.7f).sp,
                )
            }
        }
    }
}

/**
 * Convenience: resolve raw text and render.
 */
@Composable
fun VisualTokenFromText(
    rawText: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val token = QuizVisualTokenMapper.resolve(rawText)
    VisualTokenView(token = token, modifier = modifier, size = size)
}

// ── Shape Canvas ────────────────────────────────────────────────────────

@Composable
private fun ShapeCanvas(
    shape: ShapeType,
    fillColor: Color,
    strokeColor: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.size(size),
    ) {
        val padding = this.size.minDimension * 0.08f
        val drawSize = Size(
            this.size.width - padding * 2,
            this.size.height - padding * 2,
        )
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = min(drawSize.width, drawSize.height) / 2f

        drawShape(shape, center, radius, fillColor, strokeColor)
    }
}

private fun DrawScope.drawShape(
    shape: ShapeType,
    center: Offset,
    radius: Float,
    fill: Color,
    stroke: Color,
) {
    val strokeWidth = radius * 0.08f

    when (shape) {
        ShapeType.CIRCLE -> {
            drawCircle(fill, radius, center)
            drawCircle(stroke, radius, center, style = Stroke(strokeWidth))
        }

        ShapeType.SQUARE -> {
            val half = radius * 0.85f
            val topLeft = Offset(center.x - half, center.y - half)
            drawRoundRect(fill, topLeft, Size(half * 2, half * 2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.12f))
            drawRoundRect(stroke, topLeft, Size(half * 2, half * 2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.12f), style = Stroke(strokeWidth))
        }

        ShapeType.TRIANGLE -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.866f, center.y + radius * 0.5f)
                lineTo(center.x - radius * 0.866f, center.y + radius * 0.5f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.DIAMOND -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.7f, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius * 0.7f, center.y)
                close()
            }
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.STAR -> {
            val path = starPath(center, radius, radius * 0.4f, 5)
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.PENTAGON -> {
            val path = regularPolygonPath(center, radius, 5)
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.HEXAGON -> {
            val path = regularPolygonPath(center, radius, 6)
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.OCTAGON -> {
            val path = regularPolygonPath(center, radius, 8)
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.HEART -> {
            val path = heartPath(center, radius)
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.PLUS -> {
            val arm = radius * 0.3f
            val path = Path().apply {
                moveTo(center.x - arm, center.y - radius * 0.85f)
                lineTo(center.x + arm, center.y - radius * 0.85f)
                lineTo(center.x + arm, center.y - arm)
                lineTo(center.x + radius * 0.85f, center.y - arm)
                lineTo(center.x + radius * 0.85f, center.y + arm)
                lineTo(center.x + arm, center.y + arm)
                lineTo(center.x + arm, center.y + radius * 0.85f)
                lineTo(center.x - arm, center.y + radius * 0.85f)
                lineTo(center.x - arm, center.y + arm)
                lineTo(center.x - radius * 0.85f, center.y + arm)
                lineTo(center.x - radius * 0.85f, center.y - arm)
                lineTo(center.x - arm, center.y - arm)
                close()
            }
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.CROSS -> {
            val thickness = strokeWidth * 3f
            val len = radius * 0.7f
            drawLine(stroke, Offset(center.x - len, center.y - len), Offset(center.x + len, center.y + len), thickness, StrokeCap.Round)
            drawLine(stroke, Offset(center.x + len, center.y - len), Offset(center.x - len, center.y + len), thickness, StrokeCap.Round)
        }

        ShapeType.ARROW_UP -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.6f, center.y)
                lineTo(center.x + radius * 0.2f, center.y)
                lineTo(center.x + radius * 0.2f, center.y + radius * 0.8f)
                lineTo(center.x - radius * 0.2f, center.y + radius * 0.8f)
                lineTo(center.x - radius * 0.2f, center.y)
                lineTo(center.x - radius * 0.6f, center.y)
                close()
            }
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.ARROW_RIGHT -> {
            val path = Path().apply {
                moveTo(center.x + radius, center.y)
                lineTo(center.x, center.y - radius * 0.6f)
                lineTo(center.x, center.y - radius * 0.2f)
                lineTo(center.x - radius * 0.8f, center.y - radius * 0.2f)
                lineTo(center.x - radius * 0.8f, center.y + radius * 0.2f)
                lineTo(center.x, center.y + radius * 0.2f)
                lineTo(center.x, center.y + radius * 0.6f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.SEMICIRCLE -> {
            val path = Path().apply {
                addArc(
                    oval = androidx.compose.ui.geometry.Rect(
                        center.x - radius,
                        center.y - radius,
                        center.x + radius,
                        center.y + radius,
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                )
                close()
            }
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.PARALLELOGRAM -> {
            val skew = radius * 0.3f
            val half = radius * 0.75f
            val path = Path().apply {
                moveTo(center.x - half + skew, center.y - half * 0.6f)
                lineTo(center.x + half + skew, center.y - half * 0.6f)
                lineTo(center.x + half - skew, center.y + half * 0.6f)
                lineTo(center.x - half - skew, center.y + half * 0.6f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.TRAPEZOID -> {
            val half = radius * 0.85f
            val path = Path().apply {
                moveTo(center.x - half * 0.6f, center.y - half * 0.6f)
                lineTo(center.x + half * 0.6f, center.y - half * 0.6f)
                lineTo(center.x + half, center.y + half * 0.6f)
                lineTo(center.x - half, center.y + half * 0.6f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
        }

        ShapeType.CRESCENT -> {
            val path = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center.x - radius,
                        center.y - radius,
                        center.x + radius,
                        center.y + radius,
                    ),
                )
            }
            drawPath(path, fill)
            // Inner cutout circle offset to the right
            drawCircle(
                Color.White.copy(alpha = 0.85f),
                radius * 0.75f,
                Offset(center.x + radius * 0.35f, center.y - radius * 0.1f),
            )
            drawPath(path, stroke, style = Stroke(strokeWidth))
        }
    }
}

// ── Path Helpers ────────────────────────────────────────────────────────

private fun regularPolygonPath(center: Offset, radius: Float, sides: Int): Path {
    val angleStep = 2.0 * PI / sides
    val startAngle = -PI / 2.0  // start from top
    return Path().apply {
        for (i in 0 until sides) {
            val angle = startAngle + i * angleStep
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private fun starPath(center: Offset, outerRadius: Float, innerRadius: Float, points: Int): Path {
    val angleStep = PI / points
    val startAngle = -PI / 2.0
    return Path().apply {
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = startAngle + i * angleStep
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private fun heartPath(center: Offset, radius: Float): Path {
    val r = radius * 0.85f
    return Path().apply {
        moveTo(center.x, center.y + r * 0.7f) // bottom tip
        // Left curve
        cubicTo(
            center.x - r * 1.5f, center.y,
            center.x - r * 0.8f, center.y - r * 1.2f,
            center.x, center.y - r * 0.5f,
        )
        // Right curve
        cubicTo(
            center.x + r * 0.8f, center.y - r * 1.2f,
            center.x + r * 1.5f, center.y,
            center.x, center.y + r * 0.7f,
        )
        close()
    }
}

// ── Color Helpers ───────────────────────────────────────────────────────

private fun defaultShapeColor(shape: ShapeType): Color = when (shape) {
    ShapeType.TRIANGLE -> Color(0xFF3B82F6)   // blue
    ShapeType.SQUARE -> Color(0xFFEF4444)      // red
    ShapeType.CIRCLE -> Color(0xFF22C55E)      // green
    ShapeType.PENTAGON -> Color(0xFFA855F7)     // purple
    ShapeType.STAR -> Color(0xFFEAB308)         // yellow
    ShapeType.DIAMOND -> Color(0xFFF97316)      // orange
    ShapeType.HEXAGON -> Color(0xFF06B6D4)      // cyan
    ShapeType.HEART -> Color(0xFFEC4899)        // pink
    ShapeType.PLUS -> Color(0xFF10B981)         // emerald
    ShapeType.CROSS -> Color(0xFFEF4444)        // red
    ShapeType.ARROW_UP -> Color(0xFF6366F1)     // indigo
    ShapeType.ARROW_RIGHT -> Color(0xFF6366F1)  // indigo
    ShapeType.SEMICIRCLE -> Color(0xFF8B5CF6)   // violet
    ShapeType.PARALLELOGRAM -> Color(0xFF0EA5E9) // sky
    ShapeType.TRAPEZOID -> Color(0xFFD97706)    // amber
    ShapeType.OCTAGON -> Color(0xFFDC2626)      // red (stop sign)
    ShapeType.CRESCENT -> Color(0xFFFBBF24)     // amber
}

private fun Color.darken(factor: Float): Color {
    return Color(
        red = (this.red * (1f - factor)).coerceIn(0f, 1f),
        green = (this.green * (1f - factor)).coerceIn(0f, 1f),
        blue = (this.blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = this.alpha,
    )
}
