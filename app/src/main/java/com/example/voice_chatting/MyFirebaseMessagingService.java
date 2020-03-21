package com.example.voice_chatting;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CancelledCallInvite;
import com.twilio.voice.MessageListener;
import com.twilio.voice.Voice;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "VoiceFCMService";
    private static final String NOTIFICATION_ID_KEY = "NOTIFICATION_ID";
    private static final String CALL_SID_KEY = "CALL_SID";
    private static final String VOICE_CHANNEL = "default";

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onNewToken(@NonNull String firebase_idToken) {
        super.onNewToken(firebase_idToken);

        String refreshedToken = firebase_idToken;
        Intent intent = new Intent(VoiceActivity.ACTION_FCM_TOKEN);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//        getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Received onMessageReceived()");
        Log.d(TAG, "Bundle data: " + remoteMessage.getData());
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0){
            Map<String, String> data = remoteMessage.getData();
            final int notificationId = (int) System.currentTimeMillis();

            boolean valid = Voice.handleMessage(remoteMessage.getData(), new MessageListener() {
                @Override
                public void onCallInvite(@NonNull CallInvite callInvite) {
                    final int notificationId = (int) System.currentTimeMillis();
                    MyFirebaseMessagingService.this.notify(callInvite, notificationId);
                    MyFirebaseMessagingService.this.sendCallInvitetoActivity(callInvite, notificationId);
                }

                @Override
                public void onCancelledCallInvite(@NonNull CancelledCallInvite cancelledCallInvite) {
                    MyFirebaseMessagingService.this.cancelNotification(cancelledCallInvite);
                    MyFirebaseMessagingService.this.sendCancelledCalllInviteToActivity(cancelledCallInvite);
                }
            });

            if (!valid){
                Log.e(TAG, "The message was not a valid Twilio Voice SDK payload: " +
                        remoteMessage.getData());
            }
        }
    }

    private void notify(CallInvite callInvite, int notificationId){
        Intent intent = new Intent(this, VoiceActivity.class);
        intent.setAction(VoiceActivity.ACTION_INCOMING_CALL);
        intent.putExtra(VoiceActivity.INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(VoiceActivity.INCOMING_CALL_INVITE, callInvite);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Bundle extras = new Bundle();
        extras.putInt(NOTIFICATION_ID_KEY, notificationId);
        extras.putString(CALL_SID_KEY, callInvite.getCallSid());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel callInviteChannel = new NotificationChannel(VOICE_CHANNEL,
                    "Primary Voice Channel", NotificationManager.IMPORTANCE_DEFAULT);
            callInviteChannel.setLightColor(Color.GREEN);
            callInviteChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(callInviteChannel);

            Notification notification = new Notification.Builder(getApplicationContext(), VOICE_CHANNEL)
                    .setSmallIcon(R.drawable.ic_call_end_white_24dp)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(callInvite.getFrom() + " is calling.")
                    .setContentIntent(pendingIntent)
                    .setExtras(extras)
                    .setAutoCancel(true)
                    .build();
        }else {
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_call_end_white_24dp)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(callInvite.getFrom() + " is calling.")
                            .setAutoCancel(true)
                            .setExtras(extras)
                            .setContentIntent(pendingIntent)
                            .setGroup("test_app_notification")
                            .setColor(Color.rgb(214, 10, 37));

            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    private void cancelNotification(CancelledCallInvite cancelledCallInvite){
        SoundPoolManager.getInstance(this).stopRinging();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification statusBarNotification : activeNotifications){
                Notification notification = statusBarNotification.getNotification();
                Bundle extras = notification.extras;
                String notificationCallSid = extras.getString(CALL_SID_KEY);

                if (cancelledCallInvite.getCallSid().equals(notificationCallSid)){
                    notificationManager.cancel(extras.getInt(NOTIFICATION_ID_KEY));
                }
            }
        }else {
            notificationManager.cancelAll();
        }
    }

    private void sendCallInvitetoActivity(CallInvite callInvite, int notificationId){
        Intent intent = new Intent(this, VoiceActivity.class);
        intent.setAction(VoiceActivity.ACTION_INCOMING_CALL);
        intent.putExtra(VoiceActivity.INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(VoiceActivity.INCOMING_CALL_INVITE, callInvite);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    private void sendCancelledCalllInviteToActivity(CancelledCallInvite cancelledCallInvite){
        Intent intent = new Intent(VoiceActivity.ACTION_CANCEL_CALL);
        intent.putExtra(VoiceActivity.CANCELLED_CALL_INVITE, cancelledCallInvite);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
