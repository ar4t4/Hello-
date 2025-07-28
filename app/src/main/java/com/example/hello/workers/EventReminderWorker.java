package com.example.hello.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.hello.R;
import com.example.hello.EventsActivity;

public class EventReminderWorker extends Worker {
    private static final String CHANNEL_ID = "event_reminders";
    private static final int NOTIFICATION_ID = 1;

    public EventReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String eventTitle = getInputData().getString("eventTitle");
        String eventLocation = getInputData().getString("eventLocation");
        
        createNotificationChannel();
        showNotification(eventTitle, eventLocation);
        
        return Result.success();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for community events");
            
            NotificationManager notificationManager = getApplicationContext()
                .getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String eventTitle, String eventLocation) {
        Intent intent = new Intent(getApplicationContext(), EventsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
            getApplicationContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Event Reminder: " + eventTitle)
            .setContentText("Event location: " + eventLocation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) 
            getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
} 