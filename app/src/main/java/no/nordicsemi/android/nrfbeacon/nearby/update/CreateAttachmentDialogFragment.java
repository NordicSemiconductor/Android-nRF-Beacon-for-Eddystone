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

import java.nio.charset.Charset;

import no.nordicsemi.android.nrfbeacon.nearby.R;

/**
 * Created by rora on 07.03.2016.
 */
public class CreateAttachmentDialogFragment extends DialogFragment {

    private static final String TAG = "BEACON";
    private static final String BEACON_NAME = "BEACON_NAME";
    private String mBeaconName;
    private EditText mAttachment;

    public interface OnAttachmentCreatedListener {
        void createAttachmentForBeacon(final String mBeaconName, final byte[] attachmentData);
    }

    public static CreateAttachmentDialogFragment newInstance(final String beaconName){
        CreateAttachmentDialogFragment fragment = new CreateAttachmentDialogFragment();
        Bundle args = new Bundle();
        args.putString(BEACON_NAME, beaconName);
        fragment.setArguments(args);
        return fragment;
    }

    public CreateAttachmentDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null)
            mBeaconName = getArguments().getString(BEACON_NAME);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.create_attachment_title));
        alertDialogBuilder.setMessage(getString(R.string.create_attachment_message));
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_create_beacon_attachment, null);
        mAttachment = (EditText) view.findViewById(R.id.attachment);

        final AlertDialog alertDialog = alertDialogBuilder.setView(view).setPositiveButton(getString(R.string.create), null).setNegativeButton(getString(R.string.cancel), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateInput()) {
                    final byte [] attachmentData = mAttachment.getText().toString().trim().getBytes(Charset.forName("UTF-8"));
                    ((OnAttachmentCreatedListener)getParentFragment()).createAttachmentForBeacon(mBeaconName, attachmentData);
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

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private boolean validateInput(){
        final String attachment = mAttachment.getText().toString().trim();
        if(attachment.isEmpty()){
            mAttachment.setError(getString(R.string.enter_attachment_data));
            return false;
        }
        return true;
    }
}
