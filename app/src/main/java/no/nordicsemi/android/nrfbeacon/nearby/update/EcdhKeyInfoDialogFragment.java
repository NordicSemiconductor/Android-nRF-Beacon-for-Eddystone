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
public class EcdhKeyInfoDialogFragment extends DialogFragment {


    private static final String BEACON_ECDH_PUBLIC_KEY = "BEACON_ECDH_PUBLIC_KEY";
    private static final String ENCRYPTED_IDENTITY_KEY = "ENCRYPTED_IDENTITY_KEY";
    private static final String DECRYPTED_IDENTITY_KEY = "DECRYPTED_IDENTITY_KEY";
    private static final String FRAME_TYPE = "FRAME_TYPE";
    private static final String ACTIVE_SLOT = "ACTIVE_SLOT";

    private static final int TYPE_UID = 0x00;
    private static final int TYPE_URL = 0x10;
    private static final int TYPE_TLM = 0x20;
    private static final int TYPE_EID = 0x30;
    private int mFrameType;
    private int mActiveSlot;
    private String mEncryptedIdentityKey;
    private String mDecryptedIdentityKey;
    private String mPublicEcdhKey;

    public static EcdhKeyInfoDialogFragment newInstance(final int activeSlot, final int frameType, final String beaconEcdhKeyPublicKey, final String encryptedIdentityKey, final String decryptedIdentityKey){
        EcdhKeyInfoDialogFragment fragment = new EcdhKeyInfoDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ACTIVE_SLOT, activeSlot);
        args.putInt(FRAME_TYPE, frameType);
        args.putString(BEACON_ECDH_PUBLIC_KEY, beaconEcdhKeyPublicKey);
        args.putString(ENCRYPTED_IDENTITY_KEY, encryptedIdentityKey);
        args.putString(DECRYPTED_IDENTITY_KEY, decryptedIdentityKey);
        fragment.setArguments(args);
        return fragment;
    }

    public EcdhKeyInfoDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(getArguments() != null){
            mActiveSlot = getArguments().getInt(ACTIVE_SLOT);
            mFrameType = getArguments().getInt(FRAME_TYPE);
            mPublicEcdhKey = getArguments().getString(BEACON_ECDH_PUBLIC_KEY);
            mEncryptedIdentityKey = getArguments().getString(ENCRYPTED_IDENTITY_KEY);
            mDecryptedIdentityKey = getArguments().getString(DECRYPTED_IDENTITY_KEY);
        }
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.title_ecdh_key_info));

        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_ecdh, null);
        AlertDialog alertDialog = alertDialogBuilder.setView(view).setPositiveButton(getString(R.string.ok), null).show();
        final TextView activeSlot = (TextView) view.findViewById(R.id.active_slot);
        final TextView frameType = (TextView) view.findViewById(R.id.frame_type);
        final TextView ecdhKey = (TextView) view.findViewById(R.id.ecdh_public_key);
        final TextView encryptedIdentityKey = (TextView) view.findViewById(R.id.encrypted_ecdh_identity_key);
        final TextView decryptedIdentityKey = (TextView) view.findViewById(R.id.decrypted_ecdh_identity_key);
        activeSlot.setText(String.valueOf(mActiveSlot));
        ecdhKey.setText(mPublicEcdhKey);

        //display identity key only if the active slot frame type is EID as the identity keys will differ for each slot
        switch (mFrameType){
            case TYPE_UID:
                frameType.setText(getString(R.string.type_uid));
                encryptedIdentityKey.setText(getString(R.string.no_identity_key));
                decryptedIdentityKey.setText(getString(R.string.no_identity_key));
                break;
            case TYPE_URL:
                frameType.setText(getString(R.string.type_url));
                encryptedIdentityKey.setText(getString(R.string.no_identity_key));
                decryptedIdentityKey.setText(getString(R.string.no_identity_key));
                break;
            case TYPE_TLM:
                frameType.setText(getString(R.string.type_tlm));
                encryptedIdentityKey.setText(getString(R.string.no_identity_key));
                decryptedIdentityKey.setText(getString(R.string.no_identity_key));
                break;
            case TYPE_EID:
                frameType.setText(getString(R.string.type_eid));
                encryptedIdentityKey.setText(mEncryptedIdentityKey);
                decryptedIdentityKey.setText(mDecryptedIdentityKey);
                break;
            default:
                frameType.setText(getString(R.string.type_empty));
                encryptedIdentityKey.setText(mEncryptedIdentityKey);
                decryptedIdentityKey.setText(mDecryptedIdentityKey);
                break;

        }




        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return alertDialog;

    }
}
