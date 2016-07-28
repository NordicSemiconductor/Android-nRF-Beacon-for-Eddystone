package no.nordicsemi.android.nrfbeacon.nearby;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import java.nio.charset.Charset;
import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.util.Utils;

/**
 * Created by rora on 28.01.2016.
 */
public class NearbyBackgroundService extends IntentService {
    private static final int NOTIFICATION_ID = 1;
    private final static int OPEN_ACTIVITY_REQ = 195; // random

    public ArrayList<Message> mNearbyDevicesMessageList = new ArrayList<>();

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
        Log.v(Utils.TAG, "Destroyed");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        Nearby.Messages.handleIntent(intent, new MessageListener() {
            @Override
            public void onFound(Message message) {
                String nearbyMessage = new String(message.getContent(), Charset.forName("UTF-8"));
                Log.i(Utils.TAG, "Found message via PendingIntent: " + nearbyMessage);
                sendMessage(Utils.DISPLAY_NOTIFICATION, message);
                //displayNotification(message);
            }

            @Override
            public void onLost(Message message) {
                String nearbyMessage = new String(message.getContent(), Charset.forName("UTF-8"));
                Log.i(Utils.TAG, "Lost message via PendingIntent: " + nearbyMessage);
                sendMessage(Utils.REMOVE_NOTIFICATION, message);
                //updateNotification(message);
            }
        });
    }

    private void sendMessage(String broadcast, Message message) {
        Intent intent;
        Bundle bundle;
        switch (broadcast){
            case Utils.DISPLAY_NOTIFICATION:
                intent = new Intent(Utils.NEW_MESSAGE_FOUND);
                intent.putExtra(Utils.NEW_MESSAGE_FOUND, message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                displayNotification(message);
                break;
            case Utils.REMOVE_NOTIFICATION:
                intent = new Intent(Utils.MESSAGE_LOST);
                intent.putExtra(Utils.MESSAGE_LOST, message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                updateNotification(message);
                break;
        }
    }

    private void displayNotification(Message message){
        if (!checkIfnearbyDeviceAlreadyExists(new String(message.getContent(), Charset.forName("UTF-8")))) {
            Log.i(Utils.TAG, "Adding message");
            mNearbyDevicesMessageList.add(message);
            Log.i(Utils.TAG, "count after adding: " + mNearbyDevicesMessageList.size());
            createNotification();
        }
    }

    private void updateNotification(Message message){
        if (checkIfnearbyDeviceAlreadyExists(new String(message.getContent(), Charset.forName("UTF-8")))) {
            Log.i(Utils.TAG, "removing message: " + message);
            removeLostNearbyMessage(new String(message.getContent(), Charset.forName("UTF-8")));
            Log.i(Utils.TAG, "count after removing: " + mNearbyDevicesMessageList.size());
            createNotification();
        }
    }

    private void createNotification() {
        //mNearbyDevicesMessageList = loadNearbyMessageListForNotification();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if(mNearbyDevicesMessageList.size() == 0){
            notificationManager.cancelAll();
            return;

        }
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(Utils.NEARBY_DEVICE_DATA, mNearbyDevicesMessageList);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivities(getApplicationContext(), OPEN_ACTIVITY_REQ, new Intent[]{intent}, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_eddystone)
                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.actionBarColor))
                        .setContentTitle(getString(R.string.app_name))
                        .setContentIntent(pendingIntent);

        if(mNearbyDevicesMessageList.size() == 1) {
            mBuilder.setContentText(new String(mNearbyDevicesMessageList.get(0).getContent(), Charset.forName("UTF-8")));
        } else {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(getString(R.string.app_name));
            inboxStyle.setSummaryText(mNearbyDevicesMessageList.size() + " beacons found");
            mBuilder.setContentText(mNearbyDevicesMessageList.size() + " beacons found");
            for (int i = 0; i < mNearbyDevicesMessageList.size(); i++) {
                inboxStyle.addLine(new String(mNearbyDevicesMessageList.get(i).getContent(), Charset.forName("UTF-8")));
            }
            mBuilder.setStyle(inboxStyle);
        }

        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private ArrayList<Message> loadNearbyMessageListForNotification(){
        final ArrayList<Message> nearbyMessageList = new ArrayList<>();
        for(int i = 0; i < mNearbyDevicesMessageList.size(); i++){
            nearbyMessageList.add(mNearbyDevicesMessageList.get(i));
        }
        return  nearbyMessageList;
    }

    private boolean checkIfnearbyDeviceAlreadyExists(String nearbyDeviceMessage){
        String message;
        for(int i = 0; i < mNearbyDevicesMessageList.size(); i++){
            message = new String(mNearbyDevicesMessageList.get(i).getContent(), Charset.forName("UTF-8"));
            if(nearbyDeviceMessage.equals(message)){
                return true;
            }
        }
        return false;
    }

    private void removeLostNearbyMessage(String nearbyDeviceMessage){
        String message;
        for(int i = 0; i < mNearbyDevicesMessageList.size(); i++){
            message = new String(mNearbyDevicesMessageList.get(i).getContent(), Charset.forName("UTF-8"));
            if(nearbyDeviceMessage.equals(message)){
                mNearbyDevicesMessageList.remove(i);
                break;
            }
        }
    }
}
