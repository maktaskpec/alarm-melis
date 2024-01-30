package com.example.myapplication;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AlarmService extends Service {
    private MediaPlayer mediaPlayer;
    private static final String NOTIFICATION_CHANNEL_ID = "alarm_channel_id";
    private static final int NOTIFICATION_ID = 1;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotification();
        startAlarmSound();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("STOP_ALARM")) {
                // Kapat düğmesine tıklanıldığında yapılacak işlemler
                stopAlarmSound();
                stopSelf(); // Servisi durdur
            }
        }
        return START_NOT_STICKY;
    }
    private void showNotification() {
        String channelId = "alarm_channel_id";
        String channelName = "Alarm Channel";

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // stopIntent ve stopPendingIntent'ı metodun içinde tanımlama
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction("STOP_ALARM");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Notification oluştururken kapat düğmesi eklemesi
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Alarm")
                .setContentText("Alarm Message")
                .setSmallIcon(R.drawable.horoz1)
                .setContentIntent(pendingIntent)
                .addAction(0, "Kapat", stopPendingIntent)
                .setAutoCancel(true)
                .build();


        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    private void startAlarmSound() {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // Medya dosyası tamamlandığında yapılacak işlemler buraya yazılır.
                    mediaPlayer.start(); // Medya dosyasını tekrar başlat
                }
            });
            mediaPlayer.start();
        }
    }
    private void stopAlarmSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}