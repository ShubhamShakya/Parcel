package com.parcel.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Original vector parcel friend; the opening and confetti are drawn in shared Compose. */
@Composable
fun ParcelFriend(modifier: Modifier = Modifier, opening: Float = 0f, greeting: Boolean = false) {
    val hello = remember { Animatable(if (greeting) 0f else 1f) }
    LaunchedEffect(greeting) { if (greeting) hello.animateTo(1f, spring(dampingRatio = .45f, stiffness = 120f)) }
    val Teal = Teal
    val Ink = Ink
    Canvas(modifier.size(100.dp).semantics { contentDescription = if (opening > .8f) "An open, smiling parcel with confetti" else "A smiling parcel friend" }) {
        val u = size.minDimension / 240f
        val bounce = if (greeting) (1f - hello.value) * 24f * u else sin(opening * PI * 4).toFloat() * 5f * u
        translate((size.width - 240*u)/2, (size.height - 240*u)/2 + bounce) {
            drawOval(Ink.copy(alpha=.08f), Offset(42*u,205*u),Size(156*u,15*u))
            if (opening > 0f) repeat(20) { i ->
                val angle = i * 2.399f
                val radius = (25 + opening * 75) * u
                val centre = Offset(120*u + cos(angle)*radius, 110*u + sin(angle)*radius - sin(opening*PI).toFloat()*30*u)
                rotate(i * 23f + opening*130, centre) {
                    drawRoundRect(listOf(Teal, Orange, Color(0xFFB399E8), Color(0xFFEE91AA))[i%4].copy(alpha = minOf(1f, opening*3)), centre, Size(7*u,12*u),CornerRadius(3*u))
                }
            }
            drawRoundRect(Color(0xFFE8AD75),Offset(48*u,95*u),Size(144*u,110*u),CornerRadius(15*u))
            drawRoundRect(Color(0xFFFFDFC9),Offset(54*u,95*u),Size(132*u,105*u),CornerRadius(12*u))
            drawRect(Color(0xFFFFF4C7),Offset(108*u,95*u),Size(24*u,105*u))
            drawCircle(Color(0xFF14213D),5*u,Offset(91*u,151*u)); drawCircle(Color(0xFF14213D),5*u,Offset(150*u,151*u))
            drawOval(Color(0xFFFFDCE7),Offset(70*u,158*u),Size(20*u,10*u));drawOval(Color(0xFFFFDCE7),Offset(152*u,158*u),Size(20*u,10*u))
            val smile=Path().apply { moveTo(108*u,164*u);quadraticTo(120*u,179*u,132*u,164*u) }
            drawPath(smile,Color(0xFF14213D),style=Stroke(3*u))
            rotate(-opening*12, Offset(120*u,70*u)) {
                drawRoundRect(Color(0xFFF1BA86),Offset(40*u,(78-opening*40)*u),Size(160*u,32*u),CornerRadius(8*u))
                drawRect(Color(0xFFFFF4C7),Offset(108*u,(78-opening*40)*u),Size(24*u,32*u))
            }
        }
    }
}
