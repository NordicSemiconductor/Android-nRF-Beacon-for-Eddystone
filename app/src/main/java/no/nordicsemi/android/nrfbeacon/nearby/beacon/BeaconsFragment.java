/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, getActivity() list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, getActivity() list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from getActivity()
 * software without specific prior written permission.
 *
 * getActivity() SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF getActivity() SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrfbeacon.nearby.beacon;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.sample.libproximitybeacon.Project;

import java.nio.charset.Charset;
import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.common.EddystoneBeaconsAdapter;
import no.nordicsemi.android.nrfbeacon.nearby.MainActivity;
import no.nordicsemi.android.nrfbeacon.nearby.NearbyBackgroundService;
import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.common.BaseFragment;
import no.nordicsemi.android.nrfbeacon.nearby.settings.NearbySettingsActivity;
import no.nordicsemi.android.nrfbeacon.nearby.util.Utils;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

public class BeaconsFragment extends BaseFragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener /*,
        PermissionRationaleDialogFragment.PermissionDialogListener*/ {

    public static final String EXTRA_ADAPTER_POSITION = "no.nordicsemi.android.nrfbeacon.extra.adapter_position";
    public static final String TAG = "BEACON";
    private static final String NEW_MESSAGE_FOUND = "no.nordicsemi.android.nrfbeacon.nearby.NEW_MESSAGE_FOUND";
    private static final String MESSAGE_LOST = "no.nordicsemi.android.nrfbeacon.nearby.MESSAGE_LOST";
    public static final String NEARBY_DEVICE_DATA = "NEARBY_DEVICE_DATA";

    private final static int OPEN_ACTIVITY_REQ = 195; // random
    private static final int REQUEST_NEARBY_SETTINGS = 252;
    private static final int REQUEST_PERMISSION_REQ_CODE = 76; // any 8-bit number
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_RESOLVE_ERROR = 261; //random
    private static final int NOTIFICATION_ID = 1;
    public static final String NEARBY_SETTINGS_HELP = "NEARBY_SETTINGS_HELP";

    private ImageView mNearbyImage;
    private TextView mPermissions;
    private ImageView mNearbySettings;

    private int mSelectedTabPosition = 0;

    private boolean mResolvingError;
    private boolean mScanForNearbyInBackground = false;
    private boolean mNearbyPermissionGranted = false;

    public ArrayList<Message> mNearbyDevicesMessageList;

    private PendingIntent mPendingIntent;
    private Intent mParentIntent;

    private Context mContext;
    private GoogleApiClient mGoogleApiClient = null;
    private NotificationManagerCompat mNotificationManager;
    private EddystoneBeaconsAdapter mEddystoneBeaconsAdapter;
    private Project mProject;

    private final BroadcastReceiver mBackgroundScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final Message message = intent.getParcelableExtra(action);
            switch (action) {
                case NEW_MESSAGE_FOUND:
                    updateBeaconsAdapter(message);
                    break;
                case MESSAGE_LOST:
                    removeLostNearbyMessage(new String(message.getContent()));
                    break;
            }
        }
    };


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedTabPosition = getArguments().getInt("index");
        }

        if(savedInstanceState != null){
            mSelectedTabPosition = savedInstanceState.getInt(EXTRA_ADAPTER_POSITION);
        }

        final MainActivity parent = (MainActivity) mContext;
        parent.setBeaconsFragment(this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_beacons_list, container, false);
        final ListView listView = (ListView) rootView.findViewById(R.id.listNearbyBeacons);
        mNearbyDevicesMessageList = new ArrayList<>();
        mEddystoneBeaconsAdapter = new EddystoneBeaconsAdapter(getActivity(), mNearbyDevicesMessageList);
        listView.setAdapter(mEddystoneBeaconsAdapter);
        mPermissions = (TextView) rootView.findViewById(R.id.ble_permission);
        mNearbyImage = (ImageView) rootView.findViewById(R.id.img_nearby);

        mParentIntent = new Intent(getActivity(), MainActivity.class);
        mNotificationManager = NotificationManagerCompat.from(getActivity());
        mScanForNearbyInBackground = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.nearby_settings_key), false);
       /* mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder().setPermissions(NearbyPermissions.BLE).build())
                .addConnectionCallbacks(this)
                //.addOnConnectionFailedListener(this)
                .enableAutoManage(getActivity(), this)
                .build();*/

        mPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String message = mPermissions.getText().toString();
                if(message.equals(getString(R.string.enable_ble))){
                    if(!isBleEnabled())
                        enableBle();
                }

            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().registerReceiver(mBluetoothStateChange, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_nearby_settings, menu);

        //onOptionsItemSelected is handled here for the tool bar icon
        //because a view target has to be passed in to creating the material show case view
        MenuItem item = menu.findItem(R.id.action_nearby_settings);
        item.setActionView(R.layout.menu_nearby);
        mNearbySettings = (ImageView) item.getActionView();
        mNearbySettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nearby_settings = new Intent(getActivity(), NearbySettingsActivity.class);
                startActivityForResult(nearby_settings, REQUEST_NEARBY_SETTINGS);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        //handled above
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        getMetaData();
    }

    @Override
    public void onStop() {
        super.onStop();
        unsubscribe();
        disconnectFromGoogleApiClient();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mBluetoothStateChange);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final MainActivity parent = (MainActivity) mContext;
        parent.setBeaconsFragment(null);

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBackgroundScanReceiver);

        //deallocating image resources
        mNearbyImage.setImageBitmap(null);
        mNearbyImage.setImageDrawable(null);
        if(mNearbySettings != null) {
            mNearbySettings.setImageBitmap(null);
            mNearbySettings.setImageDrawable(null);
        }
        mPermissions = null;
        mNotificationManager = null;
        mPendingIntent = null;
        mParentIntent = null;
        mNearbyDevicesMessageList = null;
        mEddystoneBeaconsAdapter = null;
    }

    private void getMetaData(){
        String TAG = "Example Meta-Data";
        try {
            ApplicationInfo applicationInfo = getActivity().getPackageManager().getApplicationInfo(getActivity().getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            String nearbyApiKey = bundle.getString("com.google.android.nearby.messages.API_KEY");
            if(!nearbyApiKey.equals(Utils.UTILS_API_KEY)){
                if (checkIfVersionIsMarshmallowOrAbove()) {
                    final String [] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
                    boolean flag = ensurePermission(permissions);
                    Log.v("BEACON", "Permission flags: " + flag);
                } else {
                    if (!isBleEnabled()) {
                        final Intent bluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(bluetoothEnable, REQUEST_ENABLE_BT);
                    } else {
                        updateBlePermissionStatus(true);
                        //connectToGoogleApiClient();
                    }
                }


            } else {
                mPermissions.setText(getString(R.string.nearby_api_message));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG,
                    "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e(TAG,
                    "Failed to load meta-data, NullPointer: " + e.getMessage());
        }
    }

    private void createShowcaseForNearbySettings(){
        if(((MainActivity)mContext).getTabPosition() == 0)
            new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(mNearbySettings)
                    .setDismissText(getString(R.string.got_it))
                    .setContentText(getString(R.string.nearby_Settings_showcase))
                    .setDelay(1000)
                    .singleUse(NEARBY_SETTINGS_HELP)
                    .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_NEARBY_SETTINGS:
                if (resultCode == Activity.RESULT_OK){
                    final boolean tutorials_enabled = data.getExtras().getBoolean("TUTORIALS_ENABLED", false);
                    if(tutorials_enabled){
                        createShowcaseForNearbySettings();
                    }
                    updateNearbyScanning();
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    //connectToGoogleApiClient();
                } else {
                    updateBlePermissionStatus(false);
                }
                break;
            case REQUEST_RESOLVE_ERROR:
                if(resultCode == Activity.RESULT_OK) {
                    //connectToGoogleApiClient();
                }
                else {
                    updateNearbyPermissionStatus(false);
                    Toast.makeText(getActivity(), getString(R.string.grant_location_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_REQ_CODE: {
                if(permissions.length > 0)
                    for(int i = 0; i < permissions.length; i++){
                        if(Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])){
                            if(grantResults[i] == PackageManager.PERMISSION_GRANTED)
                                onPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION);
                            else {
                                updateLocationPermissionStatus(false);
                                Toast.makeText(getActivity(), R.string.rationale_permission_denied, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                break;
            }
        }
    }

    @Override
    protected void onPermissionGranted(final String permission) {
        // Now, when the permission is granted, we may start scanning for beacons.
        // We bind even if the FAB was clicked.
        if(Manifest.permission.ACCESS_COARSE_LOCATION.equalsIgnoreCase(permission)){
            /*if(!isBleEnabled()){
                enableBle();
            } else {
                updateBlePermissionStatus(true);
                connectToGoogleApiClient();
            }*/
            buildGoogleApiClient();
        } else {
            updateLocationPermissionStatus(false);
        }
    }

    private synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(Nearby.MESSAGES_API/*, new MessagesOptions.Builder()
                            .setPermissions(NearbyPermissions.BLE)
                            .build()*/)
                    .addConnectionCallbacks(this)
                    .enableAutoManage(getActivity(), this)
                    .build();
        }
    }

    @Override
    public void onCancelRequestPermission() {
        super.onCancelRequestPermission();
        updateLocationPermissionStatus(false);
    }

    public void updateAdapter(boolean clearAdapter){
        if(clearAdapter)
            mEddystoneBeaconsAdapter.clear();
        mEddystoneBeaconsAdapter.notifyDataSetChanged();
    }

    public void updateBlePermissionStatus(boolean flag){
        if(!flag) {
            mPermissions.setText(getString(R.string.enable_ble));
            mPermissions.setVisibility(View.VISIBLE);
        } else {
            if(mNearbyPermissionGranted)
                mPermissions.setVisibility(View.GONE);
            else {
                updateNearbyPermissionStatus(mNearbyPermissionGranted);
            }
        }
    }

    public void updateLocationPermissionStatus(boolean locationPermissions){
        if(!locationPermissions) {
            mPermissions.setText(getString(R.string.grant_location_permission));
            mPermissions.setVisibility(View.VISIBLE);
        } else {
            if(isBleEnabled())
                mPermissions.setVisibility(View.GONE);
            else updateBlePermissionStatus(false);
        }
    }

    public void updateNearbyPermissionStatus(boolean flag){
        mNearbyPermissionGranted = flag;
        if(!flag) {
            mPermissions.setText(getString(R.string.grant_nearby_permission));
            mPermissions.setVisibility(View.VISIBLE);
        } else {
            mPermissions.setVisibility(View.GONE);
        }
    }

    /**
     * Checks whether the Bluetooth adapter is enabled.
     */
    private boolean isBleEnabled() {
        final BluetoothManager bm = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter ba = bm.getAdapter();
        return ba != null && ba.isEnabled();
    }

    /**
     * Tries to start Bluetooth adapter.
     */
    private void enableBle() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void connectToGoogleApiClient(){
        checkGoogleApiClientConnectionStateAndSubscribe();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "Connected ");
        updateNearbyPermissionStatus(true);
        subscribe();
        //connectToGoogleApiClient();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "Connection susspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

        Log.v(TAG, "GoogleApiClient failed");
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.v(TAG, "GoogleApiClient connection failed");
            updateNearbyPermissionStatus(false);
        }
    }


    public void checkGoogleApiClientConnectionStateAndSubscribe(){
        if(mGoogleApiClient != null && !mGoogleApiClient.isConnected()){
            if(!mGoogleApiClient.isConnecting()){
                mGoogleApiClient.connect();
            }
        } else {
            createShowcaseForNearbySettings();
            subscribe();
        }
    }

    private void subscribe() {

        readProjectInformation();
        Log.v(TAG, "Subscribing to beacons with namespace: " + mProject.getProjectNamespace());
        updateNearbyScanning();
        MessageFilter filter = new MessageFilter.Builder()
                //.includeNamespacedType(mProject.getProjectNamespace(), "string")
                .includeAllMyTypes()
                .build();

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else if (!mScanForNearbyInBackground) {
            SubscribeOptions options = new SubscribeOptions.Builder().setStrategy(Strategy.BLE_ONLY).setFilter(filter).build();
            Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        Log.v(TAG, "Subscribed successfully for foreground scanning.");
                    } else {
                        Log.v(TAG, "Could not subscribe.");
                        //handleUnsuccessfulNearbyResult(status);
                    }
                }
            });

        } else {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBackgroundScanReceiver, createIntentFilters());
            SubscribeOptions options = new SubscribeOptions.Builder().setStrategy(Strategy.BLE_ONLY).setFilter(filter).build();
            Nearby.Messages.subscribe(mGoogleApiClient, getPendingIntent(), options)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.v(TAG, "Subscribed successfully for background scanning.");
                            } else {
                                Log.v(TAG, "Could not subscribe.");
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
    }

    private void unsubscribe(){
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
        }
    }

    private void disconnectFromGoogleApiClient(){

        /*if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            unsubscribe();
            mGoogleApiClient.disconnect();
            Log.v(TAG, "is connected? " + mGoogleApiClient.isConnected());
        }*/
        mNearbyDevicesMessageList.clear();
        mNotificationManager.cancelAll();

    }

    private PendingIntent getPendingIntent() {
        PendingIntent pendingIntent = PendingIntent.getService(getActivity(), 0,
                getBackgroundSubscribeServiceIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public Intent getBackgroundSubscribeServiceIntent() {
        return new Intent(getActivity(), NearbyBackgroundService.class);
    }

    private void handleUnsuccessfulNearbyResult(Status status) {
        Log.v(TAG, "Processing error, status = " + status);
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (status.hasResolution()) {
            try {
                mResolvingError = true;
                status.startResolutionForResult(getActivity(),
                        REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                mResolvingError = false;
                Log.v(TAG, "Failed to resolve error status.", e);
            }
        } else {
            if (status.getStatusCode() == CommonStatusCodes.NETWORK_ERROR) {
                Toast.makeText(getActivity(),
                        "No connectivity, cannot proceed. Fix in 'Settings' and try again.",
                        Toast.LENGTH_LONG).show();
            } else {
                // To keep things simple, pop a toast for all other error messages.
                Toast.makeText(getActivity(), "Unsuccessful: " +
                        status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private final MessageListener mMessageListener = new MessageListener() {
        @Override
        public void onFound(Message message) {
            String nearbyMessage = new String(message.getContent(), Charset.forName("UTF-8"));
            Log.v(TAG, "Found message: " + message.getNamespace());
            displayNotification(message);
        }

        @Override
        public void onLost(Message message) {
            String nearbyMessage = new String(message.getContent(), Charset.forName("UTF-8"));
            Log.v(TAG, "Lost message: " + nearbyMessage);
            updateNotification(message);
        }
    };

    private void displayNotification(Message message){
        if (!checkIfnearbyDeviceAlreadyExists(new String(message.getContent(), Charset.forName("UTF-8")))) {
            Log.v(TAG, "Adding message");
            mNearbyDevicesMessageList.add(message);
            updateAdapter(false);
            Log.v(TAG, "count after adding: " + mNearbyDevicesMessageList.size());
            createNotification();
        }
    }

    private void createNotification() {

        if(mNearbyDevicesMessageList.size() == 0){
            mNotificationManager.cancelAll();
            return;
        }

        final ArrayList<Message> nearbyMessageList = loadNearbyMessageListForNotification();
        mParentIntent.putExtra(NEARBY_DEVICE_DATA, nearbyMessageList);
        mParentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mPendingIntent = PendingIntent.getActivities(getActivity(), OPEN_ACTIVITY_REQ, new Intent[]{mParentIntent}, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getActivity())
                        .setSmallIcon(R.drawable.ic_eddystone)
                        .setColor(ContextCompat.getColor(getActivity(), R.color.actionBarColor))
                        .setContentTitle(getString(R.string.app_name))
                        .setContentIntent(mPendingIntent);

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
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void updateNotification(Message message){
        if (checkIfnearbyDeviceAlreadyExists(new String(message.getContent(), Charset.forName("UTF-8")))) {
            Log.v(TAG, "removing message: " + message);
            removeLostNearbyMessage(new String(message.getContent(), Charset.forName("UTF-8")));
            Log.v(TAG, "count after removing: " + mNearbyDevicesMessageList.size());
            createNotification();
        }
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

    private void updateBeaconsAdapter(Message message){
        if (!checkIfnearbyDeviceAlreadyExists(new String(message.getContent(), Charset.forName("UTF-8")))) {
            Log.v(TAG, "Adding message");
            mNearbyDevicesMessageList.add(message);
            updateAdapter(false);
        }
    }

    private void removeLostNearbyMessage(String nearbyDeviceMessage){
        String message;
        for(int i = 0; i < mNearbyDevicesMessageList.size(); i++){
            message = new String(mNearbyDevicesMessageList.get(i).getContent(), Charset.forName("UTF-8"));
            if(nearbyDeviceMessage.equals(message)){
                mNearbyDevicesMessageList.remove(i);
                updateAdapter(false);
                break;
            }
        }
    }

    public void updateNearbyScanning(){
        boolean flag = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.nearby_settings_key), false);
        if(mScanForNearbyInBackground != flag){
            mScanForNearbyInBackground = flag;
            /*unsubscribe();
            disconnectFromGoogleApiClient();*/
            mNotificationManager.cancelAll();
            mNearbyDevicesMessageList.clear();
            updateAdapter(true);
            //connectToGoogleApiClient();
        }
    }

    private IntentFilter createIntentFilters() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(NEW_MESSAGE_FOUND);
        filter.addAction(MESSAGE_LOST);
        return filter;
    }

    public void readProjectInformation() {
        SharedPreferences sp = getActivity().getSharedPreferences(Utils.PROJECT_INFO, Context.MODE_PRIVATE);
        final String projectName = sp.getString(Utils.PROJECT_NAME, "");
        final String projectId = sp.getString(Utils.PROJECT_ID, "");
        final String projectNamespace = sp.getString(Utils.PROJECT_NAMESPACE, "");
        mProject = new Project(projectName, projectId, projectNamespace);
    }

    private final BroadcastReceiver mBluetoothStateChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // This will be executed only once
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_TURNING_ON:
                case BluetoothAdapter.STATE_ON:
                    updateBlePermissionStatus(true);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_OFF:
                    updateBlePermissionStatus(false);
                    break;
            }
        }
    };

    private boolean checkIfVersionIsMarshmallowOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
