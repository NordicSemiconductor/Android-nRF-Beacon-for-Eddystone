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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

/**
 * Created by rora on 06.04.2016.
 */
public class AdvertisingIntervalDialogFragment extends DialogFragment {
    private static final String ADV_INTERVAL = "ADV_INTERVAL";
    private static final String TAG = "BEACON";

    private String mCurrentAdvInterval;
    private EditText advInterval;

    public interface OnAdvertisingIntervalListener {
        void configureAdvertisingInterval(final byte [] advertisingInterval);
    }

    public static AdvertisingIntervalDialogFragment newInstance(final String currentAdvInterval){
        AdvertisingIntervalDialogFragment fragment = new AdvertisingIntervalDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ADV_INTERVAL, currentAdvInterval);
        fragment.setArguments(args);
        return fragment;
    }

    public AdvertisingIntervalDialogFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mCurrentAdvInterval = getArguments().getString(ADV_INTERVAL);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.title_config_radio_tx_power));
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_advertising_interval, null);
        final TextView textView = (TextView) view.findViewById(R.id.current_adv_interval);
        textView.setText(mCurrentAdvInterval);
        advInterval = (EditText) view.findViewById(R.id.advertising_interval);
        final AlertDialog alertDialog = alertDialogBuilder.setView(view).setPositiveButton(getString(R.string.configure), null).setNegativeButton(getString(R.string.cancel), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateInput()){
                    final byte [] advertisingInterval = getValueFromView();
                    ((OnAdvertisingIntervalListener)getParentFragment()).configureAdvertisingInterval(advertisingInterval);
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

    private boolean validateInput() {
        final String advertisingInterval = advInterval.getText().toString().trim();

        if(advertisingInterval.isEmpty()){
            advInterval.setError("Enter advertising interval value");
            return false;
        } else {
            boolean valid;
            try {
                int i = Integer.parseInt(advertisingInterval);
                valid = (i & 0xFFFF0000) == 0 || (i & 0xFFFF0000) == 0xFFFF0000;
            } catch (NumberFormatException e) {
                valid = false;
            }
            if (!valid) {
                advInterval.setError("Value does not match UINT16");
                return false;
            }
        }

        return true;
    }

    private byte[] getValueFromView() {
        final byte [] data = new byte[2];
        final int value = Integer.parseInt(advInterval.getText().toString().trim());
        ParserUtils.setValue(data, 0, value, ParserUtils.FORMAT_UINT16_BIG_INDIAN);
        return data;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
