package com.almamun252.nikhuthisab.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.almamun252.nikhuthisab.R

// Use the notification symbol and full-color logo with the system's default tint.
object NotificationAppearance {

    fun apply(
        context: Context,
        builder: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        builder.setSmallIcon(R.drawable.ic_notification_wallet)

        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.let { drawable ->
            builder.setLargeIcon(
                drawable.toBitmap(128, 128, Bitmap.Config.ARGB_8888)
            )
        }

        return builder
    }
}