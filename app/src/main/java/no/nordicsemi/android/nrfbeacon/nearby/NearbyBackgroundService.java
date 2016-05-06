package no.nordicsemi.android.nrfbeacon.nearby;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Created by rora on 28.01.2016.
 */
public class NearbyBackgroundService extends IntentService {

    private static final String DISPLAY_NOTIFICATION = "no.nordicsemi.android.nrfbeacon.nearby.DISPLAY_NOTIFICATION";
    private static final String REMOVE_NOTIFICATION = "no.nordicsemi.android.nrfbeacon.nearby.REMOVE_NOTIFICATION";
    private static final String NEARBY_MESSAGE = "no.nordicsemi.android.nrfbeacon.nearby.NEARBY_MESSAGE";
    public static final String NEARBY_DEVICE_DATA = "NEARBY_DEVICE_DATA";
    private static final String TAG = "BEACON";

    public NearbyBackgroundService(String name) {
        super(name);
    }

    public NearbyBackgroundService() {
        super("NearbyBackgroundService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Destroyed");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        Nearby.Messages.handleIntent(intent, new MessageListener() {
            @Override
            public void onFound(Message message) {
                String nearbyMessage = new String(message.getContent(), Charset.forName("UTF-8"));
                Log.i(TAG, "Found message via PendingIntent: " + nearbyMessage);
                sendMessage(DISPLAY_NOTIFICATION, message);
            }

            @Override
            public void onLost(Message message) {
                String nearbyMessage = new String(message.getContent(), Charset.forName("UTF-8"));
                Log.i(TAG, "Lost message via PendingIntent: " + nearbyMessage);
                sendMessage(REMOVE_NOTIFICATION, message);
            }
        });
    }

    private void sendMessage(String broadcast, Message message) {
        Intent intent;
        Bundle bundle;
        switch (broadcast){
            case DISPLAY_NOTIFICATION:
                intent = new Intent(broadcast);
                bundle = new Bundle();
                bundle.putParcelable(NEARBY_MESSAGE, message);
                intent.putExtras(bundle);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case REMOVE_NOTIFICATION:
                intent = new Intent(broadcast);
                bundle = new Bundle();
                bundle.putParcelable(NEARBY_MESSAGE, message);
                intent.putExtras(bundle);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
        }
    }
}
