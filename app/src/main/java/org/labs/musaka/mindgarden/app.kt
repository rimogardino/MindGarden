package org.labs.musaka.mindgarden


import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat


class app : Application() {


    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }




    private fun createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Garden status", NotificationCompat.PRIORITY_DEFAULT)
            channel.description = ""
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }
    }

    companion object {
        const val CHANNEL_ID = "1"
    }

}