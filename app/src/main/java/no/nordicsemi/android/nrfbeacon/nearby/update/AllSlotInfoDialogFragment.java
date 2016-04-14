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
import android.support.v7.widget.LinearLayoutCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

/**
 * Created by rora on 07.03.2016.
 */
public class AllSlotInfoDialogFragment extends DialogFragment {

    private static final String ALL_SLOT_INFO = "ALL_SLOT_INFO";
    private ArrayList<String> mAllSlotInfo;

    public static AllSlotInfoDialogFragment newInstance(final ArrayList<String> allSlotInfo){
        AllSlotInfoDialogFragment fragment = new AllSlotInfoDialogFragment();
        final Bundle args = new Bundle();
        args.putStringArrayList(ALL_SLOT_INFO, allSlotInfo);
        fragment.setArguments(args);
        return fragment;
    }

    public AllSlotInfoDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mAllSlotInfo = getArguments().getStringArrayList(ALL_SLOT_INFO);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("All Slot Information");
        alertDialogBuilder.setMessage("Following slots have been configured in the beacon");

        final View alertDialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_all_slot_info, null);
        final LinearLayout slotTitleContainer = (LinearLayout) alertDialogView.findViewById(R.id.slot_title_container);
        final LinearLayout slotInfoContainer = (LinearLayout) alertDialogView.findViewById(R.id.slot_info_container);
        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(LinearLayoutCompat.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        for(int i = 0; i < mAllSlotInfo.size(); i++) {
            TextView slotTitle = new TextView(getActivity());
            slotTitle.setLayoutParams(lParams);
            slotTitle.setText("Slot " + i);
            slotTitleContainer.addView(slotTitle);

            TextView slotInfo = new TextView(getActivity());
            slotInfo.setLayoutParams(lParams);
            slotInfo.setText(mAllSlotInfo.get(i));
            slotInfoContainer.addView(slotInfo);

            if(i > 0){
                lParams.setMargins(0, 10, 0, 0);
            }

        }

        final AlertDialog alertDialog = alertDialogBuilder.setView(alertDialogView).setPositiveButton(getString(R.string.ok), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    dismiss();
            }
        });



        return alertDialog;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
