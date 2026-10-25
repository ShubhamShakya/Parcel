package com.parcel.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.parcel.domain.*

/** Illustrative route: position comes from persisted delivery progress, never GPS. */
@Composable
fun DeliveryMap(progress: DeliveryProgress) {
    val Teal = Teal
    var style by rememberSaveable { mutableStateOf("Evening") }
    val mapBackground = when (style) { "Evening" -> Color(0xFF152039); "Soft" -> Color(0xFF322B28); else -> Color(0xFF1C302F) }
    val blockColor = when (style) { "Evening" -> Color(0xFF24314D); "Soft" -> Color(0xFF433A31); else -> Color(0xFF2C4140) }
    val target = when (progress.stage) {
        DeliveryStage.Confirmed -> 0f
        DeliveryStage.Shipped -> .22f
        DeliveryStage.NearbyHub -> .52f
        DeliveryStage.OutForDelivery -> .65f + .3f * ((progress.observedAt - progress.scheduledTime(DeliveryStage.OutForDelivery)).toFloat() / (progress.dueAt - progress.scheduledTime(DeliveryStage.OutForDelivery)).coerceAtLeast(1)).coerceIn(0f, 1f)
        DeliveryStage.Delivered -> 1f
    }
    val position by animateFloatAsState(target, tween(1200))
    Surface(shape = RoundedCornerShape(24.dp), color = ParcelSurface) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Streets", "Soft", "Evening").forEach { option -> FilterChip(style == option, { style = option }, label = { Text(option) }) }
            }
            Box(Modifier.fillMaxWidth().height(250.dp).testTag("delivery_map").semantics { contentDescription = "Illustrative delivery map. ${progress.stage.label}. Simulated route, not GPS." }) {
                Canvas(Modifier.fillMaxSize().background(mapBackground)) {
                    fun point(x: Float, y: Float) = Offset(size.width*x, size.height*y)
                    for (x in 0..5) for (y in 0..3) {
                        drawRoundRect(blockColor, point(x*.2f+.025f, y*.28f+.03f), Size(size.width*.14f, size.height*.20f), CornerRadius(8.dp.toPx()))
                    }
                    drawRoundRect(Color(0xFF2A503C), point(.43f,.31f), Size(size.width*.14f,size.height*.2f), CornerRadius(12.dp.toPx()))
                    for (x in 1..4) drawLine(Color(0xFF53617B), point(x*.2f,0f), point(x*.2f,1f), 12.dp.toPx())
                    for (y in 1..3) drawLine(Color(0xFF53617B), point(0f,y*.28f), point(1f,y*.28f), 12.dp.toPx())
                    val route = listOf(point(.2f,.84f),point(.4f,.84f),point(.4f,.56f),point(.8f,.56f),point(.8f,.28f))
                    val path = Path().apply { moveTo(route[0].x,route[0].y); route.drop(1).forEach { lineTo(it.x,it.y) } }
                    drawPath(path,Color.White,style=Stroke(10.dp.toPx(),cap=StrokeCap.Round))
                    drawPath(path,Color(0xFF2F6FED),style=Stroke(5.dp.toPx(),cap=StrokeCap.Round))
                    val step=(position*4).coerceAtMost(3.9999f); val index=step.toInt(); val f=step-index
                    val rider=route[index]+(route[index+1]-route[index])*f
                    drawCircle(Teal,10.dp.toPx(),route.first());drawCircle(Color.White,4.dp.toPx(),route.first())
                    drawCircle(Color(0xFFC8372D),12.dp.toPx(),route.last());drawCircle(Color.White,4.dp.toPx(),route.last())
                    drawCircle(Color.White,17.dp.toPx(),rider);drawCircle(Teal,13.dp.toPx(),rider)
                    drawCircle(Orange,5.dp.toPx(),rider)
                }
                Surface(Modifier.align(Alignment.TopStart).padding(12.dp), shape=RoundedCornerShape(20.dp), color=ParcelSurface) {
                    Text("SIMULATED ROUTE", Modifier.padding(12.dp,7.dp), style=MaterialTheme.typography.labelSmall, color=Teal)
                }
                Text("Parcel hub", Modifier.align(Alignment.BottomStart).padding(16.dp), color = Color.White, style=MaterialTheme.typography.labelMedium)
                Surface(Modifier.align(Alignment.TopEnd).padding(top=22.dp,end=12.dp), shape=RoundedCornerShape(10.dp),color=Teal) {
                    Text("Your address",Modifier.padding(8.dp),color=OnAccent,style=MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(progress.stage.label,style=MaterialTheme.typography.titleMedium);Text("Connaught Place · Delhi",color=Muted,style=MaterialTheme.typography.bodySmall) }
                Text(if(progress.stage==DeliveryStage.Delivered) "Arrived ✓" else "On its way",color=Teal,style=MaterialTheme.typography.labelMedium)
            }
        }
    }
}
