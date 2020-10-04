package com.stretcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.Duration;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ReminderWorker extends Worker {
    /**
     * Log tag
     */
    private static final String kTAG = ReminderWorker.class.getCanonicalName();

    /**
     * Unique work name
     */
    private static final String kUNIQUE_WORK_NAME = "com.stretcher.ReminderWorker";

    /**
     * Channel name used for notifications
     */
    private static final String kNOTIFICATION_CHANNEL_NAME = "StretcherReminder";

    /**
     * Unique channel ID used for notifications
     */
    private static final String kNOTIFICATION_CHANNEL_ID = kUNIQUE_WORK_NAME;

    /**
     * How many hours in a day
     */
    private static final int kHOURS_IN_DAY = 24;

    /**
     * 0-24 hour at which the reminder should fire
     */
    private static final int kREMIND_HOUR_OF_DAY = 18;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(kTAG, "Worker triggered");

        // Create a channel
        NotificationChannel channel = new NotificationChannel(kNOTIFICATION_CHANNEL_ID, kNOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        NotificationManagerCompat.from(getApplicationContext()).createNotificationChannel(channel);

        // Create & post a notification
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), kNOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Stretcher")
                .setContentText("Stretch reminder")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).build();

        NotificationManagerCompat.from(getApplicationContext()).notify(new Random().nextInt(), notification);

        return Result.success();
    }

    /**
     * Schedule a worker to be executed every day
     * <p>
     * The worker will reminder the user to stretch
     */
    public static void schedule(Context context) {
        Calendar calendar = Calendar.getInstance();

        // Current time
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        // How much do we need to wait in order to get to kREMIND_HOUR_OF_DAY hour ?
        int deltaHours;
        if (currentHour > kREMIND_HOUR_OF_DAY) {
            // It's tomorrow
            deltaHours = kHOURS_IN_DAY - currentHour + kREMIND_HOUR_OF_DAY;
        } else {
            // It's today
            deltaHours = kREMIND_HOUR_OF_DAY - currentHour;
        }

        Log.d(kTAG, "Scheduling reminder in " + deltaHours + " hours");

        PeriodicWorkRequest.Builder reminderBuilder = new PeriodicWorkRequest.Builder(ReminderWorker.class, kHOURS_IN_DAY, TimeUnit.HOURS)
                .setInitialDelay(Duration.ofHours(deltaHours)
                );

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                kUNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderBuilder.build()
        );
    }
}
