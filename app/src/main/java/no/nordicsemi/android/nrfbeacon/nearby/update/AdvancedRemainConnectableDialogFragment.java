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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import no.nordicsemi.android.nrfbeacon.nearby.R;

/**
 * Created by rora on 07.03.2016.
 */
public class AdvancedRemainConnectableDialogFragment extends DialogFragment {

    private static final String REMAIN_CONNECTABLE_FLAG = "REMAIN_CONNECTABLE_FLAG";
    private boolean mRemainConnectableFlag;
    private boolean mRemainConnectable;

    public interface OnAdvancedRemainConnectableStateListener {
        void onRemainConntable(boolean remainConnectable);
    }

    public static AdvancedRemainConnectableDialogFragment newInstance(final boolean flag){
        AdvancedRemainConnectableDialogFragment fragment = new AdvancedRemainConnectableDialogFragment();
        final Bundle args = new Bundle();
        args.putBoolean(REMAIN_CONNECTABLE_FLAG, flag);
        fragment.setArguments(args);
        return fragment;
    }

    public AdvancedRemainConnectableDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mRemainConnectableFlag = getArguments().getBoolean(REMAIN_CONNECTABLE_FLAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.title_remain_connectable));
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_remain_connectable, null);
        final AlertDialog alertDialog = alertDialogBuilder.setView(view).setPositiveButton(getString(R.string.ok), null).setNegativeButton(getString(R.string.cancel), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        final RadioGroup remainConnectableRadioGroup = (RadioGroup) view.findViewById(R.id.rg_remain_connectable);
        final RadioButton remainConnectable = (RadioButton) view.findViewById(R.id.rb_remain_connectable);
        final RadioButton remainNonConnectable = (RadioButton) view.findViewById(R.id.rb_remain_non_connectable);
        final TextView message = (TextView) view.findViewById(R.id.tv_remain_connnectable_message);

        if(mRemainConnectableFlag)
            remainConnectableRadioGroup.setVisibility(View.VISIBLE);
        else
            message.setVisibility(View.VISIBLE);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRemainConnectableFlag) {
                    if (remainConnectable.isChecked()) {
                        ((AdvancedRemainConnectableDialogFragment.OnAdvancedRemainConnectableStateListener) getParentFragment()).onRemainConntable(true);
                    }
                    else if(remainNonConnectable.isChecked()) {
                        ((AdvancedRemainConnectableDialogFragment.OnAdvancedRemainConnectableStateListener) getParentFragment()).onRemainConntable(false);
                    } else {
                        Toast.makeText(getActivity(), "Please select an option to proceed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                dismiss();
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


}
