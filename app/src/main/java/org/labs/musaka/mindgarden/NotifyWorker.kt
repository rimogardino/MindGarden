package org.labs.musaka.mindgarden

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import androidx.work.Worker
import androidx.work.WorkerParameters

class NotifyWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {


    override fun doWork(): Result {
        notificationShow()

        return Result.success()
    }


    private fun notificationShow() {
        val intent = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(context, app.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_spa)
                .setContentTitle("Reminder")
                .setContentText("Watter your plats!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()


        NotificationManagerCompat.from(context).notify(0, builder)
    }
}