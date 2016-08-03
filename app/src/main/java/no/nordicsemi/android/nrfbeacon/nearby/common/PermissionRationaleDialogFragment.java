/*************************************************************************************************************************************************
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * <p/>
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p/>
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************************************************************************************/

package no.nordicsemi.android.nrfbeacon.nearby.common;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import no.nordicsemi.android.nrfbeacon.nearby.R;


public class PermissionRationaleDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

	private static final String RATIONALE_MESSAGE = "RATIONALE_MESSAGE";
	private String mRationaleMessage;

	public static PermissionRationaleDialogFragment getInstance(final String message){
		PermissionRationaleDialogFragment fragment = new PermissionRationaleDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(RATIONALE_MESSAGE, message);
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getArguments() != null)
			mRationaleMessage = getArguments().getString(RATIONALE_MESSAGE);
	}

	public interface PermissionDialogListener {
		void onRequestPermission();
		void onCancelRequestPermission();
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		return new AlertDialog.Builder(getContext()).setTitle(R.string.rationale_title)
				.setMessage(mRationaleMessage)
				.setPositiveButton(R.string.rationale_request, this)
				.setNegativeButton(R.string.rationale_cancel, this).create();
	}

	@Override
	public void onClick(final DialogInterface dialogInterface, final int i) {
		switch (i) {
			case DialogInterface.BUTTON_POSITIVE:
				((PermissionDialogListener) getParentFragment()).onRequestPermission();
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				((PermissionDialogListener) getParentFragment()).onCancelRequestPermission();
				break;
		}
	}
}
