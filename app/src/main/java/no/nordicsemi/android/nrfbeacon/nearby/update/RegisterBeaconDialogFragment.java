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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

/**
 * Created by rora on 07.03.2016.
 */
public class RegisterBeaconDialogFragment extends DialogFragment {

    private static final String PATTERN_NAMESPACE_ID = "[0-9a-fA-F]{20}";
    private static final String PATTERN_INSTANCE_ID = "[0-9a-fA-F]{12}";
    private static final String TAG = "BEACON";
    private static final int TYPE_UID = 0x00;
    private static final int TYPE_EID = 0x30;
    private EditText mNamespaceId;
    private EditText mInstanceId;
    private EditText mAttachment;

    public interface OnBeaconRegisteredListener {
        void registerBeaconListener(final byte[] uid);
    }

    public static RegisterBeaconDialogFragment newInstance(){
        RegisterBeaconDialogFragment fragment = new RegisterBeaconDialogFragment();
        return fragment;
    }

    public RegisterBeaconDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.register_beacon_title));
        alertDialogBuilder.setMessage(getString(R.string.register_beacon_message));
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_register_beacon, null);
        mNamespaceId = (EditText) view.findViewById(R.id.namespace_id);
        mInstanceId = (EditText) view.findViewById(R.id.instance_id);

        final AlertDialog alertDialog = alertDialogBuilder.setView(view)
                .setPositiveButton(getString(R.string.register), null)
                .setNeutralButton(getString(R.string.random_uid), null)
                .setNegativeButton(getString(R.string.cancel), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    final byte[] uid = new byte[16];
                    ParserUtils.setByteArrayValue(uid, 0, mNamespaceId.getText().toString().trim());
                    ParserUtils.setByteArrayValue(uid, 10, mInstanceId.getText().toString().trim());
                    ((OnBeaconRegisteredListener) getParentFragment()).registerBeaconListener(uid);
                    dismiss();
                }
            }
        });

        alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String randomUid = ParserUtils.randomUid(16);
                final String namespace = (randomUid.substring(0,20));
                final String instance = (randomUid.substring(20,randomUid.length()));
                mNamespaceId.setText(namespace.toUpperCase());
                mInstanceId.setText(instance.toUpperCase());
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
        return true;
    }

    private byte[] aes128Encrypt(byte[] data, SecretKeySpec keySpec) {
        Cipher cipher;
        try {
            // Ignore the "ECB encryption should not be used" warning. We use exactly one block so
            // the difference between ECB and CBC is just an IV or not. In addition our blocks are
            // always different since they have a monotonic timestamp. Most importantly, our blocks
            // aren't sensitive. Decrypting them means means knowing the beacon time and its rotation
            // period. If due to ECB an attacker could find out that the beacon broadcast the same
            // block a second time, all it could infer is that for some reason the clock of the beacon
            // reset, which is not very helpful
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "Error constructing cipher instance", e);
            return null;
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Error initializing cipher instance", e);
            return null;
        }

        byte[] ret;
        try {
            ret = cipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Log.e(TAG, "Error executing cipher", e);
            return null;
        }

        return ret;
    }



}
