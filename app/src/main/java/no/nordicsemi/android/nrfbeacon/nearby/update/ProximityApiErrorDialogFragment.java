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
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import no.nordicsemi.android.nrfbeacon.nearby.R;

/**
 * Created by rora on 16.03.2016.
 */
public class ProximityApiErrorDialogFragment extends DialogFragment{

    private static final String ERROR_CODE = "ERROR_CODE";
    private static final String MESSAGE = "MESSAGE";
    private static final String STATUS = "STATUS";
    private String mErrorCode;
    private String mMessage;
    private String mStatus;


    public static ProximityApiErrorDialogFragment newInstance(final String errorCode, final String message, final String status){
        ProximityApiErrorDialogFragment fragment = new ProximityApiErrorDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ERROR_CODE, errorCode);
        args.putString(MESSAGE, message);
        args.putString(STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    public ProximityApiErrorDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mErrorCode = getArguments().getString(ERROR_CODE);
            mMessage = getArguments().getString(MESSAGE);
            mStatus = getArguments().getString(STATUS);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Error " +  mErrorCode);
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_error, null);
        final TextView message = (TextView) view.findViewById(R.id.error_message);
        final TextView status = (TextView) view.findViewById(R.id.error_status);
        message.setText(mMessage);
        status.setText(mStatus);
        final AlertDialog alertDialog = alertDialogBuilder.setView(view).setPositiveButton(getString(R.string.ok), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return alertDialog;
    }
}
