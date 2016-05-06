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
import android.widget.TextView;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

/**
 * Created by rora on 07.03.2016.
 */
public class BroadcastCapabilitesDialogFragment extends DialogFragment {

    private static final String BROADCAST_CAPABILITIES = "BROADCAST_CAPABILITIES";
    private static final String TAG = "BEACON";
    private static final int IS_VARIABLE_ADV_SUPPORTED = 0x01;
    private static final int IS_VARIABLE_TX_POWER_SUPPORTED = 0x02;
    private static final int TYPE_UID = 0x0001;
    private static final int TYPE_URL = 0x0002;
    private static final int TYPE_TLM = 0x0004;
    private static final int TYPE_EID = 0x0008;
    private TextView mVersion;

    private byte [] mBroadcastCapabilities;

    public static BroadcastCapabilitesDialogFragment newInstance(byte [] broadcastCapabilities){
        BroadcastCapabilitesDialogFragment fragment = new BroadcastCapabilitesDialogFragment();
        final Bundle args = new Bundle();
        args.putByteArray(BROADCAST_CAPABILITIES, broadcastCapabilities);
        fragment.setArguments(args);
        return fragment;
    }

    public BroadcastCapabilitesDialogFragment(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mBroadcastCapabilities = getArguments().getByteArray(BROADCAST_CAPABILITIES);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Broadcast Capabilities");

        final View alertDialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_broadcast_capabilities, null);
        mVersion = (TextView) alertDialogView.findViewById(R.id.version_value);
        final TextView totalSlots = (TextView) alertDialogView.findViewById(R.id.total_slots);
        final TextView totalEidSlots = (TextView) alertDialogView.findViewById(R.id.total_eid_slots);
        final TextView variableAdvertisingSupported = (TextView) alertDialogView.findViewById(R.id.variable_adv_supported);
        final TextView variableTxPowerSupported = (TextView) alertDialogView.findViewById(R.id.variable_tx_power_supported);
        final TextView supportedFrameTypes = (TextView) alertDialogView.findViewById(R.id.frame_types);
        final TextView supportedTxPower = (TextView) alertDialogView.findViewById(R.id.supported_tx_power);

        final AlertDialog alertDialog = alertDialogBuilder.setView(alertDialogView).setPositiveButton(getString(R.string.ok), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    dismiss();
            }
        });

        mVersion.setText(String.valueOf(ParserUtils.getIntValue(mBroadcastCapabilities, 0, BluetoothGattCharacteristic.FORMAT_UINT8)));
        totalSlots.setText(String.valueOf(ParserUtils.getIntValue(mBroadcastCapabilities, 1, BluetoothGattCharacteristic.FORMAT_UINT8)));
        totalEidSlots.setText(String.valueOf(ParserUtils.getIntValue(mBroadcastCapabilities, 2, BluetoothGattCharacteristic.FORMAT_UINT8)));


        final int capabilities = ParserUtils.getIntValue(mBroadcastCapabilities, 3, BluetoothGattCharacteristic.FORMAT_UINT8);
        final boolean variableAdvertising = (capabilities & IS_VARIABLE_ADV_SUPPORTED) > 0;
        if(variableAdvertising)
            variableAdvertisingSupported.setText("YES");
        else
            variableAdvertisingSupported.setText("NO");

        final boolean variableTxPower = (capabilities & IS_VARIABLE_TX_POWER_SUPPORTED) > 0;
        if(variableTxPower)
            variableTxPowerSupported.setText("YES");
        else
            variableTxPowerSupported.setText("NO");

        final int supportedEddystoneFrameTypes = ParserUtils.getIntValue(mBroadcastCapabilities, 4, ParserUtils.FORMAT_UINT16_BIG_INDIAN);

        final boolean typeUid = (supportedEddystoneFrameTypes & TYPE_UID) > 0;
        final boolean typeUrl = (supportedEddystoneFrameTypes & TYPE_URL) > 0;
        final boolean typeTlm = (supportedEddystoneFrameTypes & TYPE_TLM) > 0;
        final boolean typeEid = (supportedEddystoneFrameTypes & TYPE_EID) > 0;

        StringBuilder builder = new StringBuilder();

        if(typeUid)
            builder.append("UID, ");
        if(typeUrl)
            builder.append("URL, ");
        if(typeTlm)
            builder.append("TLM, ");
        if(typeEid)
            builder.append("EID");

        if(builder.toString().endsWith(",")) {
            builder.setLength(builder.length() - 2);
        }

        supportedFrameTypes.setText(builder.toString());

        builder = new StringBuilder();
        for(int i = 6; i < mBroadcastCapabilities.length; i++){
            builder.append(" " + ParserUtils.getIntValue(mBroadcastCapabilities, i , BluetoothGattCharacteristic.FORMAT_SINT8)).append(",");
        }

        if(builder.toString().endsWith(",")){
            builder.setLength(builder.length()-1);
        }
        builder.append(" dBm");
        supportedTxPower.setText(builder.toString());

        return alertDialog;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
