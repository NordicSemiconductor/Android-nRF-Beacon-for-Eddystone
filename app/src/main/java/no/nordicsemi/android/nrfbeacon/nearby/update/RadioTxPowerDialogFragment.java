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

import android.app.Dialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

/**
 * Created by rora on 06.04.2016.
 */
public class RadioTxPowerDialogFragment extends DialogFragment {

    private static final String PATTERN_TX_POWER = "^-?\\d{1,3}$";
    private static final int MIN_TX_POWER = -100;
    private static final int MAX_TX_POWER = 20;
    private static final int IS_VARIABLE_TX_POWER_SUPPORTED = 0x02;
    private static final String RADIO_TX_POWER = "RADIO_TX_POWER";
    private static final String ADVANCED_ADV_TX_POWER = "ADVANCED_ADV_TX_POWER";
    private static final String BROADCAST_CAPABILITIES = "BROADCAST_CAPABILITIES";
    private static final String TAG = "BEACON";

    private String mRadioTxPower;
    private boolean mAdvancedAdvTxPower;
    private byte [] mBroadcastCapabilities;
    private EditText radioTxPower;
    private LinearLayout mVariableRadioTxPowerContainer;
    private LinearLayout mRadioTxPowerContainer;
    private Spinner mVariableTxPowerTypes;
    private ArrayList<String> mVariableTxPowerList;
    private ArrayAdapter mVariableTxPowerAdapter;

    public interface OnRadioTxPowerListener {
        void configureRadioTxPower(final byte [] radioTxPower, final boolean advanceTxPowerSupported);
    }

    public static RadioTxPowerDialogFragment newInstance(final String radioTxPower, final byte []  broadcastCapabilities, final boolean advancedAdvTxPower){
        RadioTxPowerDialogFragment fragment = new RadioTxPowerDialogFragment();
        final Bundle args = new Bundle();
        args.putString(RADIO_TX_POWER, radioTxPower);
        args.putByteArray(BROADCAST_CAPABILITIES, broadcastCapabilities);
        args.putBoolean(ADVANCED_ADV_TX_POWER, advancedAdvTxPower);
        fragment.setArguments(args);
        return fragment;
    }

    public RadioTxPowerDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mRadioTxPower = getArguments().getString(RADIO_TX_POWER);
            mAdvancedAdvTxPower = getArguments().getBoolean(ADVANCED_ADV_TX_POWER);
            mBroadcastCapabilities = getArguments().getByteArray(BROADCAST_CAPABILITIES);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        if(!mAdvancedAdvTxPower) {
            alertDialogBuilder.setTitle(getString(R.string.title_config_radio_tx_power));
        } else {
            alertDialogBuilder.setTitle(getString(R.string.title_config_adv_radio_tx_power));
        }


        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_radio_tx_power, null);
        mRadioTxPowerContainer = (LinearLayout) view.findViewById(R.id.radio_tx_power_container);
        mVariableRadioTxPowerContainer = (LinearLayout) view.findViewById(R.id.var_radio_tx_power_container);
        mVariableTxPowerTypes = (Spinner) view.findViewById(R.id.var_radio_tx_power_types);
        final TextView currentTxPower = (TextView) view.findViewById(R.id.current_radio_tx_power);
        currentTxPower.setText(mRadioTxPower);
        final TextView currentTxPowerTitle = (TextView) view.findViewById(R.id.radio_tx_power_title);
        radioTxPower = (EditText) view.findViewById(R.id.radio_tx_power);
        final AlertDialog alertDialog = alertDialogBuilder.setView(view).setPositiveButton(getString(R.string.configure), null).setNegativeButton(getString(R.string.cancel), null).show();
        alertDialog.setCanceledOnTouchOutside(false);


        if(mAdvancedAdvTxPower) {
            radioTxPower.setHint(getString(R.string.advanced_adv_tx_power));
            currentTxPowerTitle.setText(getString(R.string.current_advanced_adv_tx_power));
            mVariableRadioTxPowerContainer.setVisibility(View.GONE);
        } else {
            if (mBroadcastCapabilities != null) {
                final int capabilities = ParserUtils.getIntValue(mBroadcastCapabilities, 3, BluetoothGattCharacteristic.FORMAT_UINT8);
                final boolean variableTxPower = (capabilities & IS_VARIABLE_TX_POWER_SUPPORTED) > 0;
                if(!variableTxPower) {
                    mVariableRadioTxPowerContainer.setVisibility(View.GONE);
                } else {
                    mRadioTxPowerContainer.setVisibility(View.GONE);
                    updateUi();
                }
            }
        }



        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateInput()){
                    final byte [] radioTxPower = getValueFromView();
                    ((OnRadioTxPowerListener)getParentFragment()).configureRadioTxPower(radioTxPower, mAdvancedAdvTxPower);
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
        return alertDialog;
    }

    private void updateUi() {
        mVariableTxPowerList = new ArrayList<>();
        for(int i = 6; i < mBroadcastCapabilities.length; i++){
           mVariableTxPowerList.add(String.valueOf(ParserUtils.getIntValue(mBroadcastCapabilities, i , BluetoothGattCharacteristic.FORMAT_SINT8)));
        }
        mVariableTxPowerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, mVariableTxPowerList);
        mVariableTxPowerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mVariableTxPowerTypes.setAdapter(mVariableTxPowerAdapter);

        if(mVariableTxPowerList.contains(mRadioTxPower)){
            final int index = mVariableTxPowerList.indexOf(mRadioTxPower);
            mVariableTxPowerTypes.setSelection(index);
        }
    }

    private boolean validateInput() {

        if(mVariableRadioTxPowerContainer.getVisibility() == View.VISIBLE){
            return true;
        }

        final String txPower = radioTxPower.getText().toString().trim();
        if(txPower.isEmpty()){
            radioTxPower.setError("Enter supported Radio Tx power value");
            return false;
        } else {
            if (!txPower.matches(PATTERN_TX_POWER)) {
                radioTxPower.setError("Please enter a valid value for Radio Tx power between -100 and +20");
                return false;
            }

            final int txPowerVal = Integer.parseInt(txPower);
            if(txPowerVal < MIN_TX_POWER || txPowerVal > MAX_TX_POWER ){
                radioTxPower.setError("Please enter a valid value for Radio Tx power between -100 and +20");
                return false;
            }
        }
        return true;
    }

    private byte[] getValueFromView() {
        if(mVariableRadioTxPowerContainer.getVisibility() == View.VISIBLE) {
            final byte[] data = new byte[1];
            data[0] = (byte) Integer.parseInt(mVariableTxPowerTypes.getSelectedItem().toString().trim());
            return data;
        } else {
            final byte[] data = new byte[1];
            data[0] = (byte) Integer.parseInt(radioTxPower.getText().toString().trim());
            return data;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
