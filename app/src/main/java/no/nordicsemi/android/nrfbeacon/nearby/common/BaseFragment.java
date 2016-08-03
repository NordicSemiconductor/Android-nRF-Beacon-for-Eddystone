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

package no.nordicsemi.android.nrfbeacon.nearby.common;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.R;


public abstract class BaseFragment extends Fragment implements PermissionRationaleDialogFragment.PermissionDialogListener {
    private static final int REQUEST_PERMISSION_REQ_CODE = 76; // any 8-bit number
    private ArrayList<String> mPermissionList;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onRequestPermission() {
        if(mPermissionList != null && mPermissionList.size() > 0)
            requestPermissions(mPermissionList.toArray(new String[mPermissionList.size()]), REQUEST_PERMISSION_REQ_CODE);
        else checkForUngrantedPermissions(new String [] {Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @Override
    public void onCancelRequestPermission() {

    }

    /**
     * Method called when user has granted the coarse location permission to the application.
     */
    protected abstract void onPermissionGranted(final String permission);

    /**
     * Ensures the required permission has been granted by the user. On Android pre-6.0 it always returns true.
     * In case the permission has not been granted this method may also show a dialog with rationale.
     * @return true if the permission required for BLE scanning is granted
     */

    protected boolean ensurePermission(final String [] permissions) {
        // Since Android 6.0 we need to obtain either Manifest.permission.ACCESS_COARSE_LOCATION or Manifest.permission.ACCESS_FINE_LOCATION to be able to scan for
        // Bluetooth LE devices. This is related to beacons as proximity devices.
        // On API older than Marshmallow the following code does nothing.
        mPermissionList = new ArrayList<>();
        if(permissions.length > 0 ) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(getContext(), permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    // When user pressed Deny and still wants to use this functionality, show the rationale
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permissions[i])) {
                        if(permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION) {
                            final PermissionRationaleDialogFragment dialog = PermissionRationaleDialogFragment.getInstance(getString(R.string.rationale_message_location));
                            dialog.show(getChildFragmentManager(), null);
                            return false;
                        }
                    } else {
                        mPermissionList.add(permissions[i]);
                    }
                } else {
                    onPermissionGranted(permissions[i]);
                }
            }

            if(mPermissionList.size() > 0) {
                onRequestPermission();
                return true;
            }
        }
        return true;
    }

    protected void checkForUngrantedPermissions(final String [] permissions) {
        mPermissionList = new ArrayList<>();
        if(permissions.length > 0 ) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(getContext(), permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    // When user pressed Deny and still wants to use this functionality, show the rationale
                    mPermissionList.add(permissions[i]);
                }
            }

            if(mPermissionList.size() > 0) {
                onRequestPermission();
            }
        }
    }
}
