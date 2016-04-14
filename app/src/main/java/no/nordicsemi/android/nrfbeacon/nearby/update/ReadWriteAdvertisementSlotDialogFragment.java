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
package no.nordicsemi.android.nrfbeacon.nearby.update;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import no.nordicsemi.android.nrfbeacon.nearby.AuthorizedServiceTask;
import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.util.AuthTaskUrlShortener;
import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

/**
 * Created by rora on 07.03.2016.
 */
public class ReadWriteAdvertisementSlotDialogFragment extends DialogFragment {


    private static final String PATTERN_NAMESPACE_ID = "[0-9a-fA-F]{20}";
    private static final String PATTERN_INSTANCE_ID = "[0-9a-fA-F]{12}";
    private static final String RW_ADVERTISEMENT_SLOT = "RW_ADVERTISEMENT_SLOT";
    private static final String SLOT_TASK = "SLOT_TASK";
    private static final String ACTIVE_SLOT = "ACTIVE_SLOT";
    private static final String TAG = "BEACON";
    private static final String ACCOUNT_NAME_PREF = "userAccount";
    private static final String SHARED_PREFS_NAME = "nrfNearbyInfo";
    private static final String AUTH_PROXIMITY_API = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";
    private static final String AUTH_SCOPE_URL_SHORTENER = "oauth2:https://www.googleapis.com/auth/urlshortener";

    private static final int EMPTY_SLOT = -1;
    private static final int TYPE_UID = 0x00;
    private static final int TYPE_URL = 0x10;
    private static final int TYPE_TLM = 0x20;
    private static final int TYPE_EID = 0x30;


    private byte [] mReadWriteAdvSlotData;
    private int mActiveSlot;
    private boolean mNewAddSlot;

    private LinearLayout mSlotStateContainer;
    private LinearLayout mUidInfoContainer;
    private LinearLayout mUrlInfoContainer;
    private LinearLayout mUrlShortenerContainer;
    private LinearLayout mEidInfoContainer;
    private Spinner mFrameTypes;
    private Spinner mUrlTypes;
    private Spinner mSecurityTypes;
    private TextView mActiveSlotNumber;
    private TextView mSlotState;
    private TextView mUrlShortText;
    private EditText mNamespaceId;
    private EditText mInstanceId;
    private EditText mUrl;
    private EditText mTimerExponent;
    private int mFrameType;
    private int mNewFrameType;
    private Button mButtonNeutral;
    private ProgressDialog mProgressDialog;
    private Handler mProgressDialogHandler;
    private boolean mUrlShortenerClicked = false;

    public interface OnReadWriteAdvertisementSlotListener {
        void configureEidSlot(byte[] eidSlotData);

        void configureUidSlot(byte[] uidSlotData);

        void configureUrlSlot(byte[] urlSlotData);

        void configureTlmSlot(byte[] tlmSlotData);
    }

    public static ReadWriteAdvertisementSlotDialogFragment newInstance(final boolean addNewSlot, final int activeSlot, byte [] readWriteAdvSlotData){
        ReadWriteAdvertisementSlotDialogFragment fragment = new ReadWriteAdvertisementSlotDialogFragment();
        final Bundle args = new Bundle();
        args.putBoolean(SLOT_TASK, addNewSlot);
        args.putInt(ACTIVE_SLOT, activeSlot);
        args.putByteArray(RW_ADVERTISEMENT_SLOT, readWriteAdvSlotData);
        fragment.setArguments(args);
        return fragment;
    }

    public ReadWriteAdvertisementSlotDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mActiveSlot = getArguments().getInt(ACTIVE_SLOT);
            mNewAddSlot = getArguments().getBoolean(SLOT_TASK);
            mReadWriteAdvSlotData = getArguments().getByteArray(RW_ADVERTISEMENT_SLOT);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.title_rw_adv_slot));

        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_rw_adv_slot, null);
        mSlotStateContainer = (LinearLayout) view.findViewById(R.id.slot_state_container);
        mUidInfoContainer = (LinearLayout) view.findViewById(R.id.uid_container);
        mUrlInfoContainer = (LinearLayout) view.findViewById(R.id.url_container);
        mUrlShortenerContainer = (LinearLayout) view.findViewById(R.id.url_shortener_container);
        mEidInfoContainer = (LinearLayout) view.findViewById(R.id.eid_data_container);
        mUrlTypes = (Spinner) view.findViewById(R.id.url_types);
        mFrameTypes = (Spinner) view.findViewById(R.id.frame_types);
        mSecurityTypes = (Spinner) view.findViewById(R.id.security_type);
        mNamespaceId = (EditText) view.findViewById(R.id.namespace_id);
        mInstanceId = (EditText) view.findViewById(R.id.instance_id);
        mUrl = (EditText) view.findViewById(R.id.url_data);
        mTimerExponent = (EditText) view.findViewById(R.id.timer_exponent);
        mActiveSlotNumber = (TextView) view.findViewById(R.id.slot_number);
        mSlotState = (TextView) view.findViewById(R.id.slot_state);
        mUrlShortText = (TextView) view.findViewById(R.id.url_short_text);

        final AlertDialog alertDialog = alertDialogBuilder.setView(view)
                .setPositiveButton(getString(R.string.configure), null)
                .setNegativeButton(getString(R.string.cancel), null)
                .setNeutralButton(getString(R.string.random_uid), null).show();
        alertDialog.setCanceledOnTouchOutside(false);


        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setTitle(getString(R.string.prog_dialog_url_shortener));
        mProgressDialog.setMessage(getString(R.string.prog_dialog_url_shortener_message));

        mFrameTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mButtonNeutral.setVisibility(View.VISIBLE);
                        mButtonNeutral.setText(getString(R.string.random_uid));
                        mUidInfoContainer.setVisibility(View.VISIBLE);
                        mUrlInfoContainer.setVisibility(View.GONE);
                        mUrlShortenerContainer.setVisibility(View.GONE);
                        mEidInfoContainer.setVisibility(View.GONE);
                        if (TYPE_UID == mFrameType) {
                            final String namespaceId = ParserUtils.bytesToHex(mReadWriteAdvSlotData, 2, 10, false);
                            final String instanceId = ParserUtils.bytesToHex(mReadWriteAdvSlotData, 11, 6, false);
                            mNamespaceId.setText(namespaceId);
                            mInstanceId.setText(instanceId);
                        }
                        mNewFrameType = TYPE_UID;
                        break;
                    case 1:
                        mButtonNeutral.setVisibility(View.VISIBLE);
                        mButtonNeutral.setText(getString(R.string.url_shorten_button));
                        mUrlInfoContainer.setVisibility(View.VISIBLE);
                        mUrlShortenerContainer.setVisibility(View.GONE);
                        mUidInfoContainer.setVisibility(View.GONE);
                        mEidInfoContainer.setVisibility(View.GONE);
                        if (TYPE_URL == mFrameType) {
                            String url = ParserUtils.decodeUri(mReadWriteAdvSlotData, 2, mReadWriteAdvSlotData.length - 2);
                            if(url.startsWith("https://www.")){
                                url = url.replace("https://www.", "");
                                mUrlTypes.setSelection(0);
                            } else if(url.startsWith("http://www.")){
                                url = url.replace("http://www.", "");
                                mUrlTypes.setSelection(1);
                            } else if(url.startsWith("https://")){
                                url = url.replace("https://", "");
                                mUrlTypes.setSelection(2);
                            } else if(url.startsWith("https://")){
                                url = url.replace("http://", "");
                                mUrlTypes.setSelection(3);
                            }
                            mUrl.setText(url.trim());


                        }
                        mNewFrameType = TYPE_URL;
                        break;
                    case 2:
                        mButtonNeutral.setVisibility(View.GONE);
                        mUidInfoContainer.setVisibility(View.GONE);
                        mUrlInfoContainer.setVisibility(View.GONE);
                        mUrlShortenerContainer.setVisibility(View.GONE);
                        mEidInfoContainer.setVisibility(View.GONE);
                        mNewFrameType = TYPE_TLM;
                        break;
                    case 3:
                        mButtonNeutral.setVisibility(View.GONE);
                        mEidInfoContainer.setVisibility(View.VISIBLE);
                        mUidInfoContainer.setVisibility(View.GONE);
                        mUrlInfoContainer.setVisibility(View.GONE);
                        mUrlShortenerContainer.setVisibility(View.GONE);
                        if (TYPE_EID == mFrameType) {
                            final String timerExponent = String.valueOf(ParserUtils.getIntValue(mReadWriteAdvSlotData, 1, BluetoothGattCharacteristic.FORMAT_UINT8));
                            mTimerExponent.setText(timerExponent);
                        }
                        mNewFrameType = TYPE_EID;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if(mReadWriteAdvSlotData.length > 0)
            mSecurityTypes.setSelection(0);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    byte[] data = getValueFromView();
                    switch (mFrameTypes.getSelectedItemPosition()) {
                        case 0:
                            ((OnReadWriteAdvertisementSlotListener) getParentFragment()).configureUidSlot(data);
                            break;
                        case 1:
                            ((OnReadWriteAdvertisementSlotListener) getParentFragment()).configureUrlSlot(data);
                            break;
                        case 2:
                            ((OnReadWriteAdvertisementSlotListener) getParentFragment()).configureTlmSlot(data);
                            break;
                        case 3:
                            ((OnReadWriteAdvertisementSlotListener) getParentFragment()).configureEidSlot(data);
                            break;
                    }
                    dismiss();
                }
            }
        });

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mButtonNeutral = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        mButtonNeutral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mFrameTypes.getSelectedItemPosition()) {
                    case 0:
                        final String randomUid = ParserUtils.randomUid(16);
                        final String namespace = (randomUid.substring(0, 20));
                        final String instance = (randomUid.substring(20, randomUid.length()));
                        mNamespaceId.setText(namespace.toUpperCase());
                        mInstanceId.setText(instance.toUpperCase());
                        break;
                    case 1:
                        final String url = mUrl.getText().toString().trim();

                        if (!url.isEmpty()) {
                            mUrlShortenerClicked = true;
                            /*mProgressDialog.show();
                            mProgressDialogHandler = new Handler();
                            mProgressDialogHandler.postDelayed(mRunnableHandler, 15000);*/
                            if(getUserAccount() != null)
                                new AuthTaskUrlShortener(mUrlShortenerCallback, url, getActivity(), getUserAccount()).execute();
                            else
                                Toast.makeText(getActivity(), getString(R.string.user_account_unavailable), Toast.LENGTH_LONG).show();

                        }
                        break;
                }
            }
        });

        mUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mUrl.setError(null);
                if(mUrlShortenerContainer.getVisibility() == View.VISIBLE)
                    mUrlShortenerContainer.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        updateUi();

        final String name = getUserAccountName();
        if(!name.isEmpty()) {
            Account userAccount = null;
            final Account[] accounts = AccountManager.get(getActivity()).getAccounts();
            for (Account account : accounts) {
                if (account.name.equals(name)) {
                    userAccount = account;
                    break;
                }
            }
            new AuthorizedServiceTask(getActivity(), userAccount, AUTH_SCOPE_URL_SHORTENER).execute();
        }
        return alertDialog;
    }

    private String getUserAccountName(){
        SharedPreferences mSharedPreferences = getContext().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(ACCOUNT_NAME_PREF, null);
    }

    private Account getUserAccount(){
        final String name = getUserAccountName();
        if(!name.isEmpty()) {
            final Account[] accounts = AccountManager.get(getActivity()).getAccounts();
            for (Account account : accounts) {
                if (account.name.equals(name)) {
                    return account;
                }
            }
        }
        return null;
    }

    private final Runnable mRunnableHandler = new Runnable() {
        @Override
        public void run() {
            if(mProgressDialog != null && mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
        }
    };

    private boolean validateInput() {

        if(mUidInfoContainer.getVisibility() == View.VISIBLE) {
            final String namespaceId = mNamespaceId.getText().toString().trim();
            if (namespaceId.isEmpty()) {
                mNamespaceId.setError("Please enter namespace id");
                return false;
            } else {
                if (!namespaceId.matches(PATTERN_NAMESPACE_ID)) {
                    mNamespaceId.setError("Please enter a valid value for namespace id");
                    return false;
                }
            }
            final String instanceId = mInstanceId.getText().toString().trim();
            if (instanceId.isEmpty()) {
                mInstanceId.setError("Please enter instance id");
                return false;
            } else {
                if (!instanceId.matches(PATTERN_INSTANCE_ID)) {
                    mInstanceId.setError("Please enter a valid value for instance id");
                    return false;
                }
            }
        }

        if(mUrlInfoContainer.getVisibility() == View.VISIBLE) {

            if(mUrlShortenerContainer.getVisibility() == View.VISIBLE){
                return true;
            }

            final String url = mUrlTypes.getSelectedItem().toString().trim() + mUrl.getText().toString().trim();
            if (url.isEmpty()) {
                mUrl.setError("Please enter a value for URL");
                return false;
            } else {
                if (!URLUtil.isValidUrl(url)) {
                    mUrl.setError("Please enter a valid value for URL");
                    return false;
                } else if (!URLUtil.isNetworkUrl(url)) {
                    mUrl.setError("Please enter a valid value for URL");
                    return false;
                } else if (ParserUtils.encodeUri(url).length > 18){
                    mUrl.setError("Please enter a shortened URL or press use the URL shortener!");
                    return false;
                }
            }
        }

        if(mEidInfoContainer.getVisibility() == View.VISIBLE) {

            final String timerExponent = mTimerExponent.getText().toString().trim();
            if(timerExponent.isEmpty()){
                mTimerExponent.setError(getString(R.string.timer_exponent_error));
                return false;
            } else {
                boolean valid;
                int i = 0;
                try {
                    i = Integer.parseInt(timerExponent);
                    valid = ((i & 0xFFFFFF00) == 0 || (i & 0xFFFFFF00) == 0xFFFFFF00);
                } catch (NumberFormatException e) {
                    valid = false;
                }
                if (!valid) {
                    mTimerExponent.setError(getString(R.string.uint8_error));
                    return false;
                } else if ((i <  10 || i > 15)){
                    mTimerExponent.setError(getString(R.string.timer_exponent_error_value));
                    return false;
                }
            }
        }
        return true;
    }

    private byte[] getValueFromView() {
        byte [] data = null;
        int length;
        switch (mNewFrameType) {
            case TYPE_UID:
                data = new byte[17];
                data[0] = TYPE_UID;
                final String namespaceId = mNamespaceId.getText().toString().trim();
                ParserUtils.setByteArrayValue(data, 1, namespaceId);
                final String instanceId = mInstanceId.getText().toString().trim();
                ParserUtils.setByteArrayValue(data, 11, instanceId);
                return data;
            case TYPE_URL:
                final String urlText;
                if(mUrlShortenerContainer.getVisibility() != View.VISIBLE){
                    urlText = mUrlTypes.getSelectedItem().toString().trim() + mUrl.getText().toString().trim();
                } else {
                    urlText = mUrlShortText.getText().toString().trim();
                }

                byte [] urlData = ParserUtils.encodeUri(urlText);
                data = new byte[urlData.length + 1];
                data[0] = (byte) TYPE_URL;
                System.arraycopy(urlData, 0, data , 1, urlData.length);
                return data;
            case TYPE_TLM:
                data = new byte[1];
                data[0] = TYPE_TLM;
                return data;
            case TYPE_EID:
                final String timerExponent = mTimerExponent.getText().toString().trim();

                if (mSecurityTypes.getSelectedItemPosition() == 0) {
                    data = new byte[34];
                    data[0] = TYPE_EID;
                    data[33] = (byte) Integer.parseInt(timerExponent);
                    return data;
                } else {
                    data = new byte[18];
                    data[0] = TYPE_EID;
                    data[17] = (byte) Integer.parseInt(timerExponent);
                    return data;
                }
        }
        return data;
    }

    private void updateUi() {
        final int frameType;
        if(mReadWriteAdvSlotData == null || mReadWriteAdvSlotData.length == 0)
            frameType = EMPTY_SLOT;
        else
            frameType = ParserUtils.getIntValue(mReadWriteAdvSlotData, 0, BluetoothGattCharacteristic.FORMAT_UINT8);
        switch (frameType){
            case TYPE_UID:
                mButtonNeutral.setVisibility(View.VISIBLE);
                mFrameType = TYPE_UID;
                mFrameTypes.setSelection(0);
                break;
            case TYPE_URL:
                mButtonNeutral.setVisibility(View.VISIBLE);
                mButtonNeutral.setText(getString(R.string.url_shorten_button));
                mFrameType = TYPE_URL;
                mFrameTypes.setSelection(1);
                break;
            case TYPE_TLM:
                mButtonNeutral.setVisibility(View.GONE);
                mFrameType = TYPE_TLM;
                mFrameTypes.setSelection(2);
                break;
            case TYPE_EID:
                mButtonNeutral.setVisibility(View.GONE);
                mFrameType = TYPE_EID;
                mFrameTypes.setSelection(3);
                break;
            case EMPTY_SLOT:
                mSlotStateContainer.setVisibility(View.VISIBLE);
                mSlotState.setText(getString(R.string.slot_state_empty));
                mFrameType = EMPTY_SLOT;
                break;
        }
        mActiveSlotNumber.setText(String.valueOf(mActiveSlot));
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private final Callback mUrlShortenerCallback = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {
            Log.v(TAG, "Failure: " + request.toString());
            if(mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
            if(!response.isSuccessful()){
                //GoogleAuthUtil.clearToken(getActivity(), token);
                mUrlShortenerContainer.setVisibility(View.GONE);
                Toast.makeText(getActivity(), response.body().string(), Toast.LENGTH_SHORT).show();
                Account userAccount = getUserAccount();
                if(userAccount != null)
                    new AuthorizedServiceTask(getActivity(), userAccount, AUTH_SCOPE_URL_SHORTENER).execute();
            } else {
                try {
                    mUrlShortenerContainer.setVisibility(View.VISIBLE);
                    mUrl.setError(null);
                    final JSONObject json = new JSONObject(response.body().string());
                    mUrlShortText.setText(json.getString("id"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    };
}
