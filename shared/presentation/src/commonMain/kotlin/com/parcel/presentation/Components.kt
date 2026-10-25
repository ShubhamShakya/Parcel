package com.parcel.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.parcel.domain.Product
import com.parcel.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

enum class Glyph { Home, Heart, Search, Back, Info, Bag, Shelf, Close }

@Composable
fun Symbol(glyph: Glyph, description: String? = null, modifier: Modifier = Modifier, color: Color = LocalContentColor.current, filled: Boolean = false) {
    Canvas(modifier.size(24.dp).semantics { if (description != null) contentDescription = description }) {
        val u = size.minDimension / 24f
        fun pt(x: Float, y: Float) = Offset(x*u, y*u)
        fun line(x: Float,y: Float,x2: Float,y2: Float) = drawLine(color,pt(x,y),pt(x2,y2),2*u)
        when (glyph) {
            Glyph.Close -> { line(6f,6f,18f,18f); line(18f,6f,6f,18f) }
            Glyph.Shelf -> {
                drawRoundRect(color,pt(3f,3f),Size(18*u,19*u),CornerRadius(2*u),style=Stroke(1.7f*u))
                line(3f,12f,21f,12f);line(12f,3f,12f,22f)
                drawCircle(Orange,2.5f*u,pt(7.5f,8f));drawRoundRect(color,pt(15f,15f),Size(3*u,4*u),CornerRadius(u))
            }
            Glyph.Search -> { drawCircle(color,7*u,pt(10f,10f),style=Stroke(2*u));line(15f,15f,21f,21f) }
            Glyph.Back -> { line(20f,12f,4f,12f);line(4f,12f,11f,5f);line(4f,12f,11f,19f) }
            Glyph.Home -> {
                val p=Path().apply { moveTo(3*u,11*u);lineTo(12*u,3*u);lineTo(21*u,11*u)
                    moveTo(5*u,10*u);lineTo(5*u,21*u);lineTo(10*u,21*u);lineTo(10*u,15*u)
                    lineTo(14*u,15*u);lineTo(14*u,21*u);lineTo(19*u,21*u);lineTo(19*u,10*u) }
                drawPath(p,color,style=Stroke(1.8f*u))
            }
            Glyph.Heart -> {
                val p=Path().apply { moveTo(12*u,21*u);cubicTo(9*u,18*u,2*u,13*u,2*u,7*u)
                    cubicTo(2*u,1*u,10*u,1*u,12*u,6*u);cubicTo(14*u,1*u,22*u,1*u,22*u,7*u)
                    cubicTo(22*u,13*u,15*u,18*u,12*u,21*u);close() }
                if (filled) drawPath(p,color) else drawPath(p,color,style=Stroke(1.8f*u))
            }
            Glyph.Info -> { drawCircle(color,9*u,pt(12f,12f),style=Stroke(1.8f*u));line(12f,10f,12f,17f);drawCircle(color,1*u,pt(12f,7f)) }
            Glyph.Bag -> {
                drawRoundRect(color,pt(4f,8f),Size(16*u,14*u),CornerRadius(2*u),style=Stroke(1.7f*u))
                val p=Path().apply { moveTo(8*u,9*u);lineTo(8*u,6*u);cubicTo(8*u,0f,16*u,0f,16*u,6*u);lineTo(16*u,9*u) }
                drawPath(p,Orange,style=Stroke(2*u))
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier, imageKey: String = product.imageKey) {
    AsyncImage(model = Res.getUri("files/products/${imageKey}.png"),
        contentDescription = "${product.title}",
        contentScale = ContentScale.Crop, modifier = modifier.background(Ivory))
}

@Composable
fun SimulationNote(modifier: Modifier = Modifier) {
    Surface(color = Mint, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text("SIMULATION  ·  No real payments or deliveries", modifier = Modifier.padding(10.dp, 8.dp),
            style = MaterialTheme.typography.labelSmall, color = Teal)
    }
}
