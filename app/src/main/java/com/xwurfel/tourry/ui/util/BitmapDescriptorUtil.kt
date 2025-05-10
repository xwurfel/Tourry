package com.xwurfel.tourry.ui.util

import android.content.Context
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object BitmapDescriptorUtil {
    fun vectorToBitmap(context: Context, id: Int, tint: Color? = null): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, id)!!

        val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)

        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)

        if (tint != null) {
            vectorDrawable.setTint(tint.toArgb())
        }

        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}