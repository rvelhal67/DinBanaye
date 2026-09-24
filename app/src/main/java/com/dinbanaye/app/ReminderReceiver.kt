package com.dinbanaye.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Routine"
        val n = NotificationCompat.Builder(context, "routine_reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("DinBanaye")
            .setContentText("Time for: $title")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(intent.getIntExtra("id", 1), n)
    }
}
