/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrfbeacon.nearby;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

public class UpdateService extends Service {
    private static final String TAG = "UpdateService";

    public final static String ACTION_STATE_CHANGED = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_STATE_CHANGED";
    public final static String ACTION_GATT_ERROR = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_GATT_ERROR";
    public final static String ACTION_DONE = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_DONE";
    public final static String ACTION_UUID_READY = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_UUID_READY";

    public final static String ACTION_BROADCAST_CAPABILITIES = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_BROADCAST_CAPABILITIES";
    public final static String ACTION_ACTIVE_SLOT = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_ACTIVE_SLOT";
    public final static String ACTION_ADVERTISING_INTERVAL = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_ADVERTISING_INTERVAL";
    public final static String ACTION_RADIO_TX_POWER = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_RADIO_TX_POWER";
    public final static String ACTION_ADVANCED_ADVERTISED_TX_POWER = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_ADVANCED_ADVERTISED_TX_POWER";
    public final static String ACTION_LOCK_STATE = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_LOCK_STATE";
    public final static String ACTION_UNLOCK = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_UNLOCK";
    public final static String ACTION_ECDH_KEY = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_ECDH_KEY";
    public final static String ACTION_EID_IDENTITY_KEY = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_EID_IDENTITY_KEY";
    public final static String ACTION_READ_WRITE_ADV_SLOT = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_READ_WRITE_ADV_SLOT";
    public final static String ACTION_ADVANCED_FACTORY_RESET = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_ADVANCED_FACTORY_RESET";
    public final static String ACTION_ADVANCED_REMAIN_CONNECTABLE = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_ADVANCED_REMAIN_CONNECTABLE";
    public final static String ACTION_UNLOCK_BEACON = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_UNLOCK_BEACON";
    public final static String ACTION_DISSMISS_UNLOCK = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_DISSMISS_UNLOCK";
    public static final String ACTION_BROADCAST_ALL_SLOT_INFO = "no.nordicsemi.android.nrfbeacon.nearby.ACTION_BROADCAST_ALL_SLOT_INFO";


    public final static String EXTRA_DATA = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_DATA";
    public final static String EXTRA_FRAME_TYPE = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_FRAME_TYPE";
    public final static String EXTRA_NAMESPACE_ID = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_NAMESPACE_ID";
    public final static String EXTRA_INSTANCE_ID = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_INSTANCE_ID";
    public final static String EXTRA_URL = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_URL";
    public final static String EXTRA_CLOCK_VALUE = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_CLOCK_VALUE";
    public final static String EXTRA_TIMER_EXPONENT = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_TIMER_EXPONENT";
    public final static String EXTRA_EID = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_EID";
    public final static String EXTRA_VOLTAGE = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_VOLTAGE";
    public final static String EXTRA_BEACON_TEMPERATURE = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_BEACON_TEMPERATURE";
    public final static String EXTRA_PDU_COUNT = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_PDU_COUNT";
    public final static String EXTRA_TIME_SINCE_BOOT = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_TIME_SINCE_BOOT";
    public final static String EXTRA_ETLM = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_ETLM";
    public final static String EXTRA_SALT = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_SALT";
    public final static String EXTRA_MESSAGE_INTEGRITY_CHECK = "no.nordicsemi.android.nrfbeacon.nearby.EXTRA_MESSAGE_INTEGRITY_CHECK";

    private static final int EMPTY_SLOT = -1;
    private static final int TYPE_UID = 0x00;
    private static final int TYPE_URL = 0x10;
    private static final int TYPE_TLM = 0x20;
    private static final int TYPE_EID = 0x30;

    public static final int LOCKED = 0x00;
    public static final int UNLOCKED = 0x01;
    public static final int UNLOCKED_AUTOMATIC_RELOCK_DISABLED = 0x02;

    public final static int ERROR_UNSUPPORTED_DEVICE = -1;

    private int mConnectionState;
    public final static int STATE_DISCONNECTED = 0;
    public final static int STATE_CONNECTING = 1;
    public final static int STATE_DISCOVERING_SERVICES = 2;
    public final static int STATE_CONNECTED = 3;
    public final static int STATE_DISCONNECTING = 4;

    public final static int SERVICE_UUID = 1;
    public final static int SERVICE_MAJOR_MINOR = 2;
    public final static int SERVICE_CALIBRATION = 3;

    public static final UUID EDDYSTONE_GATT_CONFIG_SERVICE_UUID =                   new UUID(0xA3C875008ED34BDFL, 0x8A39A01BEBEDE295L);
    private static final UUID EDDYSTONE_BROADCAST_CAPABILITIES_UUID =               new UUID(0xA3C875018ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_ACTIVE_SLOT_UUID =                          new UUID(0xA3C875028ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_ADVERTISING_INTERVAL_UUID =                 new UUID(0xA3C875038ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_RADIO_TX_POWER_UUID =                       new UUID(0xA3C875048ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_ADVANCED_ADVERTISED_TX_POWER_UUID =         new UUID(0xA3C875058ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_LOCK_STATE_UUID =                           new UUID(0xA3C875068ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_UNLOCK_UUID =                               new UUID(0xA3C875078ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_ECDH_KEY_UUID =                             new UUID(0xA3C875088ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_EID_IDENTITY_KEY_UUID =                     new UUID(0xA3C875098ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_READ_WRITE_ADV_SLOT_UUID =                  new UUID(0xA3C8750A8ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_ADVANCED_FACTORY_RESET_UUID =               new UUID(0xA3C8750B8ed34bdfL, 0x8a39a01bebede295L);
    private static final UUID EDDYSTONE_ADVANCED_REMAIN_CONNECTABLE_UUID =          new UUID(0xA3C8750C8ed34bdfL, 0x8a39a01bebede295L);

    private BluetoothAdapter mAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mBroadcastCapabilitesCharacterisitc;
    private BluetoothGattCharacteristic mRadioTxPowerCharacteristic;
    private BluetoothGattCharacteristic mActiveSlotCharacteristic;
    private BluetoothGattCharacteristic mAdvertisingIntervalCharacteristic;
    private BluetoothGattCharacteristic mAdvancedAdvertisedTxPowerCharacteristic;
    private BluetoothGattCharacteristic mLockStateCharacteristic;
    private BluetoothGattCharacteristic mUnlockCharacteristic;
    private BluetoothGattCharacteristic mPublicEcdhKeyCharacteristic;
    private BluetoothGattCharacteristic mEidIdentityKeyCharacteristic;
    private BluetoothGattCharacteristic mReadWriteAdvSlotCharacteristic;
    private BluetoothGattCharacteristic mAdvancedFactoryResetCharacteristic;
    private BluetoothGattCharacteristic mAdvancedRemainConnectableCharacteristic;

    private Handler mHandler;
    private boolean mIsBeaconLocked = false;
    private final Queue<Request> mQueue = new LinkedList<Request>();
    private boolean mConfigureSlot = false;
    private boolean mReadlAllSlots = false;
    private int mSlotCounter = 0;

    private int mMaxSlots = -1;
    private int mMaxEidSlots = -1;
    private ArrayList<String> mActiveSlotsTypes = new ArrayList<>();
    private boolean mStartReadingInitialCharacteristics = false;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.v("BEACON", "Connection state change error: " + status);
                broadcastError(status);
                return;
            }

            Log.v("BEACON", "Connection state change: " + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setState(STATE_DISCOVERING_SERVICES);
                mActiveSlotsTypes.clear();
                // Attempts to discover services after successful connection.
                Log.v("BEACON", "delaying service discovery for 2s");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.v("BEACON", "Calling gatt.discoverServies");
                        gatt.discoverServices();
                    }
                }, 2500);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mQueue.clear();
                mActiveSlotsTypes.clear();
                setState(STATE_DISCONNECTED);
                refreshDeviceCache(gatt);
                if(gatt != null)
                    gatt.close();
                mBluetoothGatt = null;
                stopSelf();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.v("BEACON", "Service discovery error: " + status);
                broadcastError(status);
                return;
            }
            Log.v("BEACON", "onServices Discovered");
            // We have successfully connected
            setState(STATE_CONNECTED);

            // Search for config service
            final BluetoothGattService configService = gatt.getService(EDDYSTONE_GATT_CONFIG_SERVICE_UUID);
            if (configService == null) {
                // Config service is not present
                Log.v("BEACON", "Gatt error on service discovery: " + ERROR_UNSUPPORTED_DEVICE);
                broadcastError(ERROR_UNSUPPORTED_DEVICE);
                setState(STATE_DISCONNECTING);
                gatt.disconnect();
                return;
            }

            mBroadcastCapabilitesCharacterisitc = configService.getCharacteristic(EDDYSTONE_BROADCAST_CAPABILITIES_UUID);
            mRadioTxPowerCharacteristic = configService.getCharacteristic(EDDYSTONE_RADIO_TX_POWER_UUID);
            mActiveSlotCharacteristic = configService.getCharacteristic(EDDYSTONE_ACTIVE_SLOT_UUID);
            mAdvertisingIntervalCharacteristic = configService.getCharacteristic(EDDYSTONE_ADVERTISING_INTERVAL_UUID);
            mAdvancedAdvertisedTxPowerCharacteristic = configService.getCharacteristic(EDDYSTONE_ADVANCED_ADVERTISED_TX_POWER_UUID);
            mLockStateCharacteristic = configService.getCharacteristic(EDDYSTONE_LOCK_STATE_UUID);
            mUnlockCharacteristic = configService.getCharacteristic(EDDYSTONE_UNLOCK_UUID);
            mPublicEcdhKeyCharacteristic = configService.getCharacteristic(EDDYSTONE_ECDH_KEY_UUID);
            mEidIdentityKeyCharacteristic = configService.getCharacteristic(EDDYSTONE_EID_IDENTITY_KEY_UUID);
            mReadWriteAdvSlotCharacteristic = configService.getCharacteristic(EDDYSTONE_READ_WRITE_ADV_SLOT_UUID);
            mAdvancedFactoryResetCharacteristic = configService.getCharacteristic(EDDYSTONE_ADVANCED_FACTORY_RESET_UUID);
            mAdvancedRemainConnectableCharacteristic = configService.getCharacteristic(EDDYSTONE_ADVANCED_REMAIN_CONNECTABLE_UUID);

            add(RequestType.READ_CHARACTERISTIC, mLockStateCharacteristic);
            Log.v("BEACON", "Service discovery complete");
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.v("BEACON", "Characteristic write error: " + status);
                broadcastError(status);
                return;
            }

            if (EDDYSTONE_ACTIVE_SLOT_UUID.equals(characteristic.getUuid())) {
                if(mReadlAllSlots)
                    add(RequestType.READ_CHARACTERISTIC, mActiveSlotCharacteristic);
                else if (!mStartReadingInitialCharacteristics) {
                    add(RequestType.READ_CHARACTERISTIC, mActiveSlotCharacteristic);
                    startReadingCharacteristicsForActiveSlot();
                } else {
                    mStartReadingInitialCharacteristics = false;
                    add(RequestType.READ_CHARACTERISTIC, mActiveSlotCharacteristic);
                }

            } else if (EDDYSTONE_ADVERTISING_INTERVAL_UUID.equals(characteristic.getUuid())) {
                add(RequestType.READ_CHARACTERISTIC, mAdvertisingIntervalCharacteristic);
            } else if (EDDYSTONE_RADIO_TX_POWER_UUID.equals(characteristic.getUuid())) {
                add(RequestType.READ_CHARACTERISTIC, mRadioTxPowerCharacteristic);
            } else if (mAdvancedAdvertisedTxPowerCharacteristic != null && EDDYSTONE_ADVANCED_ADVERTISED_TX_POWER_UUID.equals(characteristic.getUuid())) {
                add(RequestType.READ_CHARACTERISTIC, mAdvancedAdvertisedTxPowerCharacteristic);
            } else if (EDDYSTONE_LOCK_STATE_UUID.equals(characteristic.getUuid())) {
                add(RequestType.READ_CHARACTERISTIC, mLockStateCharacteristic);
            } else if (EDDYSTONE_UNLOCK_UUID.equals(characteristic.getUuid())) {
                if (mIsBeaconLocked)
                    add(RequestType.READ_CHARACTERISTIC, mLockStateCharacteristic);
            } else if (EDDYSTONE_READ_WRITE_ADV_SLOT_UUID.equals(characteristic.getUuid())) {
                startReadingCharacteristicsForActiveSlot();
            } else if (mAdvancedFactoryResetCharacteristic != null && EDDYSTONE_ADVANCED_FACTORY_RESET_UUID.equals(characteristic.getUuid())) {
                broadcastAdvancedFactoryReset(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            } else if (mAdvancedRemainConnectableCharacteristic != null && EDDYSTONE_ADVANCED_REMAIN_CONNECTABLE_UUID.equals(characteristic.getUuid())) {
                add(RequestType.READ_CHARACTERISTIC, mAdvancedFactoryResetCharacteristic);
            }
            processNext();
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logw("Characteristic read error: " + status);
                broadcastError(status);
                return;
            }

            if (EDDYSTONE_BROADCAST_CAPABILITIES_UUID.equals(characteristic.getUuid())) {
                broadcastBeaconCapabilities(characteristic.getValue());
                startReadingAllActiveSlots(characteristic.getValue());
            } else if (EDDYSTONE_ACTIVE_SLOT_UUID.equals(characteristic.getUuid())) {
                if(mReadlAllSlots){
                    mSlotCounter = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    if(mSlotCounter <= mMaxSlots - 1) {
                        add(RequestType.READ_CHARACTERISTIC, mReadWriteAdvSlotCharacteristic);
                    } else stopReadingAllActiveSlots();
                } else	broadcastActiveSlot(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            } else if (EDDYSTONE_ADVERTISING_INTERVAL_UUID.equals(characteristic.getUuid())) {
                broadcastAdvertisingInterval(ParserUtils.getIntValue(characteristic.getValue(), 0, ParserUtils.FORMAT_UINT16_BIG_INDIAN));
            } else if (EDDYSTONE_RADIO_TX_POWER_UUID.equals(characteristic.getUuid())) {
                broadcastRadioTxPower(characteristic.getValue());
            } else if (EDDYSTONE_ADVANCED_ADVERTISED_TX_POWER_UUID.equals(characteristic.getUuid())) {
                broadcastAdvancedAdvertisedTxPower(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            } else if (EDDYSTONE_LOCK_STATE_UUID.equals(characteristic.getUuid())) {
                Log.v("Beacon", "Lock state: " + ParserUtils.getIntValue(characteristic.getValue(), 0, BluetoothGattCharacteristic.FORMAT_UINT8));
                broadcastLockState(ParserUtils.getIntValue(characteristic.getValue(), 0, BluetoothGattCharacteristic.FORMAT_UINT8));
            } else if (EDDYSTONE_UNLOCK_UUID.equals(characteristic.getUuid())) {
                broadcastUnlockRequest(characteristic.getValue());
            } else if (EDDYSTONE_ECDH_KEY_UUID.equals(characteristic.getUuid())) {
                broadcastEcdhKey(characteristic.getValue());
            } else if (EDDYSTONE_EID_IDENTITY_KEY_UUID.equals(characteristic.getUuid())) {
                broadcastEidIdentityKey(characteristic.getValue());
            } else if (EDDYSTONE_READ_WRITE_ADV_SLOT_UUID.equals(characteristic.getUuid())) {
                broadcastReadWriteAdvSlot(characteristic.getValue());
            } else if (EDDYSTONE_ADVANCED_REMAIN_CONNECTABLE_UUID.equals(characteristic.getUuid())) {
                broadcastAdvancedRemainConnectable(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            }
            processNext();
        }
    };


    public class ServiceBinder extends Binder {

        public byte[] mBeaconLockCode;

        /**
         * Connects to the service. The bluetooth device must have been passed during binding to the service in {@link UpdateService#EXTRA_DATA} field.
         *
         * @return <code>true</code> if connection process has been initiated
         */
        public boolean connect() {
            if (mAdapter == null) {
                logw("BluetoothAdapter not initialized or unspecified address.");
                return false;
            }

            if (mBluetoothDevice == null) {
                logw("Target device not specified. Start service with the BluetoothDevice set in EXTRA_DATA field.");
                return false;
            }

            // the device may be already connected
            if (mConnectionState == STATE_CONNECTED) {
                return true;
            }

            setState(STATE_CONNECTING);
            mBluetoothGatt = mBluetoothDevice.connectGatt(UpdateService.this, false, mGattCallback);
            return true;
        }

        /**
         * Disconnects from the device and closes the Bluetooth GATT object afterwards.
         */
        public void disconnectAndClose() {
            // This sometimes happen when called from UpdateService.ACTION_GATT_ERROR event receiver in UpdateFragment.
            if (mBluetoothGatt == null)
                return;

            setState(STATE_DISCONNECTING);
            mBluetoothGatt.disconnect();

            // Sometimes the connection gets error 129 or 133. Calling disconnect() method does not really disconnect... sometimes the connection is already broken.
            // Here we have a security check that notifies UI about disconnection even if onConnectionStateChange(...) has not been called.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mConnectionState == STATE_DISCONNECTING)
                        mGattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED);
                }
            }, 1500);
        }

        /**
         * Reads all the values from the device, one by one.
         *
         * @return <code>true</code> if at least one required characteristic has been found on the beacon.
         */
        public boolean read() {
            if (mBluetoothGatt == null)
                return false;

            if (mBroadcastCapabilitesCharacterisitc != null) {
                mBluetoothGatt.readCharacteristic(mBroadcastCapabilitesCharacterisitc);
                return true;
            } else if (mActiveSlotCharacteristic != null) {
                mBluetoothGatt.readCharacteristic(mActiveSlotCharacteristic);
                return true;
            } else if (mAdvertisingIntervalCharacteristic != null) {
                mBluetoothGatt.readCharacteristic(mAdvertisingIntervalCharacteristic);
                return true;
            }
            return false;
        }

        /**
         * Returns <code>true</code> if the beacon supports the advanced configuration.
         */
        public boolean isAdvancedSupported() {
            return mAdvancedAdvertisedTxPowerCharacteristic != null || mLockStateCharacteristic != null || mUnlockCharacteristic != null;
        }

        public int getState() {
            return mConnectionState;
        }

        //Unlocking the beacon by writing to the unclock characteristic which is called when a client connects to the beacon's configuration service
        public void unlockBeacon(final byte[] encryptedLockCode, final byte[] beaconLockCode) {
            mBeaconLockCode = beaconLockCode;
            if(mUnlockCharacteristic.setValue(encryptedLockCode)) {
                add(RequestType.WRITE_CHARACTERISTIC, mUnlockCharacteristic);
            }
        }

        //Changing active slot to the selected active slot
        public void changeToSelectedActiveSlot(final byte [] position) {
            if(mActiveSlotCharacteristic.setValue(position)) {
                add(RequestType.WRITE_CHARACTERISTIC, mActiveSlotCharacteristic);
            }
        }

        public void configureActiveSlot(final byte [] newSlotData, final String frameType) {
            if(mReadWriteAdvSlotCharacteristic.setValue(newSlotData)){
                mConfigureSlot = true;
                final int activeSlot = getActiveSlot();
                mActiveSlotsTypes.set(activeSlot, frameType); //Update the slotList on new slot configuration
                if(!frameType.equals("EID"))
                    add(RequestType.WRITE_CHARACTERISTIC, mReadWriteAdvSlotCharacteristic);
                else
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            add(RequestType.WRITE_CHARACTERISTIC, mReadWriteAdvSlotCharacteristic);
                        }
                    }, 100);
            }
        }

        public void configureRadioTxPower(byte[] radioTxPower) {
            if(mRadioTxPowerCharacteristic.setValue(radioTxPower)){
                add(RequestType.WRITE_CHARACTERISTIC, mRadioTxPowerCharacteristic);
            }
        }

        public void configureAdvancedAdvertisedTxPower(final byte[] radioTxPower) {
            if(mAdvancedAdvertisedTxPowerCharacteristic.setValue(radioTxPower)){
                add(RequestType.WRITE_CHARACTERISTIC, mAdvancedAdvertisedTxPowerCharacteristic);
            }
        }
        public void configureAdvertistingInterval(byte[] advertisingInterval) {
            if(mAdvertisingIntervalCharacteristic.setValue(advertisingInterval))
                add(RequestType.WRITE_CHARACTERISTIC, mAdvertisingIntervalCharacteristic);
        }

        public void lockBeacon(byte[] lockCode) {
            if(mLockStateCharacteristic.setValue(lockCode)){
                add(RequestType.WRITE_CHARACTERISTIC, mLockStateCharacteristic);
            }
        }

        public void startReadingCharacteristicsForActiveSlot() {
            add(RequestType.READ_CHARACTERISTIC, mAdvertisingIntervalCharacteristic);
            add(RequestType.READ_CHARACTERISTIC, mRadioTxPowerCharacteristic);
            add(RequestType.READ_CHARACTERISTIC, mReadWriteAdvSlotCharacteristic);
        }

        public byte [] getBroadcastCapabilities(){
            final BluetoothGattCharacteristic characteristic = mBroadcastCapabilitesCharacterisitc;
            if(characteristic != null){
                final byte [] data = characteristic.getValue();
                if(data == null || data.length < 7){
                    return null;
                }
                return data;
            }
            return null;
        }

        /**
         * Obtains the cached value of the active slot characteristic. If the value has not been obtained yet using {@link #read()}, or the characteristic has not been found on the beacon,
         * <code>-1</code> is returned.
         *
         * @return the advertising interval or <code>-1</code>
         */
        public int getActiveSlot() {
            final BluetoothGattCharacteristic characteristic = mActiveSlotCharacteristic;
            if(characteristic != null){
                final byte [] data = characteristic.getValue();
                if(data == null || data.length == 0){
                    return -1;
                }
                return characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            }
            return -1;
        }

        /**
         * Obtains the cached value of the advertising interval characteristic. If the value has not been obtained yet using {@link #read()}, or the characteristic has not been found on the beacon,
         * <code>null</code> is returned.
         *
         * @return the advertising interval or <code>null</code>
         */
        public Integer getAdvInterval() {
            final BluetoothGattCharacteristic characteristic = mAdvertisingIntervalCharacteristic;
            if (characteristic != null) {
                final byte[] data = characteristic.getValue();
                if (data == null || data.length == 0)
                    return null;
                return ParserUtils.getIntValue(characteristic.getValue(), 0, ParserUtils.FORMAT_UINT16_BIG_INDIAN);
            }
            return null;
        }

        /**
         * Obtains the cached value of the Radio Tx Power characteristic. If the value has not been obtained yet using {@link #read()}, or the characteristic has not been found on the beacon,
         * <code>null</code> is returned.
         *
         * @return the Radio Tx Power or <code>null</code>
         */
        public Integer getRadioTxPower() {
            final BluetoothGattCharacteristic characteristic = mRadioTxPowerCharacteristic;
            if (characteristic != null) {
                final byte[] data = characteristic.getValue();
                if (data == null || data.length == 0)
                    return null;
                return characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            }
            return null;
        }

        /**
         * Obtains the cached value of the Advanced Advertised Tx Power characteristic. If the value has not been obtained yet using {@link #read()}, or the characteristic has not been found on the beacon,
         * <code>null</code> is returned.
         *
         * @return the Advanced Advertised Tx Power or <code>null</code>
         */
        public Integer getAdvancedAdvertisedTxPower() {
            if(mAdvancedAdvertisedTxPowerCharacteristic == null)
                return null;

            final BluetoothGattCharacteristic characteristic = mAdvancedAdvertisedTxPowerCharacteristic;
            if (characteristic != null) {
                final byte[] data = characteristic.getValue();
                if (data == null || data.length == 0)
                    return null;
                return characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            }
            return null;
        }

        /**
         * Obtains the cached value of the Lock State characteristic. If the value has not been obtained yet using {@link #read()}, or the characteristic has not been found on the beacon,
         * <code>null</code> is returned.
         *
         * @return the Lock State or <code>null</code>
         */
        public Integer getLockState() {
            final BluetoothGattCharacteristic characteristic = mLockStateCharacteristic;
            if (characteristic != null) {
                final byte[] data = characteristic.getValue();
                if (data == null || data.length == 0)
                    return null;
                return ParserUtils.getIntValue(characteristic.getValue(), 0, BluetoothGattCharacteristic.FORMAT_UINT8);
            }
            return null;
        }

        /**
         * Obtains the cached value of the Public ECDH Key characteristic. If the value has not been obtained yet using {@link #read()}, or the characteristic has not been found on the beacon,
         * <code>null</code> is returned.
         *
         * @return the Public ECDH Key or <code>null</code>
         */
        public byte[] getBeaconPublicEcdhKey() {
            final BluetoothGattCharacteristic characteristic = mPublicEcdhKeyCharacteristic;
            if (characteristic != null) {
                final byte[] data = characteristic.getValue();
                if (data == null || data.length == 0)
                    return null;
                return characteristic.getValue();
            }
            return null;
        }

        /**
         * Obtains the cached value of the Encrypted Identity Key characteristic. If the value has not been obtained yet using {@link #read()}, or the characteristic has not been found on the beacon,
         * <code>null</code> is returned.
         *
         * @return the Encrypted Identity Key or <code>null</code>
         */
        public byte[] getIdentityKey() {
            final BluetoothGattCharacteristic characteristic = mEidIdentityKeyCharacteristic;
            if (characteristic != null) {
                final byte[] data = characteristic.getValue();
                if (data == null || data.length == 0)
                    return null;
                return characteristic.getValue();
            }
            return null;
        }

        /**
         * Obtains the cached value of the Read Write Adv Slot characteristic. If the value has not been obtained yet using {@link #read()}, or the characteristic has not been found on the beacon,
         * <code>null</code> is returned.
         *
         * @return the Read Write Adv Slot or <code>null</code>
         */
        public byte[] getReadWriteAdvSlotData() {
            final BluetoothGattCharacteristic characteristic = mReadWriteAdvSlotCharacteristic;
            if (characteristic != null) {
                final byte[] data = characteristic.getValue();
                if (data == null || data.length == 0)
                    return null;
                return characteristic.getValue();
            }
            return null;
        }

        /**
         * Get beacon lock code stored in the service
         *
         * @return the beacon lock code or <code>null</code>
         */
        public byte[] getBeaconLockCode() {
            if(mBeaconLockCode != null)
                return mBeaconLockCode;
            return null;
        }

        /**
         * Get beacon slot information stored in the service
         *
         * @return the beacon slot iformation or <code>null</code>
         */
        public ArrayList<String> getAllSlotInformation() {
            return mActiveSlotsTypes;
        }

        public void setBeaconLockCode(byte[] beaconLockCode) {
            this.mBeaconLockCode = beaconLockCode;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initialize();
        mHandler = new Handler();
        mConnectionState = STATE_DISCONNECTED;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBluetoothGatt != null)
            mBluetoothGatt.disconnect();
        mHandler = null;
        mBluetoothDevice = null;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        // We want to allow rebinding
        return true;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        mBluetoothDevice = intent.getParcelableExtra(EXTRA_DATA);
        return START_NOT_STICKY;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     */
    private void initialize() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mAdapter = bluetoothManager.getAdapter();
    }

    private void setState(final int state) {
        mConnectionState = state;
        final Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.putExtra(EXTRA_DATA, state);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUuid(final UUID uuid) {
        final Intent intent = new Intent(ACTION_UUID_READY);
        intent.putExtra(EXTRA_DATA, new ParcelUuid(uuid));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastBeaconCapabilities(final byte [] broadcastCapabilities) {
        final Intent intent = new Intent(ACTION_BROADCAST_CAPABILITIES);
        intent.putExtra(EXTRA_DATA, broadcastCapabilities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastActiveSlot(final boolean activeSlot) {
        final Intent intent = new Intent(ACTION_ACTIVE_SLOT);
        if(activeSlot)
            intent.putExtra(EXTRA_DATA, 0);
        else
            intent.putExtra(EXTRA_DATA, -1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastActiveSlot(final int activeSlot) {
        final Intent intent = new Intent(ACTION_ACTIVE_SLOT);
        intent.putExtra(EXTRA_DATA, activeSlot);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastAdvertisingInterval(final int advertisingInterval) {
        final Intent intent = new Intent(ACTION_ADVERTISING_INTERVAL);
        intent.putExtra(EXTRA_DATA, advertisingInterval);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastRadioTxPower(final byte [] radioTxPower) {
        final Intent intent = new Intent(ACTION_RADIO_TX_POWER);
        intent.putExtra(EXTRA_DATA, radioTxPower);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastAdvancedAdvertisedTxPower(final int advertisedTxPower) {
        final Intent intent = new Intent(ACTION_ADVANCED_ADVERTISED_TX_POWER);
        intent.putExtra(EXTRA_DATA, advertisedTxPower);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastLockState(final int lockState) {
        switch(lockState){
            case LOCKED:
                mIsBeaconLocked = true;
                add(RequestType.READ_CHARACTERISTIC, mUnlockCharacteristic);
                return;
            case UNLOCKED:
                broadcastUnlockRequest();
                mIsBeaconLocked = false;
                mReadlAllSlots = true;
                add(RequestType.READ_CHARACTERISTIC, mBroadcastCapabilitesCharacterisitc);
                break;
            case UNLOCKED_AUTOMATIC_RELOCK_DISABLED:
                broadcastUnlockRequest();
                mReadlAllSlots = true;
                add(RequestType.READ_CHARACTERISTIC, mBroadcastCapabilitesCharacterisitc);
                mIsBeaconLocked = false;
                break;
        }


        final Intent intent = new Intent(ACTION_LOCK_STATE);
        intent.putExtra(EXTRA_DATA, lockState);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUnlock(final byte [] unlock) {
        final Intent intent = new Intent(ACTION_UNLOCK);
        intent.putExtra(EXTRA_DATA, unlock);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastEcdhKey(final byte [] ecdhKey) {
        final Intent intent = new Intent(ACTION_ECDH_KEY);
        intent.putExtra(EXTRA_DATA, ecdhKey);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastEidIdentityKey(final byte [] identityKey) {
        final Intent intent = new Intent(ACTION_EID_IDENTITY_KEY);
        intent.putExtra(EXTRA_DATA, identityKey);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastReadWriteAdvSlot(final byte [] readWriteAdvSlot) {
        final Intent intent = new Intent(ACTION_READ_WRITE_ADV_SLOT);
        if(readWriteAdvSlot == null || readWriteAdvSlot.length == 0){
            if(mReadlAllSlots) {
                mActiveSlotsTypes.add("EMPTY");
                mSlotCounter = mSlotCounter + 1 ;
                if(mSlotCounter <= mMaxSlots - 1) {
                    if (mActiveSlotCharacteristic.setValue(new byte[]{(byte) mSlotCounter}))
                        add(RequestType.WRITE_CHARACTERISTIC, mActiveSlotCharacteristic);
                } else stopReadingAllActiveSlots();
            } else {
                intent.putExtra(EXTRA_FRAME_TYPE, EMPTY_SLOT);
                intent.putExtra(EXTRA_DATA, readWriteAdvSlot);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
            return;
        }
        final int frameType = ParserUtils.getIntValue(readWriteAdvSlot, 0, BluetoothGattCharacteristic.FORMAT_UINT8);

        switch (frameType){
            case TYPE_UID:
                if(mReadlAllSlots) {
                    mActiveSlotsTypes.add("UID");
                    mSlotCounter = mSlotCounter + 1 ;
                    if(mSlotCounter <= mMaxSlots - 1 ) {
                        if (mActiveSlotCharacteristic.setValue(new byte[]{(byte) mSlotCounter}))
                            add(RequestType.WRITE_CHARACTERISTIC, mActiveSlotCharacteristic);
                    } else stopReadingAllActiveSlots();
                    return;
                }
                intent.putExtra(EXTRA_FRAME_TYPE, frameType);
                intent.putExtra(EXTRA_NAMESPACE_ID, ParserUtils.bytesToHex(readWriteAdvSlot, 2, 10, true));
                intent.putExtra(EXTRA_INSTANCE_ID, ParserUtils.bytesToHex(readWriteAdvSlot, 12, 6, true));
                intent.putExtra(EXTRA_DATA, readWriteAdvSlot);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case TYPE_URL:
                if(mReadlAllSlots) {
                    mActiveSlotsTypes.add("URL");
                    mSlotCounter = mSlotCounter + 1 ;
                    if(mSlotCounter <= mMaxSlots - 1 ) {
                        if (mActiveSlotCharacteristic.setValue(new byte[]{(byte) mSlotCounter}))
                            add(RequestType.WRITE_CHARACTERISTIC, mActiveSlotCharacteristic);
                    } else stopReadingAllActiveSlots();
                    return;
                }

                intent.putExtra(EXTRA_FRAME_TYPE, frameType);
                intent.putExtra(EXTRA_URL, ParserUtils.decodeUri(readWriteAdvSlot, 2, readWriteAdvSlot.length-2));
                intent.putExtra(EXTRA_DATA, readWriteAdvSlot);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case TYPE_TLM:
                if(mReadlAllSlots) {
                    mActiveSlotsTypes.add("TLM");
                    mSlotCounter = mSlotCounter + 1 ;
                    if(mSlotCounter <= mMaxSlots - 1 ) {
                        if (mActiveSlotCharacteristic.setValue(new byte[]{(byte) mSlotCounter}))
                            add(RequestType.WRITE_CHARACTERISTIC, mActiveSlotCharacteristic);
                    } else stopReadingAllActiveSlots();
                    return;
                }

                intent.putExtra(EXTRA_FRAME_TYPE, frameType);
                if(mActiveSlotsTypes.contains("EID")){
                    intent.putExtra(EXTRA_ETLM, ParserUtils.bytesToHex(readWriteAdvSlot, 2, 12, true));
                    intent.putExtra(EXTRA_SALT,  ParserUtils.bytesToHex(readWriteAdvSlot, 14, 2, true));
                    intent.putExtra(EXTRA_MESSAGE_INTEGRITY_CHECK, ParserUtils.bytesToHex(readWriteAdvSlot, 16, 2, true));
                    intent.putExtra(EXTRA_DATA, readWriteAdvSlot);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    return;
                }

                final int voltage = ParserUtils.decodeUint16BigEndian(readWriteAdvSlot, 2);
                if(voltage > 0){
                    intent.putExtra(EXTRA_VOLTAGE, String.valueOf(voltage) + getString(R.string.voltage_unit));
                } else {
                    intent.putExtra(EXTRA_VOLTAGE, getString(R.string.batt_voltage_unsupported));
                }

                final float temp = ParserUtils.decode88FixedPointNotation(readWriteAdvSlot, 4);
                if (temp > -128.0f)
                    intent.putExtra(EXTRA_BEACON_TEMPERATURE, String.valueOf(temp) + getString(R.string.temperature_unit));
                else
                    intent.putExtra(EXTRA_BEACON_TEMPERATURE, getString(R.string.temperature_unsupported));

                intent.putExtra(EXTRA_PDU_COUNT, String.valueOf(ParserUtils.decodeUint32BigEndian(readWriteAdvSlot, 6)));
                intent.putExtra(EXTRA_TIME_SINCE_BOOT,  String.valueOf(ParserUtils.decodeUint32BigEndian(readWriteAdvSlot, 10) * 100));
                intent.putExtra(EXTRA_DATA, readWriteAdvSlot);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case TYPE_EID:
                if(mReadlAllSlots) {
                    mActiveSlotsTypes.add("EID");
                    mSlotCounter = mSlotCounter + 1 ;
                    if(mSlotCounter <= mMaxSlots - 1)
                        if (mActiveSlotCharacteristic.setValue(new byte[]{(byte) mSlotCounter}))
                            add(RequestType.WRITE_CHARACTERISTIC, mActiveSlotCharacteristic);
                        else stopReadingAllActiveSlots();
                    return;
                }
                add(RequestType.READ_CHARACTERISTIC, mPublicEcdhKeyCharacteristic);
                add(RequestType.READ_CHARACTERISTIC, mEidIdentityKeyCharacteristic);
                intent.putExtra(EXTRA_FRAME_TYPE, frameType);
                intent.putExtra(EXTRA_TIMER_EXPONENT, String.valueOf(ParserUtils.getIntValue(readWriteAdvSlot, 1, BluetoothGattCharacteristic.FORMAT_UINT8)));
                intent.putExtra(EXTRA_CLOCK_VALUE, String.valueOf(ParserUtils.getIntValue(readWriteAdvSlot, 2, ParserUtils.FORMAT_UINT32_BIG_INDIAN)));
                intent.putExtra(EXTRA_EID, String.valueOf(ParserUtils.bytesToHex(readWriteAdvSlot, 6, 8, true)));
                intent.putExtra(EXTRA_DATA, readWriteAdvSlot);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
        }
    }

    private void broadcastAdvancedFactoryReset(final int factoryReset) {
        final Intent intent = new Intent(ACTION_ADVANCED_FACTORY_RESET);
        intent.putExtra(EXTRA_DATA, factoryReset);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastAdvancedRemainConnectable(final int remainConnectable) {
        final Intent intent = new Intent(ACTION_ADVANCED_REMAIN_CONNECTABLE);
        if(remainConnectable > 0)
            intent.putExtra(EXTRA_DATA, true);
        else
            intent.putExtra(EXTRA_DATA, false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastError(final int error) {
        final Intent intent = new Intent(ACTION_GATT_ERROR);
        intent.putExtra(EXTRA_DATA, error);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUnlockRequest(final byte [] challenge) {
        final Intent intent = new Intent(ACTION_UNLOCK_BEACON);
        intent.putExtra(EXTRA_DATA, challenge);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Dismiss the unlock key dialog in case the beacon is not locked
     */
    private void broadcastUnlockRequest() {
        final Intent intent = new Intent(ACTION_DISSMISS_UNLOCK);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastAllSlotInformation(){
        final Intent intent = new Intent(ACTION_BROADCAST_ALL_SLOT_INFO);
        intent.putStringArrayListExtra(EXTRA_DATA, mActiveSlotsTypes);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startReadingAllActiveSlots(byte [] mBroadcastCapabilities) {
        if (mMaxSlots < 0)
            mMaxSlots = ParserUtils.getIntValue(mBroadcastCapabilities, 1, BluetoothGattCharacteristic.FORMAT_UINT8);
        if (mMaxEidSlots < 0)
            mMaxEidSlots = ParserUtils.getIntValue(mBroadcastCapabilities, 2, BluetoothGattCharacteristic.FORMAT_UINT8);
        if (mSlotCounter < mMaxSlots)
            add(RequestType.READ_CHARACTERISTIC, mActiveSlotCharacteristic);
    }

    private void stopReadingAllActiveSlots() {
        mReadlAllSlots = false;
        mSlotCounter = 0;
        mStartReadingInitialCharacteristics = true;
        if(mActiveSlotCharacteristic.setValue(new byte [] {(byte) mSlotCounter})) {
            add(RequestType.WRITE_CHARACTERISTIC, mActiveSlotCharacteristic);
            startReadingCharacteristicsForActiveSlot();
        }
    }

    private void startReadingCharacteristicsForActiveSlot() {
        add(RequestType.READ_CHARACTERISTIC, mAdvertisingIntervalCharacteristic);
        add(RequestType.READ_CHARACTERISTIC, mRadioTxPowerCharacteristic);
        if (mAdvancedAdvertisedTxPowerCharacteristic != null && !mReadlAllSlots)
            add(RequestType.READ_CHARACTERISTIC, mAdvancedAdvertisedTxPowerCharacteristic);
        add(RequestType.READ_CHARACTERISTIC, mReadWriteAdvSlotCharacteristic);
        if (mAdvancedRemainConnectableCharacteristic != null & !mReadlAllSlots)
            add(RequestType.READ_CHARACTERISTIC, mAdvancedRemainConnectableCharacteristic);
        broadcastAllSlotInformation();
    }

    /**
     * Clears the device cache.
     * <p>
     * CAUTION:<br />
     * It is very unsafe to call the refresh() method. First of all it's hidden so it may be removed in the future release of Android. We do it because Nordic Beacon may advertise as a beacon, as
     * Beacon Config or DFU. Android does not clear cache then device is disconnected unless manually restarted Bluetooth Adapter. To do this in the code we need to call
     * {@link BluetoothGatt#refresh()} method. However is may cause a lot of troubles. Ideally it should be called before connection attempt but we get 'gatt' object by calling connectGatt method so
     * when the connection already has been started. Calling refresh() afterwards causes errors 129 and 133 to pop up from time to time when refresh takes place actually during service discovery. It
     * seems to be asynchronous method. Therefore we are refreshing the device after disconnecting from it, before closing gatt. Sometimes you may obtain services from cache, not the actual values so
     * reconnection is required.
     *
     * @param gatt
     *            the Bluetooth GATT object to refresh.
     */
    private boolean refreshDeviceCache(final BluetoothGatt gatt) {
		/*
		 * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
		 */
        try {
            final Method refresh = gatt.getClass().getMethod("refresh");
            if (refresh != null) {
                return (Boolean) refresh.invoke(gatt);
            }
        } catch (final Exception e) {
            loge("An exception occurred while refreshing device");
        }
        return false;
    }

    private void loge(final String message) {
        if (BuildConfig.DEBUG)
            Log.e(TAG, message);
    }

    private void logw(final String message) {
        if (BuildConfig.DEBUG)
            Log.w(TAG, message);
    }

    /**
     * Convert a signed byte to an unsigned long.
     */
    public static long unsignedByteToLong(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    public static int unsignedByteToInt(int b) {
        return b & 0xFF;
    }

    /**
     * BluetoothGatt request types.
     */
    public enum RequestType {
        //  CHARACTERISTIC_NOTIFICATION,
        READ_CHARACTERISTIC,
        READ_DESCRIPTOR,
        //  READ_RSSI,
        WRITE_CHARACTERISTIC,
        WRITE_DESCRIPTOR
    }

    public void add(RequestType type, BluetoothGattDescriptor descriptor) {
        Request request = new Request(type, descriptor);
        add(request);
    }

    public void add(RequestType type, BluetoothGattCharacteristic characteristic) {
        Request request = new Request(type, characteristic);
        add(request);
    }

    synchronized private void add(Request request) {
        mQueue.add(request);
        if (mQueue.size() == 1) {
            mQueue.peek().start(mBluetoothGatt);
        }
    }

    /**
     * Process the next request in the queue for a BluetoothGatt function (such as characteristic read).
     */
    synchronized private void processNext() {
        // The currently executing request is kept on the head of the queue until this is called.
        if (mQueue.isEmpty())
            throw new RuntimeException("No active request in processNext()");
        mQueue.remove();
        if (!mQueue.isEmpty()) {
            mQueue.peek().start(mBluetoothGatt);
        }
    }

    /**
     * The object that holds a Gatt request while in the queue.
     * <br>
     * This object holds the parameters for calling BluetoothGatt methods (see start());
     */
    public class Request {
        final RequestType requestType;
        BluetoothGattCharacteristic characteristic;
        BluetoothGattDescriptor descriptor;

        public Request(RequestType requestType, BluetoothGattCharacteristic characteristic) {
            this.requestType = requestType;
            this.characteristic = characteristic;
        }

        public Request(RequestType requestType, BluetoothGattDescriptor descriptor) {
            this.requestType = requestType;
            this.descriptor = descriptor;
        }

        public void start(BluetoothGatt bluetoothGatt) {
            switch (requestType) {
                case READ_CHARACTERISTIC:
                    if (!bluetoothGatt.readCharacteristic(characteristic)) {
                        throw new IllegalArgumentException("Characteristic is not valid: " + characteristic.getUuid().toString());
                    }
                    break;
                case READ_DESCRIPTOR:
                    if (!bluetoothGatt.readDescriptor(descriptor)) {
                        throw new IllegalArgumentException("Descriptor is not valid");
                    }
                    break;
                case WRITE_CHARACTERISTIC:
                    if (!bluetoothGatt.writeCharacteristic(characteristic)) {
                        throw new IllegalArgumentException("Characteristic is not valid");
                    }
                    break;
                case WRITE_DESCRIPTOR:
                    if (!bluetoothGatt.writeDescriptor(descriptor)) {
                        throw new IllegalArgumentException("Characteristic is not valid");
                    }
                    break;
            }
        }
    }
}
