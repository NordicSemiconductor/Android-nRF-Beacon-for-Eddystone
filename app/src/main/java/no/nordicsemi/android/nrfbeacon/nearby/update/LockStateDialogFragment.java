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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

/**
 * Created by rora on 07.03.2016.
 */
public class LockStateDialogFragment extends DialogFragment {

    private static final String PATTERN_LOCK_STATE = "[0-9a-fA-F]{32}";
    private static final String LOCK_STATE = "LOCK_STATE";
    private static final String UNLOCK_CODE = "UNLOCK_CODE";
    private static final String TAG = "BEACON";
    private static final int LOCKED = 0x00;
    private static final int UNLOCKED = 0x01;
    private static final int UNLOCKED_AUTOMATIC_RELOCK_DISABLED = 0x02;

    private int mCurrentLockState;
    private byte [] mUnlockCode;
    private EditText mOldLockCode;
    private EditText mNewLockCode;
    private TextView mTitleOldLockCode;
    private TextView mTitleNewLockCode;
    private LinearLayout mOldLockCodeContainer;
    private LinearLayout mNewLockCodeContainer;
    private Spinner mLockStates;

    public interface OnLockStateListener {
        void lockBeacon(byte[] lockCode);
    }

    public static LockStateDialogFragment newInstance(final int currentLockState, final byte [] unlockCode){
        LockStateDialogFragment fragment = new LockStateDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(LOCK_STATE, currentLockState);
        args.putByteArray(UNLOCK_CODE, unlockCode);
        fragment.setArguments(args);
        return fragment;
    }

    public LockStateDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mCurrentLockState = getArguments().getInt(LOCK_STATE);
            mUnlockCode = getArguments().getByteArray(UNLOCK_CODE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Lock State");

        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_lock_state, null);
        mLockStates = (Spinner) view.findViewById(R.id.frame_types);
        mOldLockCode = (EditText) view.findViewById(R.id.old_lock_code);
        mNewLockCode = (EditText) view.findViewById(R.id.new_lock_code);
        mTitleOldLockCode = (TextView) view.findViewById(R.id.title_old_lock_code);
        mTitleNewLockCode = (TextView) view.findViewById(R.id.title_new_lock_code);
        mOldLockCodeContainer = (LinearLayout) view.findViewById(R.id.old_lock_code_container);
        mNewLockCodeContainer = (LinearLayout) view.findViewById(R.id.new_lock_code_container);

        mLockStates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        mTitleOldLockCode.setVisibility(View.GONE);
                        mOldLockCodeContainer.setVisibility(View.GONE);
                        mTitleNewLockCode.setVisibility(View.GONE);
                        mNewLockCodeContainer.setVisibility(View.GONE);
                        break;
                    case 1:
                        mTitleOldLockCode.setVisibility(View.VISIBLE);
                        mOldLockCodeContainer.setVisibility(View.VISIBLE);
                        mTitleNewLockCode.setVisibility(View.VISIBLE);
                        mNewLockCodeContainer.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        mTitleOldLockCode.setVisibility(View.GONE);
                        mOldLockCodeContainer.setVisibility(View.GONE);
                        mTitleNewLockCode.setVisibility(View.GONE);
                        mNewLockCodeContainer.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        updateCurrentLockState();

        final AlertDialog alertDialog = alertDialogBuilder.setView(view).setPositiveButton(getString(R.string.ok), null).setNegativeButton(getString(R.string.cancel), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateInput()){
                    final byte [] lockCodeData = getValueFromView();
                    ((OnLockStateListener)getParentFragment()).lockBeacon(lockCodeData);
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

    private void updateCurrentLockState() {
        switch(mCurrentLockState){
            case LOCKED:
                mLockStates.setSelection(0);
                break;
            case UNLOCKED:
                mLockStates.setSelection(1);
                break;
            case UNLOCKED_AUTOMATIC_RELOCK_DISABLED:
                mLockStates.setSelection(2);
                break;
        }
    }

    private boolean validateInput() {

        if(mOldLockCodeContainer.getVisibility() == View.VISIBLE) {
            final String oldLockCode = mOldLockCode.getText().toString().trim();
            if(oldLockCode.isEmpty()){
                mOldLockCode.setError("Please enter old lock code");
                return false;
            } else if (!oldLockCode.matches(PATTERN_LOCK_STATE)) {
                mOldLockCode.setError("Please enter a valid value for old lock code");
                return false;
            }
            final byte [] oldBeaconLockCode = new byte[16];
            ParserUtils.setByteArrayValue(oldBeaconLockCode, 0, oldLockCode);
            if(!Arrays.equals(mUnlockCode, oldBeaconLockCode)){
                Toast.makeText(getActivity(), getString(R.string.lock_code_error), Toast.LENGTH_SHORT).show();
                return false;
            }

        }

        if(mNewLockCodeContainer.getVisibility() == View.VISIBLE) {
            final String newLockCode = mNewLockCode.getText().toString().trim();
            if (newLockCode.isEmpty()) {
                mNewLockCode.setError("Please enter new lock code");
                return false;
            } else {
                if (!newLockCode.matches(PATTERN_LOCK_STATE)) {
                    mNewLockCode.setError("Please enter a valid value for new lock code");
                    return false;
                }
            }
        }
        return true;
    }

    private byte[] getValueFromView() {

        final int lockState = mLockStates.getSelectedItemPosition();
        final String oldLockCode = mOldLockCode.getText().toString();
        final String newLockCode = mNewLockCode.getText().toString();
        final byte [] data;

        if(lockState == 0 || lockState == 2){
            data = new byte[1];
            data[0] = (byte)lockState;
            return data;
        } else {
            data = new byte[17]; //lockstate + security key
            data[0] = (byte)(lockState-1);

            final byte [] oldLockCodeBytes = new byte [16];
            ParserUtils.setByteArrayValue(oldLockCodeBytes, 0, oldLockCode);

            final byte [] newLockCodeBytes = new byte[16];
            ParserUtils.setByteArrayValue(newLockCodeBytes, 0, newLockCode);

            final byte [] encryptedLockCode = ParserUtils.aes128Encrypt(newLockCodeBytes, new SecretKeySpec(oldLockCodeBytes, "AES"));
            System.arraycopy(encryptedLockCode, 0, data, 1, encryptedLockCode.length);
            return data;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
