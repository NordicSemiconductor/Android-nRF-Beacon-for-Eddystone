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
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
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

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.nio.charset.Charset;
import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.AuthorizedServiceTask;
import no.nordicsemi.android.nrfbeacon.nearby.EddystoneBeaconsAdapter;
import no.nordicsemi.android.nrfbeacon.nearby.MainActivity;
import no.nordicsemi.android.nrfbeacon.nearby.NearbyBackgroundService;
import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.common.BaseFragment;
import no.nordicsemi.android.nrfbeacon.nearby.common.PermissionRationaleDialogFragment;
import no.nordicsemi.android.nrfbeacon.nearby.settings.NearbySettingsActivity;
import no.nordicsemi.android.nrfbeacon.nearby.util.NetworkUtils;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

public class BeaconsFragment extends BaseFragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, PermissionRationaleDialogFragment.PermissionDialogListener {

    public static final String EXTRA_ADAPTER_POSITION = "no.nordicsemi.android.nrfbeacon.extra.adapter_position";
    public static final String TAG = "BEACON";
    private static final String ACCOUNT_NAME_PREF = "userAccount";
    private static final String SHARED_PREFS_NAME = "nrfNearbyInfo";
    private static final String NEARBY_MESSAGE = "no.nordicsemi.android.nrfbeacon.nearby.NEARBY_MESSAGE";
    public static final String NEARBY_DEVICE_DATA = "NEARBY_DEVICE_DATA";
    private static final String AUTH_PROXIMITY_API = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";

    private final static int OPEN_ACTIVITY_REQ = 195; // random
    static final int REQUEST_CODE_USER_ACCOUNT = 1002;
    private static final int REQUEST_NEARBY_SETTINGS = 252;
    private static final int REQUEST_PERMISSION_REQ_CODE = 76; // any 8-bit number
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_RESOLVE_ERROR = 261; //random
    private static final int NOTIFICATION_ID = 1;
    public static final String NEARBY_SETTINGS_HELP = "NEARBY_SETTINGS_HELP";

    public ArrayList<Message> mNearbyDevicesMessageList;
    private int mSelectedTabPosition = 0;
    private EddystoneBeaconsAdapter mEddystoneBeaconsAdapter;
    private ImageView mNearbyPermission;
    private TextView mNearbyPermissions;
    private boolean mNearbyPermissionGranted = false;
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError;
    private boolean mScanForNearbyInBackground = false;
    private PendingIntent mPendingIntent;
    private Intent mParentIntent;
    private NotificationManagerCompat mNotificationManager;
    private Context mContext;
    private ImageView mNearbySettings;

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
    public void onDestroy() {
        super.onDestroy();
        final MainActivity parent = (MainActivity) mContext;
        parent.setBeaconsFragment(null);

        //deallocating image resources
        mNearbyPermission.setImageBitmap(null);
        mNearbyPermission.setImageDrawable(null);
        if(mNearbySettings != null) {
            mNearbySettings.setImageBitmap(null);
            mNearbySettings.setImageDrawable(null);
        }
        mNearbyPermissions = null;
        mGoogleApiClient = null;
        mNotificationManager = null;
        mPendingIntent = null;
        mParentIntent = null;
        mNearbyDevicesMessageList = null;
        mEddystoneBeaconsAdapter = null;
    }

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
        mNearbyPermissions = (TextView) rootView.findViewById(R.id.tvNearbyPermission);
        mNearbyPermission = (ImageView) rootView.findViewById(R.id.imageView);

        mParentIntent = new Intent(getActivity(), MainActivity.class);
        mNotificationManager = NotificationManagerCompat.from(getActivity());
        mScanForNearbyInBackground = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.nearby_settings_key), false);
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        return rootView;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        mNearbyPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ensurePermission(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION})) {
                    getNearbyPermissionStatusAndSubscribe();
                    mNearbyPermissions.setClickable(false);
                } else {
                    Toast.makeText(getActivity(), "Permission have not been granted", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        final String [] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.GET_ACCOUNTS};
        boolean flag = ensurePermission(permissions);
        Log.v("BEACON", "Permission flags: " + flag);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!mScanForNearbyInBackground)
        {
            unsubscribe();
            disconnectFromGoogleApiClient();
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
                if (resultCode == Activity.RESULT_OK)
                    connectToGoogleApiClient();
                else
                    onDestroy();
                break;
            case REQUEST_CODE_USER_ACCOUNT:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        final String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                        setUserAccountName(accountName);
                        // The first time the account tries to contact the beacon service we'll pop a dialog
                        // asking the user to authorize our activity. Ensure that's handled cleanly here, rather
                        // than when the scan tries to fetch the status of every beacon within range.
                        Account [] accountsList = AccountManager.get(getActivity()).getAccounts();
                        for (Account account : accountsList){
                            if (accountName.equals(account.name)){
                                if(NetworkUtils.checkNetworkConnectivity(getActivity()))
                                    new AuthorizedServiceTask(getActivity(), account, AUTH_PROXIMITY_API).execute();
                                else
                                    Toast.makeText(getActivity(), getString(R.string.check_internet_connectivity), Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }
                }
                connectToGoogleApiClient();
                break;
            case REQUEST_RESOLVE_ERROR:
                if(resultCode == Activity.RESULT_OK) {
                    getNearbyPermissionStatusAndSubscribe();
                }
                else Toast.makeText(getActivity(), getString(R.string.rationale_permission_denied), Toast.LENGTH_SHORT).show();
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
                            else Toast.makeText(getActivity(), R.string.rationale_permission_denied, Toast.LENGTH_SHORT).show();
                        } else if (Manifest.permission.GET_ACCOUNTS.equals(permissions[i])){
                            if(grantResults[i] == PackageManager.PERMISSION_GRANTED)
                                onPermissionGranted(Manifest.permission.GET_ACCOUNTS);
                            else Toast.makeText(getActivity(), R.string.rationale_permission_denied, Toast.LENGTH_SHORT).show();
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
            if(!isBleEnabled()){
                enableBle();
            } else connectToGoogleApiClient();
        } else if (Manifest.permission.GET_ACCOUNTS.equalsIgnoreCase(permission)){
            selectUserAccount();
        }
    }

    public void updateAdapter(boolean clearAdapter){
        if(clearAdapter)
            mEddystoneBeaconsAdapter.clear();
        mEddystoneBeaconsAdapter.notifyDataSetChanged();
    }

    public void updateNearbyPermissionStatus(boolean flag){
        mNearbyPermissionGranted = flag;
        if(!mNearbyPermissionGranted) {
            mNearbyPermission.setVisibility(View.GONE);
            mNearbyPermissions.setVisibility(View.VISIBLE);
            mNearbyPermissions.setClickable(true);
        }
        else {
            mNearbyPermission.setVisibility(View.VISIBLE);
            mNearbyPermissions.setVisibility(View.GONE);
        }
    }

    public void prepareForScanning(){
        final String [] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.GET_ACCOUNTS};
        if (ensurePermission(permissions)) {
            if (!isBleEnabled()) {
                enableBle();
                connectToGoogleApiClient();
            } else connectToGoogleApiClient();
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

    /**
     * Selects the primary google account to provide authorization to the proximity api.
     */
    private void selectUserAccount() {
        String accountName = getUserAccountName();
        if (accountName == null) {
            String[] accountTypes = new String[]{"com.google"};
            Intent intent = AccountPicker.newChooseAccountIntent(
                    null, null, accountTypes, false, null, null, null, null);
            startActivityForResult(intent, REQUEST_CODE_USER_ACCOUNT);
        }
    }

    private String getUserAccountName(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(ACCOUNT_NAME_PREF, null);
    }

    private void setUserAccountName(final String accountName){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ACCOUNT_NAME_PREF, accountName).apply();
    }

    private void connectToGoogleApiClient(){
        getNearbyPermissionStatusAndSubscribe();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "Connected ");
        getNearbyPermissionStatusAndSubscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "Connection susspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "Connection failed: " + connectionResult.getErrorMessage());
    }

    public void getNearbyPermissionStatusAndSubscribe(){
        if(mGoogleApiClient != null && !mGoogleApiClient.isConnected()){
            if(!mGoogleApiClient.isConnecting()){
                mGoogleApiClient.connect();
            }
        } else {
            Nearby.Messages.getPermissionStatus(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        createShowcaseForNearbySettings();
                        updateNearbyPermissionStatus(true);
                        subscribe();
                    } else if (status.getStatusCode() == NearbyMessagesStatusCodes.APP_NOT_OPTED_IN) {
                        try {
                            status.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
                        } catch (IntentSender.SendIntentException e) {
                            mResolvingError = false;
                            Log.i(TAG, "Failed to resolve error status.", e);
                        }
                    }
                }
            });
        }
    }

    private void subscribe() {
        Log.v(TAG, "Subscribing to beacons");
        MessageFilter filter = new MessageFilter.Builder()
                .includeNamespacedType("nrf-nearby-1100", "string")
                .build();
        if (!mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else if (!mScanForNearbyInBackground) {
            SubscribeOptions options = new SubscribeOptions.Builder().setStrategy(Strategy.BLE_ONLY).setCallback(new SubscribeCallback() {
                @Override
                public void onExpired() {
                    Log.v(TAG, "nearby disabled?");
                }
            })/*setFilter(filter)*/.build();
            Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        Log.i(TAG, "Subscribed successfully for foreground scanning.");
                    } else {
                        Log.i(TAG, "Could not subscribe.");
                        handleUnsuccessfulNearbyResult(status);
                    }
                }
            });

        } else {
            SubscribeOptions options = new SubscribeOptions.Builder().setStrategy(Strategy.BLE_ONLY).build();
            Nearby.Messages.subscribe(mGoogleApiClient, getPendingIntent(), options)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Subscribed successfully for background scanning.");
                            } else {
                                Log.i(TAG, "Could not subscribe.");
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
    }

    private void unsubscribe(){
        if(mGoogleApiClient.isConnected()) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
        }
    }

    private void disconnectFromGoogleApiClient(){

        if(mGoogleApiClient.isConnected()) {
            unsubscribe();
            mGoogleApiClient.disconnect();
            Log.v(TAG, "is connected? " + mGoogleApiClient.isConnected());
        }
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
        Log.i(TAG, "Processing error, status = " + status);
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
                Log.i(TAG, "Failed to resolve error status.", e);
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
            Log.i(TAG, "Found message via PendingIntent: " + nearbyMessage);
            displayNotification(message);
        }

        @Override
        public void onLost(Message message) {
            String nearbyMessage = new String(message.getContent(), Charset.forName("UTF-8"));
            Log.i(TAG, "Lost message via PendingIntent: " + nearbyMessage);
            updateNotification(message);
        }
    };

    private void displayNotification(Message message){
        if (!checkIfnearbyDeviceAlreadyExists(new String(message.getContent(), Charset.forName("UTF-8")))) {
            Log.i(TAG, "Adding message");
            mNearbyDevicesMessageList.add(message);
            updateAdapter(false);
            Log.i(TAG, "count after adding: " + mNearbyDevicesMessageList.size());
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
            Log.i(TAG, "removing message: " + message);
            removeLostNearbyMessage(new String(message.getContent(), Charset.forName("UTF-8")));
            Log.i(TAG, "count after removing: " + mNearbyDevicesMessageList.size());
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
            unsubscribe();
            disconnectFromGoogleApiClient();
            mNotificationManager.cancelAll();
            mNearbyDevicesMessageList.clear();
            updateAdapter(true);
            connectToGoogleApiClient();
        }
    }
}
